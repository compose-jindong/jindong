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
import io.github.compose.jindong.core.element.DelayElement
import kotlin.time.Duration

/**
 * Pauses the pattern for the specified duration without vibrating.
 *
 * ```
 * Jindong(trigger) {
 *     Haptic(50.ms)
 *     Delay(100.ms)  // wait 100ms
 *     Haptic(50.ms)  // starts at 150ms
 * }
 * ```
 *
 * @param duration How long to wait before the next event
 */
@Composable
fun JindongScope.Delay(duration: Duration) {
  ComposeNode<DelayElement, JindongApplier>(
    factory = { DelayElement(duration.inWholeMilliseconds) },
    update = { },
  )
}
