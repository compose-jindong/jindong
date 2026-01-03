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
import androidx.compose.runtime.ComposeNode
import io.github.compose.jindong.JindongScope
import io.github.compose.jindong.compose.JindongApplier
import io.github.compose.jindong.model.HapticIntensity
import io.github.compose.jindong.node.HapticEventNode
import kotlin.time.Duration

/**
 * Defines a single haptic event (vibration) in the haptic pattern.
 *
 * This composable creates a [HapticEventNode] in the composition tree,
 * which will be converted to a [ScheduledHapticEvent] when the pattern is compiled.
 *
 * Example:
 * ```
 * Jindong(trigger) {
 *     Haptic(duration = 100.ms)
 *     Haptic(duration = 50.ms, intensity = HapticIntensity.STRONG)
 * }
 * ```
 *
 * @param duration Duration of the haptic event
 * @param intensity Vibration intensity level (default: [HapticIntensity.MEDIUM])
 */
@Composable
fun JindongScope.Haptic(
  duration: Duration,
  intensity: HapticIntensity = HapticIntensity.MEDIUM,
) {
  ComposeNode<HapticEventNode, JindongApplier>(
    factory = { HapticEventNode(duration.inWholeMilliseconds, intensity) },
    update = { },
  )
}
