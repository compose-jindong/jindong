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
package io.github.compose.jindong.core.fake

import io.github.compose.jindong.core.executor.HapticExecutor
import io.github.compose.jindong.core.executor.HapticHandle
import io.github.compose.jindong.core.model.HapticPattern
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * Fake implementation of [HapticExecutor] for testing.
 *
 * Records all executed patterns and hands out handles whose natural completion is driven by an
 * injectable [timeSource], so tests exercising [io.github.compose.jindong.core.HapticManager.execute]
 * (which now awaits the handle) can advance virtual time deterministically via `runTest`'s
 * `testTimeSource` instead of relying on a real monotonic clock.
 *
 * @param playbackDuration How long each [executeAsync] handle reports itself active. Defaults to a
 *   short window so tests only need to advance the virtual clock past it to observe completion.
 * @param timeSource Clock the handles measure elapsed playback against. Pass `runTest`'s
 *   `testTimeSource` so `delay(...)` advances it; defaults to real monotonic for non-timed tests.
 */
class FakeHapticExecutor(
  override val isSupported: Boolean = true,
  override val hasAmplitudeControl: Boolean = true,
  private val playbackDuration: Duration = 10.milliseconds,
  private val timeSource: TimeSource = TimeSource.Monotonic,
) : HapticExecutor {

  private val _executedPatterns = mutableListOf<HapticPattern>()
  private val _asyncExecutedPatterns = mutableListOf<HapticPattern>()
  private val _issuedHandles = mutableListOf<FakeHapticHandle>()
  private var _releaseCalled = false

  val executedPatterns: List<HapticPattern> get() = _executedPatterns.toList()
  val asyncExecutedPatterns: List<HapticPattern> get() = _asyncExecutedPatterns.toList()

  /** Handles handed out by [executeAsync], in order, so tests can inspect in-flight cancellation. */
  val issuedHandles: List<FakeHapticHandle> get() = _issuedHandles.toList()
  val releaseCalled: Boolean get() = _releaseCalled

  override suspend fun execute(pattern: HapticPattern) {
    _executedPatterns.add(pattern)
    // Simulate minimal execution time
    delay(1)
  }

  override fun executeAsync(pattern: HapticPattern): HapticHandle {
    _asyncExecutedPatterns.add(pattern)
    return FakeHapticHandle(playbackDuration, timeSource).also { _issuedHandles.add(it) }
  }

  override fun release() {
    _releaseCalled = true
  }

  fun reset() {
    _executedPatterns.clear()
    _asyncExecutedPatterns.clear()
    _issuedHandles.clear()
    _releaseCalled = false
  }
}

/**
 * Fake implementation of [HapticHandle] for testing.
 *
 * Stays active until [cancel] is called or [playbackDuration] elapses on [timeSource], mirroring the
 * real platform handles' duration-estimate completion (there is no OS completion callback).
 */
class FakeHapticHandle(
  private val playbackDuration: Duration = 10.milliseconds,
  timeSource: TimeSource = TimeSource.Monotonic,
) : HapticHandle {
  private var _cancelled = false
  private val start = timeSource.markNow()

  val cancelled: Boolean get() = _cancelled
  override val isActive: Boolean get() = !_cancelled && start.elapsedNow() < playbackDuration

  override fun cancel() {
    _cancelled = true
  }
}
