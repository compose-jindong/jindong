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
package io.github.compose.jindong.core.executor

import io.github.compose.jindong.core.model.HapticPattern

/**
 * Platform-specific haptic executor interface.
 * Each platform (Android, iOS) provides its own actual implementation.
 *
 * This interface is used internally by [HapticManager] and the Compose integration.
 * For most use cases, prefer using [HapticManager] or the `play()` extension function
 * on [HapticPattern] instead of directly interacting with this interface.
 *
 * @see createHapticExecutor
 */
interface HapticExecutor {

  /**
   * Checks if haptic feedback is supported on this device.
   */
  val isSupported: Boolean

  /**
   * Whether the actuator can render intermediate (non-binary) amplitudes.
   *
   * - **Android**: reflects `Vibrator.hasAmplitudeControl()`. ERM motors round every non-zero
   *   amplitude up to 100%, so they report `false` and effects like fall ramps are lost; LRA
   *   motors report `true`.
   * - **iOS**: Core Haptics always renders continuous intensity, so this is always `true`.
   *   The "discrete vs. continuous" distinction is platform-specific; read the platform
   *   alongside this flag if the label matters (see the Compose `HapticCapabilities`).
   */
  val hasAmplitudeControl: Boolean

  /**
   * Executes haptic pattern. Cancellable via coroutine cancellation.
   * When cancelled, the ongoing vibration is stopped immediately.
   *
   * This is a suspend function that will complete when the haptic pattern finishes playing.
   * The implementation should handle platform-specific conversion of the pattern and
   * any necessary error handling.
   *
   * @param pattern The haptic pattern to execute
   */
  suspend fun execute(pattern: HapticPattern)

  /**
   * Executes with explicit cancellation handle.
   * Caller is responsible for lifecycle management.
   *
   * Use this method when you need fine-grained control over haptic execution,
   * such as stopping a pattern before it completes.
   *
   * @param pattern The haptic pattern to execute
   * @return A [HapticHandle] that can be used to cancel the execution
   */
  fun executeAsync(pattern: HapticPattern): HapticHandle

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
