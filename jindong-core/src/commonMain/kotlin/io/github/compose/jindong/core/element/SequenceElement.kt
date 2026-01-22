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
 * A container element that executes its children sequentially.
 *
 * Each child starts after the previous one completes. The timing is calculated
 * by tracking the maximum end time of events from each child.
 *
 * Special handling for [DelayElement]: advances time without generating events.
 *
 * Example:
 * ```
 * sequence {
 *     haptic(100.ms)   // starts at 0ms, ends at 100ms
 *     delay(50.ms)     // no event, advances to 150ms
 *     haptic(200.ms)   // starts at 150ms, ends at 350ms
 * }
 * ```
 */
public class SequenceElement : HapticElement {
  override val children: MutableList<HapticElement> = mutableListOf()

  override fun collectEvents(startTimeMs: Long): List<ScheduledHapticEvent> {
    val (_, events) = children.fold(
      initial = TimingState(currentTimeMs = startTimeMs, events = emptyList()),
    ) { state, child ->
      processChildElement(child, state)
    }
    return events
  }

  private fun processChildElement(child: HapticElement, state: TimingState): TimingState {
    val childEvents = child.collectEvents(state.currentTimeMs)
    val nextStartTime = state.currentTimeMs + child.totalDurationMs(state.currentTimeMs)
    return TimingState(
      currentTimeMs = nextStartTime,
      events = state.events + childEvents,
    )
  }

  override fun totalDurationMs(startTimeMs: Long): Long = children.sumOf { it.totalDurationMs(startTimeMs) }

  private data class TimingState(
    val currentTimeMs: Long,
    val events: List<ScheduledHapticEvent>,
  )
}
