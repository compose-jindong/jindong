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
 * Repeats the content [count] times, providing the 0-based iteration index to the content block.
 *
 * Unlike [Repeat], this function generates N different child nodes at composition time,
 * allowing each iteration to have different parameters based on the index(dynamic pattern generation).
 *
 * ```
 * Jindong(trigger) {
 *     RepeatWithIndex(count = 5) { index ->
 *         val fade = HapticIntensity.Custom(1.0f - (index * 0.2f))
 *         Haptic(50.ms, fade)  // Intensities: 1.0, 0.8, 0.6, 0.4, 0.2
 *     }
 * }
 * ```
 *
 *
 * @param count Number of times to repeat (must be non-negative)
 * @param content The pattern to repeat, receiving the 0-based iteration index
 */
@Composable
fun JindongScope.RepeatWithIndex(
  count: Int,
  content: @Composable JindongScope.(index: Int) -> Unit,
) {
  ComposeNode<SequenceElement, JindongApplier>(
    factory = { SequenceElement() },
    update = { },
    content = {
      repeat(count) { index ->
        content(index)
      }
    },
  )
}
