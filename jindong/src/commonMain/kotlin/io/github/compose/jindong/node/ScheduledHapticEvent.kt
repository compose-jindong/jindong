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
package io.github.compose.jindong.node

/**
 * Represents a scheduled haptic event with absolute timing.
 *
 * @property startTimeMs Absolute start time in milliseconds (relative to pattern start)
 * @property durationMs Duration of the haptic event in milliseconds
 * @property intensity Vibration intensity from 0.0 to 1.0
 * @property iosParameters iOS Core Haptics parameters (ignored on Android)
 */
internal data class ScheduledHapticEvent(
  val startTimeMs: Long,
  val durationMs: Long,
  val intensity: Float,
  val iosParameters: IosHapticParameters? = null,
)
