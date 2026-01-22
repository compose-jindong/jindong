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

import io.github.compose.jindong.core.model.ScheduledHapticEvent

/**
 * A leaf element representing a time delay with no haptic output.
 *
 * This element has no children and returns an empty list when [collectEvents] is called.
 * The duration is used by parent elements (e.g., [SequenceElement]) to advance timing.
 *
 * @property durationMs Duration of the delay in milliseconds
 */
public class DelayElement(
  public val durationMs: Long,
) : HapticElement {

  init {
    require(durationMs >= 0) { "durationMs must be non-negative, but was $durationMs" }
  }

  override val children: MutableList<HapticElement> = mutableListOf()

  override fun collectEvents(startTimeMs: Long): List<ScheduledHapticEvent> = emptyList()

  override fun totalDurationMs(startTimeMs: Long): Long = durationMs
}
