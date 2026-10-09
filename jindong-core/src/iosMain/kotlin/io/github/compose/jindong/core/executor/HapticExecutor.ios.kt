/*
 * Copyright (C) 2026 compose-jindong
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package io.github.compose.jindong.core.executor

import io.github.compose.jindong.core.model.HapticPattern
import io.github.compose.jindong.core.model.ScheduledHapticEvent
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import platform.CoreHaptics.CHHapticEngine
import platform.CoreHaptics.CHHapticEvent
import platform.CoreHaptics.CHHapticEventParameter
import platform.CoreHaptics.CHHapticEventParameterIDHapticIntensity
import platform.CoreHaptics.CHHapticEventParameterIDHapticSharpness
import platform.CoreHaptics.CHHapticEventTypeHapticContinuous
import platform.CoreHaptics.CHHapticPattern
import platform.Foundation.NSError
import kotlin.time.TimeSource

/**
 * iOS HapticExecutor implementation using Core Haptics.
 */
internal class DefaultIosHapticExecutor(timeSource: TimeSource = TimeSource.Monotonic) : HapticExecutor {

  private val sessions = PlaybackSessions(timeSource)
  private var engine: CHHapticEngine? = null

  override val isSupported: Boolean by lazy {
    CHHapticEngine.capabilitiesForHardware().supportsHaptics()
  }

  // Core Haptics renders intensity as a continuous float on supported devices; gate on support so we
  // never report amplitude control on a device that has no haptics at all (e.g. the simulator).
  override val hasAmplitudeControl: Boolean
    get() = isSupported

  override suspend fun execute(pattern: HapticPattern) {
    currentCoroutineContext().ensureActive()
    executeAsync(pattern).awaitCompletion()
  }

  override fun executeAsync(pattern: HapticPattern): PlaybackSessions.Session = sessions.start {
    if (pattern.events.none { it.durationMs > 0L && it.intensity.value > 0f }) {
      return@start NativePlayback(pattern.durationMs)
    }
    if (!isSupported) return@start null
    val hapticPattern = checkNotNull(pattern.toCHHapticPattern())
    val currentEngine = ensureEngine()

    memScoped {
      val errorPtr = alloc<ObjCObjectVar<NSError?>>()
      val player = currentEngine.createPlayerWithPattern(hapticPattern, errorPtr.ptr)
      check(player != null && errorPtr.value == null) {
        "Could not create haptic player: ${errorPtr.value?.localizedDescription}"
      }
      val started = player.startAtTime(0.0, errorPtr.ptr)
      if (!started || errorPtr.value != null) {
        player.stopAtTime(0.0, null)
        error("Could not start haptic player: ${errorPtr.value?.localizedDescription}")
      }
      NativePlayback(pattern.playbackDurationMs()) { player.stopAtTime(0.0, null) }
    }
  }

  override fun release() = sessions.release {
    val oldEngine = engine
    engine = null
    oldEngine?.let(::disposeEngine)
  }

  private fun ensureEngine(): CHHapticEngine {
    engine?.let { return it }
    return memScoped {
      val errorPtr = alloc<ObjCObjectVar<NSError?>>()
      val newEngine = CHHapticEngine(errorPtr.ptr)
      check(errorPtr.value == null) {
        "Could not create haptic engine: ${errorPtr.value?.localizedDescription}"
      }
      newEngine.stoppedHandler = { _ -> invalidateEngine(newEngine) }
      newEngine.resetHandler = { invalidateEngine(newEngine) }
      engine = newEngine
      val started = newEngine.startAndReturnError(errorPtr.ptr)
      if (!started || errorPtr.value != null) {
        engine = null
        disposeEngine(newEngine)
        error("Could not start haptic engine: ${errorPtr.value?.localizedDescription}")
      }
      newEngine
    }
  }

  private fun invalidateEngine(stoppedEngine: CHHapticEngine) = sessions.withLock {
    if (engine === stoppedEngine) {
      engine = null
      try {
        sessions.cancel()
      } finally {
        disposeEngine(stoppedEngine)
      }
    }
  }

  private fun disposeEngine(oldEngine: CHHapticEngine) {
    oldEngine.stoppedHandler = { _ -> }
    oldEngine.resetHandler = { }
    oldEngine.stopWithCompletionHandler(null)
  }

  // Signature is asymmetric with Android's playbackDurationMs() on purpose: playback length is derived
  // from a different input per platform (iOS = the pattern, Android = the waveform actually played).
  // Core Haptics has no compat segments; trailing silence still belongs to the logical deadline.
  private fun HapticPattern.playbackDurationMs(): Long = durationMs

  internal fun HapticPattern.toCHHapticPattern(): CHHapticPattern? {
    val hapticEvents = events.filter { it.durationMs > 0L && it.intensity.value > 0f }.map { it.toCHHapticEvent() }
    if (hapticEvents.isEmpty()) return null
    return memScoped {
      val errorPtr = alloc<ObjCObjectVar<NSError?>>()
      val pattern = CHHapticPattern(
        events = hapticEvents,
        parameters = emptyList<Any>(),
        error = errorPtr.ptr,
      )
      check(errorPtr.value == null) {
        "Could not create haptic pattern: ${errorPtr.value?.localizedDescription}"
      }
      pattern
    }
  }

  private fun ScheduledHapticEvent.toCHHapticEvent(): CHHapticEvent {
    val relativeTime = startTimeMs / 1000.0
    val duration = durationMs / 1000.0

    val intensityEventParameter = CHHapticEventParameter(
      parameterID = CHHapticEventParameterIDHapticIntensity,
      value = intensity.value,
    )

    val sharpness = iosParameters?.sharpness ?: DEFAULT_SHARPNESS
    val sharpnessEventParameter = CHHapticEventParameter(
      parameterID = CHHapticEventParameterIDHapticSharpness,
      value = sharpness,
    )

    return CHHapticEvent(
      eventType = CHHapticEventTypeHapticContinuous,
      parameters = listOf(intensityEventParameter, sharpnessEventParameter),
      relativeTime = relativeTime,
      duration = duration,
    )
  }

  companion object {
    private const val DEFAULT_SHARPNESS = 0.5f
  }
}

/**
 * Creates an iOS-specific [HapticExecutor].
 *
 * @param context Not used on iOS, can be null
 * @return IosHapticExecutor implementation
 */
actual fun createHapticExecutor(context: Any?): HapticExecutor = DefaultIosHapticExecutor()
