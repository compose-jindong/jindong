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

import io.github.compose.jindong.core.model.HapticPattern
import io.github.compose.jindong.core.model.ScheduledHapticEvent

/**
 * An element that includes an existing [HapticPattern].
 *
 * This element allows reusing previously built patterns within a new pattern,
 * enabling composition and reuse of haptic patterns.
 *
 * Example:
 * ```
 * val clickPattern = buildHapticPattern {
 *     haptic(50.ms, HapticIntensity.MEDIUM)
 * }
 *
 * val composedPattern = buildHapticPattern {
 *     include(clickPattern)
 *     delay(100.ms)
 *     include(clickPattern)
 * }
 * ```
 *
 * @property pattern The haptic pattern to include
 */
class PatternElement(
  val pattern: HapticPattern,
) : HapticElement {

  override val children: MutableList<HapticElement> = mutableListOf()

  override fun collectEvents(startTimeMs: Long): List<ScheduledHapticEvent> {
    // Offset all events in the pattern by the start time
    return pattern.events.map { event ->
      event.copy(startTimeMs = event.startTimeMs + startTimeMs)
    }
  }

  override fun totalDurationMs(startTimeMs: Long): Long {
    if (pattern.events.isEmpty()) return 0L

    // Find the maximum end time of all events in the pattern
    return pattern.events.maxOf { event ->
      event.startTimeMs + event.durationMs
    }
  }
}
