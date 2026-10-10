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
package io.github.compose.jindong.dsl

import androidx.compose.runtime.Composable
import io.github.compose.jindong.JindongScope
import io.github.compose.jindong.core.dsl.buildHapticPattern
import io.github.compose.jindong.core.model.HapticCurve
import io.github.compose.jindong.core.model.HapticIntensity
import kotlin.time.Duration

/**
 * Adds positive-duration feedback. Curves replace the corresponding fixed values.
 * Values are captured for this compile pass; include changing values in Jindong's trigger keys.
 */
@Composable
public fun JindongScope.Continuous(
  duration: Duration,
  intensity: HapticIntensity = HapticIntensity.MEDIUM,
  sharpness: Float = 0.5f,
  intensityCurve: HapticCurve? = null,
  sharpnessCurve: HapticCurve? = null,
) {
  Clip(buildHapticPattern { continuous(duration, intensity, sharpness, intensityCurve, sharpnessCurve) })
}
