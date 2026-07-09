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

/**
 * Returns a new pattern with every event's intensity multiplied by [factor].
 *
 * Every level is mapped to [HapticIntensity.Custom], since a scaled predefined level (a dimmed
 * `STRONG`, say) has no predefined equivalent. `Custom` clamps to [0, 1], so `factor` above 1 is
 * safe and saturates rather than overflowing. Timing and iOS parameters are unchanged.
 *
 * @param factor non-negative intensity multiplier
 */
public fun HapticPattern.scaleIntensity(factor: Float): HapticPattern {
  require(factor >= 0f) { "factor must be non-negative, was $factor" }
  return copy(
    events = events.map { event ->
      event.copy(intensity = HapticIntensity.Custom(event.intensity.value * factor))
    },
  )
}
