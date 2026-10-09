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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.TimeSource

/** Serializes native start/stop so a stale handle cannot cancel its replacement. */
internal class PlaybackSessions(private val timeSource: TimeSource = TimeSource.Monotonic) {
  private val lock = PlaybackLock()
  private var current: Session? = null
  private var released = false

  fun <T> withLock(action: () -> T): T = lock.withLock(action)

  fun start(playback: () -> NativePlayback?): Session = withLock {
    check(!released) { "HapticExecutor has been released" }
    current?.cancel()
    val native = playback()
    Session(native?.durationMs ?: 0L, native?.stop).also { current = it }
  }

  fun cancel() = withLock {
    current?.cancel()
  }

  fun release(dispose: () -> Unit) = withLock {
    if (!released) {
      released = true
      try {
        current?.cancel()
      } finally {
        dispose()
      }
    }
  }

  inner class Session internal constructor(
    private val durationMs: Long,
    private var stop: (() -> Unit)?,
  ) : HapticHandle {
    private val expiry = HandleExpiry(durationMs, timeSource)
    private val finished = CompletableDeferred<Unit>()

    override val isActive: Boolean
      get() = withLock { current === this && !finished.isCompleted && !expiry.isExpired }

    override fun cancel() = withLock {
      if (!finished.isCompleted) {
        val stopNative = stop
        stop = null
        val ownsPlayback = current === this
        if (ownsPlayback) current = null
        try {
          if (ownsPlayback) stopNative?.invoke()
        } finally {
          finished.complete(Unit)
        }
      }
    }

    suspend fun awaitCompletion() {
      var failure: Throwable? = null
      try {
        if (isActive) withTimeoutOrNull(durationMs) { finished.await() }
      } catch (error: Throwable) {
        failure = error
        throw error
      } finally {
        try {
          cancel()
        } catch (stopError: Throwable) {
          if (failure == null) throw stopError
          failure.addSuppressed(stopError)
        }
      }
    }
  }
}

internal class NativePlayback(val durationMs: Long, val stop: (() -> Unit)? = null)

internal expect class PlaybackLock() {
  fun <T> withLock(action: () -> T): T
}
