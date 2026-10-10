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
import io.github.compose.jindong.core.model.checkedTimeAdd
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import platform.darwin.dispatch_async
import platform.darwin.dispatch_queue_create
import kotlin.time.TimeSource

private val callbackQueue = dispatch_queue_create("io.github.compose.jindong.haptic-callbacks", null)

internal class DefaultIosHapticExecutor(
  timeSource: TimeSource = TimeSource.Monotonic,
  private val driver: IosHapticDriver = CoreHapticsDriver,
  private val enqueueCallback: (() -> Unit) -> Unit = { dispatch_async(callbackQueue, it) },
) : HapticExecutor {
  private val sessions = PlaybackSessions(timeSource)
  private var engine: IosEngineState? = null
  private var playbackEngine: IosEngineState? = null

  override val isSupported: Boolean get() = driver.supportsHaptics
  override val hasAmplitudeControl: Boolean get() = isSupported

  override fun diagnose(pattern: HapticPattern): HapticPlaybackDiagnostics = plan(pattern).diagnostics

  private fun plan(pattern: HapticPattern): IosPlaybackPlan = pattern.iosPlaybackPlan(
    HapticDeviceCapabilities(supportsHaptics = isSupported, supportsAmplitudeControl = hasAmplitudeControl),
  )

  override suspend fun execute(pattern: HapticPattern) {
    currentCoroutineContext().ensureActive()
    executeAsync(pattern).awaitCompletion()
  }

  override fun executeAsync(pattern: HapticPattern): PlaybackSessions.Session = sessions.start {
    playbackEngine = null
    val plan = plan(pattern)
    if (plan.diagnostics.backend == HapticPlaybackBackend.SILENT) return@start NativePlayback(pattern.durationMs)
    if (plan.players.isEmpty()) return@start null
    val logicalDeadline = checkedTimeAdd(pattern.durationMs, IOS_SCHEDULING_LEAD_MS, "Core Haptics scheduling")
    val currentEngine = ensureEngine()
    val players = mutableListOf<IosHapticPlayer>()
    try {
      plan.players.forEach { players += currentEngine.native.createPlayer(it) }
    } catch (failure: Throwable) {
      stopPlayers(players, failure)
      throw failure
    }
    val group = IosPlayerGroup(players)
    val completion = NativePlaybackCompletion(players.size)
    try {
      players.forEachIndexed { index, player ->
        player.onCompletion = { nativeError ->
          // Native start/stop can call back synchronously on another thread; never acquire session state here.
          enqueueCallback {
            completion.completePlayer(index, nativeError) {
              if (nativeError == null) group.finish() else group.stop()
            }
          }
        }
      }
      val startTime = currentEngine.native.currentTimeSeconds + IOS_SCHEDULING_LEAD_MS / 1000.0
      group.start(startTime)
      check(currentEngine.isValid) { "Core Haptics engine stopped while starting playback" }
      playbackEngine = currentEngine
      NativePlayback(logicalDeadline, completion) {
        try {
          group.stop()
        } finally {
          if (playbackEngine === currentEngine) playbackEngine = null
        }
      }
    } catch (failure: Throwable) {
      try {
        group.stop()
      } catch (stopError: Throwable) {
        failure.addSuppressed(stopError)
      }
      throw failure
    }
  }

  override fun release() = sessions.release {
    val oldEngine = engine
    engine = null
    playbackEngine = null
    oldEngine?.dispose()
  }

  private fun ensureEngine(): IosEngineState {
    engine?.let { current ->
      if (current.isValid) return current
      current.dispose()
      engine = null
    }
    val newEngine = IosEngineState(driver.createEngine())
    val invalidate = {
      newEngine.invalidate()
      enqueueCallback {
        sessions.withLock {
          val ownsPlayback = playbackEngine === newEngine
          val ownsCachedEngine = engine === newEngine
          if (ownsPlayback) playbackEngine = null
          if (ownsCachedEngine) engine = null
          if (ownsPlayback) {
            sessions.cancelFromNativeCallback {
              if (ownsCachedEngine) newEngine.dispose()
            }
          } else if (ownsCachedEngine) {
            runCatching { newEngine.dispose() }
          }
        }
      }
    }
    newEngine.native.onStopped = invalidate
    newEngine.native.onReset = invalidate
    engine = newEngine
    try {
      newEngine.native.start()
      check(newEngine.isValid) { "Core Haptics engine stopped during startup" }
    } catch (failure: Throwable) {
      engine = null
      newEngine.dispose()
      throw failure
    }
    return newEngine
  }
}

private class IosEngineState(val native: IosHapticEngine) {
  private val lock = PlaybackLock()
  private var valid = true
  val isValid: Boolean get() = lock.withLock { valid }
  fun invalidate() = lock.withLock { valid = false }
  fun dispose() {
    invalidate()
    native.onStopped = { }
    native.onReset = { }
    native.dispose()
  }
}

private class IosPlayerGroup(private val players: List<IosHapticPlayer>) {
  private val lock = PlaybackLock()
  private var stopped = false
  fun start(timeSeconds: Double) = lock.withLock {
    check(!stopped) { "Core Haptics playback ended before startup" }
    players.forEach { it.start(timeSeconds) }
  }
  fun finish() {
    if (settle()) players.forEach { it.onCompletion = { } }
  }
  fun stop() {
    if (settle()) stopPlayers(players)
  }
  private fun settle(): Boolean = lock.withLock {
    if (stopped) {
      false
    } else {
      stopped = true
      true
    }
  }
}

private fun stopPlayers(players: List<IosHapticPlayer>, originalFailure: Throwable? = null) {
  var failure = originalFailure
  players.forEach { player ->
    try {
      player.onCompletion = { }
      player.stop()
    } catch (error: Throwable) {
      if (failure == null) failure = error else failure.addSuppressed(error)
    }
  }
  if (originalFailure == null) failure?.let { throw it }
}

actual fun createHapticExecutor(context: Any?): HapticExecutor = DefaultIosHapticExecutor()
