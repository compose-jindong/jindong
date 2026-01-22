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
import kotlinx.coroutines.delay
import platform.CoreHaptics.CHHapticEngine
import platform.CoreHaptics.CHHapticEvent
import platform.CoreHaptics.CHHapticEventParameter
import platform.CoreHaptics.CHHapticEventParameterIDHapticIntensity
import platform.CoreHaptics.CHHapticEventParameterIDHapticSharpness
import platform.CoreHaptics.CHHapticEventTypeHapticContinuous
import platform.CoreHaptics.CHHapticPattern
import platform.CoreHaptics.CHHapticPatternPlayerProtocol
import platform.Foundation.NSError

/**
 * iOS HapticExecutor implementation using Core Haptics.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class DefaultIosHapticExecutor : HapticExecutor {

  private var engine: CHHapticEngine? = null

  override val isSupported: Boolean by lazy {
    CHHapticEngine.capabilitiesForHardware().supportsHaptics()
  }

  override suspend fun execute(pattern: HapticPattern) {
    if (!isSupported || pattern.events.isEmpty()) return

    val currentEngine = ensureEngine() ?: return
    val chPattern = pattern.toCHHapticPattern() ?: return

    memScoped {
      val errorPtr = alloc<ObjCObjectVar<NSError?>>()
      val player = currentEngine.createPlayerWithPattern(chPattern, errorPtr.ptr)
      if (errorPtr.value != null || player == null) return

      player.startAtTime(0.0, errorPtr.ptr)
      if (errorPtr.value != null) return

      val totalDurationMs = pattern.events.maxOfOrNull { it.startTimeMs + it.durationMs } ?: 0L
      delay(totalDurationMs)

      player.stopAtTime(0.0, errorPtr.ptr)
    }
  }

  override fun executeAsync(pattern: HapticPattern): HapticHandle {
    if (!isSupported || pattern.events.isEmpty()) {
      return IosHapticHandle(null)
    }

    val currentEngine = ensureEngine() ?: return IosHapticHandle(null)
    val hapticPattern = pattern.toCHHapticPattern() ?: return IosHapticHandle(null)

    return memScoped {
      val errorPtr = alloc<ObjCObjectVar<NSError?>>()
      val player = currentEngine.createPlayerWithPattern(hapticPattern, errorPtr.ptr)
      if (errorPtr.value != null || player == null) {
        return@memScoped IosHapticHandle(null)
      }

      player.startAtTime(0.0, errorPtr.ptr)
      if (errorPtr.value != null) {
        return@memScoped IosHapticHandle(null)
      }

      IosHapticHandle(player)
    }
  }

  override fun release() {
    engine?.stopWithCompletionHandler(null)
    engine = null
  }

  private fun ensureEngine(): CHHapticEngine? {
    if (engine != null) return engine

    return memScoped {
      val errorPtr = alloc<ObjCObjectVar<NSError?>>()
      val newEngine = CHHapticEngine(errorPtr.ptr)
      if (errorPtr.value != null) {
        return@memScoped null
      }

      newEngine.stoppedHandler = { _ ->
        engine = null
      }

      newEngine.resetHandler = {
        memScoped {
          val errorPtr = alloc<ObjCObjectVar<NSError?>>()
          newEngine.startAndReturnError(errorPtr.ptr)
        }
      }

      newEngine.startAndReturnError(errorPtr.ptr)
      if (errorPtr.value != null) {
        return@memScoped null
      }

      engine = newEngine
      newEngine
    }
  }

  private fun HapticPattern.toCHHapticPattern(): CHHapticPattern? {
    val hapticEvents = events.map { it.toCHHapticEvent() }
    return memScoped {
      val errorPtr = alloc<ObjCObjectVar<NSError?>>()
      val pattern = CHHapticPattern(
        events = hapticEvents,
        parameters = emptyList<Any>(),
        error = errorPtr.ptr,
      )
      if (errorPtr.value != null) null else pattern
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

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class IosHapticHandle(
  private var player: CHHapticPatternPlayerProtocol?,
) : HapticHandle {

  private var _isActive = player != null

  override val isActive: Boolean
    get() = _isActive

  override fun cancel() {
    if (!_isActive) return
    _isActive = false

    memScoped {
      val errorPtr = alloc<ObjCObjectVar<NSError?>>()
      player?.stopAtTime(0.0, errorPtr.ptr)
    }
    player = null
  }
}

/**
 * Creates an iOS-specific [HapticExecutor].
 *
 * @param context Not used on iOS, can be null
 * @return IosHapticExecutor implementation
 */
public actual fun createHapticExecutor(context: Any?): HapticExecutor = DefaultIosHapticExecutor()
