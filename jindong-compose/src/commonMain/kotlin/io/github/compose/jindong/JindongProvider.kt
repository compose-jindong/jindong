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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import io.github.compose.jindong.executor.LocalHapticExecutor
import io.github.compose.jindong.executor.createHapticExecutor

/**
 * Provides [HapticExecutor] to the composition tree.
 *
 * Wrap your app content with this provider to enable haptic feedback.
 * The platform-specific context is automatically obtained:
 * - Android: Uses `LocalContext.current`
 * - iOS: No context needed
 *
 * Example:
 * ```
 * @Composable
 * fun App() {
 *     JindongProvider {
 *         // Your app content
 *         Jindong(trigger) {
 *             Haptic(100.ms)
 *         }
 *     }
 * }
 * ```
 *
 * @param content The content to provide the executor to
 */
@Composable
fun JindongProvider(
  content: @Composable () -> Unit,
) {
  val context = platformContext()
  val executor = remember(context) { createHapticExecutor(context) }

  DisposableEffect(Unit) {
    onDispose {
      executor.release()
    }
  }

  CompositionLocalProvider(LocalHapticExecutor provides executor) {
    content()
  }
}
