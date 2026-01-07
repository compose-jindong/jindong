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
    require(count >= 0) { "count must be non-negative, but was $count" }

    return buildList {
      var currentTime = startTimeMs

      repeat(count) {
        val (events, endTimeMs) = collectIterationEvents(currentTime)
        addAll(events)
        currentTime = endTimeMs
      }
    }
  }

  /**
   * Collects events from one iteration of children.
   * Returns both the events and the end time of this iteration (including delays).
   */
  private fun collectIterationEvents(startTimeMs: Long): IterationResult {
    val (currentTimeMs, events) = children.fold(
      initial = ChildState(startTimeMs, emptyList()),
    ) { state, child ->
      val childEvents = child.collectEvents(state.currentTimeMs)
      val nextTime = state.currentTimeMs + child.totalDurationMs(state.currentTimeMs)
      ChildState(
        currentTimeMs = nextTime,
        events = state.events + childEvents,
      )
    }
    return IterationResult(events = events, endTimeMs = currentTimeMs)
  }

  private data class ChildState(
    val currentTimeMs: Long,
    val events: List<ScheduledHapticEvent>,
  )

  override fun totalDurationMs(startTimeMs: Long): Long {
    if (count == 0) return 0L

    val singleLoopDuration = children.sumOf { child ->
      child.totalDurationMs(startTimeMs)
    }

    return singleLoopDuration * count
  }

  private data class IterationResult(
    val events: List<ScheduledHapticEvent>,
    val endTimeMs: Long,
  )
}
