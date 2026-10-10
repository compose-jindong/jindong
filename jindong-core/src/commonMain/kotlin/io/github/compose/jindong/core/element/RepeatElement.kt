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
package io.github.compose.jindong.core.element

import io.github.compose.jindong.core.model.ScheduledHapticEvent
import io.github.compose.jindong.core.model.checkedTimeAdd
import io.github.compose.jindong.core.model.checkedTimeMultiply

/**
 * A container element that repeats its children N times sequentially.
 *
 * Each repetition starts after the previous iteration's complete logical duration, including silence.
 *
 * Special handling for [DelayElement]: advances time without generating events.
 *
 * Example:
 * ```
 * repeat(count = 3) {
 *     haptic(50.ms)   // starts at 0ms, 100ms, 200ms
 *     delay(50.ms)    // advances to 100ms, 200ms, 300ms
 * }
 * ```
 *
 * @property count Number of times to repeat the children
 */
class RepeatElement(
  val count: Int,
) : HapticElement {
  init {
    require(count >= 0) { "count must be non-negative, but was $count" }
  }

  override val children: MutableList<HapticElement> = mutableListOf()

  override fun collectEvents(startTimeMs: Long): List<ScheduledHapticEvent> {
    checkedTimeAdd(startTimeMs, totalDurationMs(startTimeMs), "Repeat end")
    val eventCount = expandedEventCount()
    if (eventCount == 0) return emptyList()
    return buildList(eventCount) {
      var cursor = startTimeMs
      repeat(count) {
        children.forEach { child ->
          addAll(child.collectEvents(cursor))
          cursor = checkedTimeAdd(cursor, child.totalDurationMs(cursor), "Repeat child end")
        }
      }
    }
  }

  override fun totalDurationMs(startTimeMs: Long): Long {
    if (count == 0) return 0L
    val duration = children.fold(0L) { duration, child ->
      checkedTimeAdd(duration, child.totalDurationMs(startTimeMs), "Repeat iteration duration")
    }
    return checkedTimeMultiply(duration, count, "Repeat duration")
  }
}
