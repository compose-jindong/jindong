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

import kotlin.math.roundToLong

/**
 * Returns a new pattern with every event's timeline scaled by [factor].
 *
 * Each event's edges are scaled and rounded independently, then the duration is derived as the gap
 * between them: `newDuration = round((start + duration) * f) - round(start * f)`. Rounding the two
 * edges rather than start-and-duration keeps adjacent event boundaries aligned — a shared boundary
 * rounds to one value, so events that touched before still touch after. Intensity and iOS parameters
 * (including ADSR envelope times) are carried unchanged: the envelope shapes each event internally
 * and is independent of where the event sits on the pattern timeline.
 *
 * @param factor positive timeline multiplier
 */
public fun HapticPattern.timeStretch(factor: Float): HapticPattern {
  require(factor > 0f) { "factor must be positive, was $factor" }
  return copy(
    events = events.map { event ->
      val newStart = (event.startTimeMs * factor).roundToLong()
      val newEnd = ((event.startTimeMs + event.durationMs) * factor).roundToLong()
      event.copy(startTimeMs = newStart, durationMs = newEnd - newStart)
    },
  )
}
