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
import io.github.compose.jindong.core.element.SequenceElement

/**
 * Groups events to run one after another.
 *
 * Useful for organizing patterns within [io.github.compose.jindong.dsl.Repeat] or other nested structures.
 *
 * ```
 * Jindong(trigger) {
 *     Repeat(2) {
 *         Sequence {
 *             Haptic(50.ms)
 *             Delay(50.ms)
 *         }
 *     }
 * }
 * ```
 *
 * @param content The pattern to execute sequentially
 */
@Composable
fun JindongScope.Sequence(content: @Composable JindongScope.() -> Unit) {
  ComposeNode<SequenceElement, JindongApplier>(
    factory = { SequenceElement() },
    update = { },
    content = { content() },
  )
}
