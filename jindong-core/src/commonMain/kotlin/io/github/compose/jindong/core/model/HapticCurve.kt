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

/** An absolute normalized value at a time relative to the event start. */
public data class HapticControlPoint(val timeMs: Long, val value: Float) {
  init {
    require(timeMs >= 0L) { "Control point time must be non-negative, was $timeMs" }
    require(value.isFinite() && value in 0f..1f) { "Control point value must be finite and in 0..1, was $value" }
  }
}

/** Immutable, linearly interpolated control points, from 0ms through [durationMs]. */
public class HapticCurve(points: List<HapticControlPoint>) {
  init {
    checkedControlPointCount(points.size.toLong(), "curve")
    require(points.size >= 2) { "A curve requires at least two control points" }
  }

  public val points: List<HapticControlPoint> = object : AbstractList<HapticControlPoint>() {
    private val snapshot = points.toList()
    override val size: Int get() = snapshot.size
    override fun get(index: Int): HapticControlPoint = snapshot[index]
  }

  init {
    require(this.points.first().timeMs == 0L) { "A curve must start at 0ms" }
    require(this.points.zipWithNext().all { (left, right) -> left.timeMs < right.timeMs }) {
      "Control point times must strictly increase"
    }
  }

  public val durationMs: Long get() = points.last().timeMs

  /** Returns the linear value at [timeMs], holding endpoint values outside the curve. */
  public fun valueAt(timeMs: Long): Float {
    if (timeMs <= 0L) return points.first().value
    if (timeMs >= durationMs) return points.last().value
    var low = 0
    var high = points.lastIndex
    while (high - low > 1) {
      val middle = low + (high - low) / 2
      if (points[middle].timeMs <= timeMs) low = middle else high = middle
    }
    val left = points[low]
    val right = points[high]
    val fraction = (timeMs - left.timeMs).toDouble() / (right.timeMs - left.timeMs).toDouble()
    return (left.value + (right.value - left.value) * fraction).toFloat()
  }

  override fun equals(other: Any?): Boolean = other is HapticCurve && points == other.points
  override fun hashCode(): Int = points.hashCode()
  override fun toString(): String = "HapticCurve(points=$points)"
}
