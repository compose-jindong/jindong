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
package io.github.compose.jindong.core.model

internal const val MAX_PATTERN_EVENTS: Int = 10_000
internal const val MAX_PATTERN_CONTROL_POINTS: Int = 100_000

internal fun List<ScheduledHapticEvent>.eventSpanMs(): Long {
  checkedEventCount(size.toLong(), "pattern")
  return maxOfOrNull { event -> checkedTimeAdd(event.startTimeMs, event.durationMs, "event end") } ?: 0L
}

internal fun checkedTimeAdd(left: Long, right: Long, location: String): Long {
  require(left >= 0L && right >= 0L && left <= Long.MAX_VALUE - right) {
    "$location: non-negative millisecond sum overflows or has negative input ($left + $right)"
  }
  return left + right
}

internal fun checkedTimeMultiply(value: Long, count: Int, location: String): Long {
  require(value >= 0L && count >= 0 && (count == 0 || value <= Long.MAX_VALUE / count)) {
    "$location: millisecond duration overflows or has negative input ($value * $count)"
  }
  return value * count
}

internal fun checkedEventCount(count: Long, location: String): Int {
  require(count in 0L..MAX_PATTERN_EVENTS.toLong()) {
    "$location: expanded event count $count exceeds limit $MAX_PATTERN_EVENTS"
  }
  return count.toInt()
}

internal fun checkedControlPointCount(count: Long, location: String): Int {
  require(count in 0L..MAX_PATTERN_CONTROL_POINTS.toLong()) {
    "$location: control point count $count exceeds limit $MAX_PATTERN_CONTROL_POINTS"
  }
  return count.toInt()
}

internal fun List<ScheduledHapticEvent>.controlPointCount(): Int = fold(0) { count, event ->
  checkedControlPointCount(
    count.toLong() + (event.intensityCurve?.points?.size ?: 0) + (event.sharpnessCurve?.points?.size ?: 0),
    "pattern",
  )
}
