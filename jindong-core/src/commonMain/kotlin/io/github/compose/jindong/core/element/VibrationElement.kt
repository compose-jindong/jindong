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
package io.github.compose.jindong.core.element

import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.model.IosHapticParameters
import io.github.compose.jindong.core.model.ScheduledHapticEvent

/**
 * A leaf element representing a single haptic event (vibration).
 *
 * This element has no children and returns a single [ScheduledHapticEvent]
 * when [collectEvents] is called.
 *
 * @property durationMs Duration of the haptic event in milliseconds
 * @property intensity Vibration intensity level
 * @property iosParameters iOS Core Haptics parameters (ignored on Android)
 */
public class VibrationElement(
  public val durationMs: Long,
  public val intensity: HapticIntensity,
  public val iosParameters: IosHapticParameters? = null,
) : HapticElement {

  init {
    require(durationMs >= 0) { "durationMs must be non-negative, but was $durationMs" }
  }

  override val children: MutableList<HapticElement> = mutableListOf()

  override fun collectEvents(startTimeMs: Long): List<ScheduledHapticEvent> = listOf(
    ScheduledHapticEvent(
      startTimeMs = startTimeMs,
      durationMs = durationMs,
      intensity = intensity,
      iosParameters = iosParameters,
    ),
  )

  override fun totalDurationMs(startTimeMs: Long): Long = durationMs
}
