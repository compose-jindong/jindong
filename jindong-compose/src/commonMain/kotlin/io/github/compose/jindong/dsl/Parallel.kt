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
import io.github.compose.jindong.core.element.ParallelElement

/**
 * Starts each child at the same position. The longest child determines the duration.
 * Use [Sequence] to give a branch an internal delay or multiple consecutive events.
 */
@Composable
fun JindongScope.Parallel(content: @Composable JindongScope.() -> Unit) {
  ComposeNode<ParallelElement, JindongApplier>(
    factory = { ParallelElement() },
    update = { },
    content = { content() },
  )
}
