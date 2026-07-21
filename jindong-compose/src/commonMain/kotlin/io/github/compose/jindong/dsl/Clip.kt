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
import io.github.compose.jindong.core.element.PatternElement
import io.github.compose.jindong.core.model.HapticPattern

/**
 * Places a prebuilt [pattern] on the timeline at the current position.
 *
 * A clip is a self-contained unit, typically an algebra result, dropped next to inline nodes:
 *
 * ```
 * Jindong(speed) {
 *     Sequence {
 *         Clip(heartbeat.timeStretch(1f / speed))
 *         Delay(200.ms)
 *         Haptic(60.ms, HapticIntensity.MEDIUM)
 *     }
 * }
 * ```
 *
 * The value is captured when the node is inserted. [Jindong]'s composition is single-shot, so a new
 * [pattern] is only picked up when the whole pattern recompiles; thread it through the trigger keys
 * to make it live:
 *
 * ```
 * Jindong(pattern) { Clip(pattern) }
 * ```
 *
 * [HapticPattern] is a data class, so the key comparison is structural.
 *
 * @param pattern The prebuilt pattern to place on the timeline
 */
@Composable
fun JindongScope.Clip(pattern: HapticPattern) {
  // update is empty because the composition never recomposes: the value captured by factory is final
  // for this compile pass (see Jindong's single-shot compilePattern).
  ComposeNode<PatternElement, JindongApplier>(
    factory = { PatternElement(pattern) },
    update = { },
  )
}
