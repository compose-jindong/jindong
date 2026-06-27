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
package io.github.compose.jindong

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.compose.jindong.core.model.HapticPattern

/**
 * Compiles and memoizes a haptic DSL pattern.
 *
 * The pattern is recompiled whenever any of [keys] change, so a [content] block that captures
 * mutable state stays in sync with that state instead of returning a stale first-compile result.
 *
 * @param keys Inputs that invalidate the memoized pattern when changed
 * @param content DSL block defining the haptic pattern
 * @return The compiled [HapticPattern]
 *
 * @see [Jindong] for trigger-based pattern execution
 * @see [compilePattern] for immediate pattern compilation without memoization
 */
@Composable
internal fun rememberHapticPattern(
  vararg keys: Any?,
  content: @Composable JindongScope.() -> Unit,
): HapticPattern = remember(*keys) {
  compilePattern(content)
}
