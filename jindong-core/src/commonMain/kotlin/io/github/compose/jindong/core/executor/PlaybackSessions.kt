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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.selects.select
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
    Session(native?.durationMs ?: 0L, native?.completion, native?.stop).also { current = it }
  }

  fun cancel() = withLock {
    current?.cancel()
  }

  fun cancelFromNativeCallback(dispose: () -> Unit) {
    withLock {
      val session = current
      if (session == null) runCatching(dispose) else session.cancelFromNativeCallback(dispose)
    }
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
    private val completion: NativePlaybackCompletion?,
    private var stop: (() -> Unit)?,
  ) : HapticHandle {
    private val expiry = HandleExpiry(durationMs, timeSource)
    private val finished = CompletableDeferred<Unit>()

    override val isActive: Boolean
      get() = withLock {
        current === this && !finished.isCompleted &&
          completion?.finished?.isCancelled != true &&
          (!expiry.isExpired || completion?.finished?.isCompleted == false)
      }

    override val failure: Throwable? get() = completion?.failure

    override fun cancel() = finish { stopNative -> stopNative?.invoke() }

    // Record every cleanup failure before finish can resume an unconfined waiter.
    fun cancelFromNativeCallback(dispose: () -> Unit) = finish { stopNative ->
      try {
        stopNative?.invoke()
      } catch (error: Throwable) {
        completion?.recordCleanupFailure(error)
      }
      try {
        dispose()
      } catch (error: Throwable) {
        completion?.recordCleanupFailure(error)
      }
    }

    private fun finish(cleanup: ((() -> Unit)?) -> Unit) = withLock {
      if (!finished.isCompleted) {
        val stopNative = stop
        stop = null
        val ownsPlayback = current === this
        if (ownsPlayback) current = null
        try {
          cleanup(if (ownsPlayback) stopNative else null)
        } finally {
          finished.complete(Unit)
        }
      }
    }

    suspend fun awaitCompletion() {
      var failure: Throwable? = null
      try {
        if (completion == null) {
          if (isActive) withTimeoutOrNull(durationMs) { finished.await() }
        } else {
          coroutineScope {
            val logicalEnd = async { delay(durationMs) }
            try {
              select<Unit> {
                finished.onAwait { }
                completion.finished.onAwait {
                  select<Unit> {
                    finished.onAwait { }
                    logicalEnd.onAwait { }
                  }
                }
              }
            } finally {
              logicalEnd.cancel()
            }
          }
        }
        completion?.failure?.let { throw it }
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

internal class NativePlayback(
  val durationMs: Long,
  val completion: NativePlaybackCompletion? = null,
  val stop: (() -> Unit)? = null,
)

/** Callback state never enters the session lock, including callbacks delivered during native start. */
internal class NativePlaybackCompletion(playerCount: Int) {
  private val lock = PlaybackLock()
  private val completed = BooleanArray(playerCount)
  private var remaining = playerCount
  private var finalizing = false
  private var playbackFailure: Throwable? = null
  private var cleanupFailure: Throwable? = null
  val failure: Throwable? get() = lock.withLock { playbackFailure ?: cleanupFailure }
  val finished = CompletableDeferred<Unit>()

  init {
    require(playerCount > 0) { "Native playback must own at least one player" }
  }

  fun recordCleanupFailure(error: Throwable) = lock.withLock {
    val failure = playbackFailure ?: cleanupFailure
    if (failure == null) {
      cleanupFailure = error
    } else if (failure !== error) {
      failure.addSuppressed(error)
    }
  }

  fun completePlayer(index: Int, error: Throwable? = null, onFinished: () -> Unit = { }) {
    val complete = lock.withLock {
      if (completed[index]) return@withLock null
      completed[index] = true
      remaining--
      if (error != null && playbackFailure == null) {
        playbackFailure = error
        cleanupFailure?.let { if (it !== error) error.addSuppressed(it) }
      }
      if (!finalizing && (playbackFailure != null || remaining == 0)) {
        finalizing = true
        Pair(true, playbackFailure)
      } else {
        null
      }
    }
    if (complete != null) {
      var failure = complete.second
      try {
        onFinished()
      } catch (cleanupError: Throwable) {
        if (failure == null) failure = cleanupError else failure.addSuppressed(cleanupError)
      }
      if (failure == null) {
        finished.complete(Unit)
      } else {
        lock.withLock { playbackFailure = failure }
        finished.completeExceptionally(failure)
      }
    }
  }
}

internal expect class PlaybackLock() {
  fun <T> withLock(action: () -> T): T
}
