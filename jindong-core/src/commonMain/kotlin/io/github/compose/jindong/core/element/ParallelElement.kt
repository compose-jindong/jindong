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
import io.github.compose.jindong.core.model.checkedTimeAdd

/** Starts every child at the same time and retains the longest child's complete duration. */
class ParallelElement : HapticElement {
  override val children: MutableList<HapticElement> = mutableListOf()

  override fun collectEvents(startTimeMs: Long): List<ScheduledHapticEvent> {
    checkedTimeAdd(startTimeMs, totalDurationMs(startTimeMs), "Parallel end")
    val count = expandedEventCount()
    return buildList(count) {
      children.forEach { addAll(it.collectEvents(startTimeMs)) }
    }
  }

  override fun totalDurationMs(startTimeMs: Long): Long = children.maxOfOrNull { it.totalDurationMs(startTimeMs) } ?: 0L
}
