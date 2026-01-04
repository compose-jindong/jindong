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
package io.github.compose.jindong.executor

import io.github.compose.jindong.model.HapticPattern

/**
 * Platform-specific haptic executor interface.
 * Each platform (Android, iOS) provides its own actual implementation:
 *
 * This is an internal API used by [Jindong] composable and [JindongState].
 * Users should not interact with this interface directly.
 *
 * @see createHapticExecutor
 */
internal interface HapticExecutor {
  /**
   * Executes the given haptic pattern.
   *
   * This is a suspend function that will complete when the haptic pattern finishes playing.
   * The implementation should handle platform-specific conversion of the pattern and
   * any necessary error handling.
   *
   * @param pattern The haptic pattern to execute
   * @throws Exception if haptic execution fails
   */
  suspend fun execute(pattern: HapticPattern)

  /**
   * Checks if haptic feedback is supported on this device.
   *
   * @return true if haptics are supported and available, false otherwise
   */
  fun isSupported(): Boolean

  /**
   * Releases any resources held by the executor.
   *
   * This should be called when the executor is no longer needed, typically in
   * a DisposableEffect or similar lifecycle-aware component.
   *
   * After calling this method, the executor should not be used anymore.
   */
  fun release()
}
