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
 * Base interface for all haptic nodes in the composition tree.
 *
 * Each node can have children and is responsible for collecting
 * scheduled haptic events based on the given start time.
 */
internal interface HapticNode {
  /**
   * Child nodes managed by this node.
   * Container nodes maintain their children here.
   * Leaf nodes return an empty list.
   */
  val children: MutableList<HapticNode>

  /**
   * Collects haptic events from this node and its children.
   *
   * @param startTimeMs The absolute start time for this node (relative to pattern start)
   * @return List of scheduled haptic events
   */
  fun collectEvents(startTimeMs: Long): List<ScheduledHapticEvent>

  /**
   * Returns the total duration of this node in milliseconds.
   *
   * This includes all haptic events AND delays, which is necessary for proper
   * timing calculation when nesting structures like Sequence inside RepeatWithIndex.
   *
   * @param startTimeMs The absolute start time for this node (relative to pattern start)
   * @return Total duration from start time until all children complete
   */
  fun totalDurationMs(startTimeMs: Long): Long
}
