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
@file:Suppress("ktlint:standard:filename")

package io.github.compose.jindong.core.executor

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Shared, pull-based expiry judgement for platform [HapticHandle]s.
 *
 * The OS gives no per-effect completion callback (Android [android.os.Vibrator] and iOS
 * `CHHapticPatternPlayerProtocol` both lack one), so "completed" can only be *estimated* from the
 * expected playback length. This holds the start [TimeMark] and the total duration and answers, on
 * each [isExpired] read, whether enough monotonic time has elapsed — no coroutine, scope, or timer.
 *
 * Both [io.github.compose.jindong.core.executor.AndroidHapticHandle] and
 * [io.github.compose.jindong.core.executor.IosHapticHandle] delegate to this so they share identical
 * expiry semantics across platforms.
 *
 * @param totalDurationMs Expected playback length. A non-positive value means nothing is playing, so
 *   the handle is expired from the start (a silent/empty pattern is never active).
 * @param timeSource Monotonic clock; injectable so tests can advance time deterministically.
 */
internal class HandleExpiry(
  totalDurationMs: Long,
  timeSource: TimeSource = TimeSource.Monotonic,
) {
  private val totalDuration: Duration = totalDurationMs.coerceAtLeast(0L).milliseconds
  private val start: TimeMark = timeSource.markNow()

  /** True once the estimated playback window has elapsed (best-effort; ±OS scheduling jitter). */
  val isExpired: Boolean
    get() = start.elapsedNow() >= totalDuration
}
