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

import io.github.compose.jindong.model.ScheduledHapticEvent

/**
 * A container node that executes its children sequentially.
 *
 * Each child starts after the previous one completes. The timing is calculated
 * by tracking the maximum end time of events from each child.
 *
 * Special handling for [DelayNode]: advances time without generating events.
 *
 * Example:
 * ```
 * Sequence {
 *     Haptic(100.ms)   // starts at 0ms, ends at 100ms
 *     Delay(50.ms)     // no event, advances to 150ms
 *     Haptic(200.ms)   // starts at 150ms, ends at 350ms
 * }
 * ```
 */
internal class SequenceNode : HapticNode {
  override val children: MutableList<HapticNode> = mutableListOf()

  override fun collectEvents(startTimeMs: Long): List<ScheduledHapticEvent> {
    val (_, events) = children.fold(
      initial = TimingState(currentTimeMs = startTimeMs, events = emptyList()),
    ) { state, child ->
      processChildNode(child, state)
    }
    return events
  }

  private fun processChildNode(child: HapticNode, state: TimingState): TimingState {
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
