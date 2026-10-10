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
package io.github.compose.jindong.core.dsl

import io.github.compose.jindong.core.model.HapticControlPoint
import io.github.compose.jindong.core.model.HapticCurve
import io.github.compose.jindong.core.model.checkedControlPointCount
import io.github.compose.jindong.core.toHapticMilliseconds
import kotlin.time.Duration

/** Builds a linear curve of absolute normalized values, relative to its event start. */
@HapticDslMarker
public class HapticCurveScope internal constructor() {
  private val points = mutableListOf<HapticControlPoint>()

  public fun point(time: Duration, value: Float) {
    checkedControlPointCount(points.size.toLong() + 1L, "curve")
    points.add(HapticControlPoint(time.toHapticMilliseconds(), value))
  }

  internal fun build(): HapticCurve = HapticCurve(points)
}

/** Requires at least two points, starting at 0ms, with strictly increasing rounded times. */
public fun hapticCurve(block: HapticCurveScope.() -> Unit): HapticCurve = HapticCurveScope().apply(block).build()
