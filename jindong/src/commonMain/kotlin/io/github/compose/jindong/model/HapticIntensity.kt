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
package io.github.compose.jindong.model
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

/**
 * Pre-defined haptic intensity levels and custom intensity support.
 *
 * This sealed class provides both convenient pre-defined intensity levels
 * and the flexibility to specify custom intensity values.
 *
 * ## Pre-defined Levels
 * - [LIGHT]: Subtle feedback (0.25)
 * - [MEDIUM]: Standard feedback (0.5)
 * - [STRONG]: Noticeable feedback (0.75)
 * - [HIGH]: Maximum feedback (1.0)
 *
 * ## Custom Intensity
 * Use [Custom] to specify any value between 0.0 and 1.0.
 * Values outside this range are automatically coerced.
 *
 * ## Usage
 * ```kotlin
 * // Using pre-defined levels
 * Haptic(duration = 100.ms, intensity = HapticIntensity.STRONG)
 *
 * // Using custom intensity
 * Haptic(duration = 100.ms, intensity = HapticIntensity.Custom(0.65f))
 *
 * // Gradual fade example
 * Repeat(count = 5) { index ->
 *     val fade = HapticIntensity.Custom(1.0f - (index * 0.2f))
 *     Haptic(duration = 50.ms, intensity = fade.value)
 * }
 * ```
 *
 * @property value The intensity value in range 0.0 to 1.0
 */
sealed class HapticIntensity(val value: Float) {

  data object LIGHT : HapticIntensity(0.25f)

  data object MEDIUM : HapticIntensity(0.5f)

  data object STRONG : HapticIntensity(0.75f)

  data object HIGH : HapticIntensity(1.0f)

  data class Custom(private val customValue: Float) : HapticIntensity(customValue.coerceIn(0f, 1f))
}
