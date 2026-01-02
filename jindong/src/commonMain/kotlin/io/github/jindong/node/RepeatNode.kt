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
package io.github.jindong.node

/**
 * A container node that repeats its children N times sequentially.
 *
 * Each repetition starts after the previous one completes. The timing is calculated
 * by tracking the maximum end time of events from each iteration.
 *
 * Special handling for [DelayNode]: advances time without generating events.
 *
 * Example:
 * ```
 * Repeat(count = 3) {
 *     Haptic(50.ms)   // starts at 0ms, 100ms, 200ms
 *     Delay(50.ms)    // advances to 100ms, 200ms, 300ms
 * }
 * ```
 *
 * @property count Number of times to repeat the children
 */
internal class RepeatNode(
  val count: Int,
) : HapticNode {
  override val children: MutableList<HapticNode> = mutableListOf()

  override fun collectEvents(startTimeMs: Long): List<ScheduledHapticEvent> {
    if (count <= 0) return emptyList()

    return (0 until count)
      .fold(
        initial = IterationState(startTimeMs, emptyList()),
      ) { state, _ ->
        val (events, endTimeMs) = collectIterationEvents(state.nextStartTimeMs)
        IterationState(
          nextStartTimeMs = endTimeMs,
          events = state.events + events,
        )
      }
      .events
  }

  /**
   * Collects events from one iteration of children.
   * Returns both the events and the end time of this iteration (including delays).
   */
  private fun collectIterationEvents(startTimeMs: Long): IterationResult {
    val (currentTimeMs, events) = children.fold(
      initial = ChildState(startTimeMs, emptyList()),
    ) { state, child ->
      when (child) {
        is DelayNode -> state.copy(currentTimeMs = state.currentTimeMs + child.durationMs)

        else -> {
          val childEvents = child.collectEvents(state.currentTimeMs)
          val nextTime = childEvents
            .maxOfOrNull { it.startTimeMs + it.durationMs }
            ?: state.currentTimeMs
          ChildState(
            currentTimeMs = nextTime,
            events = state.events + childEvents,
          )
        }
      }
    }
    return IterationResult(events = events, endTimeMs = currentTimeMs)
  }

  private data class IterationState(
    val nextStartTimeMs: Long,
    val events: List<ScheduledHapticEvent>,
  )

  private data class ChildState(
    val currentTimeMs: Long,
    val events: List<ScheduledHapticEvent>,
  )

  private data class IterationResult(
    val events: List<ScheduledHapticEvent>,
    val endTimeMs: Long,
  )
}
