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
import io.github.compose.jindong.executor.LocalHapticExecutor

/**
 * Platform a haptic capability reading was taken on.
 */
enum class HapticPlatform {
  Android,
  Ios,
}

/**
 * Snapshot of the current device's haptic capabilities, read from the active [HapticExecutor].
 *
 * @property isSupported whether haptic feedback is available at all on this device.
 * @property hasAmplitudeControl whether the actuator renders intermediate amplitudes. On Android
 *   this is `false` for ERM motors (every non-zero amplitude rounds up to 100%) and `true` for LRA
 *   motors. On iOS Core Haptics is always continuous, so this is always `true`; pair it with
 *   [platform] when the displayed label needs to distinguish "Yes/No" (Android) from
 *   "Continuous" (iOS).
 * @property platform the platform this reading came from.
 */
data class HapticCapabilities(
  val isSupported: Boolean,
  val hasAmplitudeControl: Boolean,
  val platform: HapticPlatform,
)

/**
 * Reads the current device's [HapticCapabilities] from the haptic executor provided by
 * [JindongProvider]. Must be called inside a `JindongProvider { }` scope.
 *
 * Example:
 * ```
 * JindongProvider {
 *     val capabilities = rememberHapticCapabilities()
 *     // capabilities.isSupported, capabilities.hasAmplitudeControl, capabilities.platform
 * }
 * ```
 */
@Composable
fun rememberHapticCapabilities(): HapticCapabilities {
  val executor = LocalHapticExecutor.current
  val platform = hapticPlatform()
  return remember(executor, platform) {
    HapticCapabilities(
      isSupported = executor.isSupported,
      hasAmplitudeControl = executor.hasAmplitudeControl,
      platform = platform,
    )
  }
}

/**
 * Returns the [HapticPlatform] the app is currently running on.
 */
internal expect fun hapticPlatform(): HapticPlatform
