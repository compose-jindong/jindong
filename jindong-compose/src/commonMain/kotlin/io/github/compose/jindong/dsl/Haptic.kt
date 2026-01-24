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
import io.github.compose.jindong.model.HapticProperties
import io.github.compose.jindong.model.toIosHapticParameters
import io.github.compose.jindong.node.HapticEventNode
import kotlin.time.Duration

/**
 * Emits a vibration for the specified duration.
 *
 * ```
 * Jindong(trigger) {
 *     Haptic(100.ms)                                    // medium intensity
 *     Haptic(50.ms, intensity = HapticIntensity.STRONG) // strong intensity
 * }
 * ```
 *
 * @param duration How long the vibration lasts
 * @param intensity Vibration strength (default: [HapticIntensity.MEDIUM])
 * @param properties iOS Core Haptics properties (ignored on Android)
 */
@Composable
fun JindongScope.Haptic(
  duration: Duration,
  intensity: HapticIntensity = HapticIntensity.MEDIUM,
  properties: HapticProperties? = null,
) {
  ComposeNode<HapticEventNode, JindongApplier>(
    factory = {
      HapticEventNode(
        durationMs = duration.inWholeMilliseconds,
        intensity = intensity,
        iosParameters = properties?.toIosHapticParameters(),
      )
    },
    update = { },
  )
}
