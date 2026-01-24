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
 * Base interface for all haptic elements in the pattern tree.
 *
 * Each element can have children and is responsible for collecting
 * scheduled haptic events based on the given start time.
 *
 * This is the core building block of the haptic pattern system,
 * used by both the Kotlin DSL and Compose Runtime integration.
 */
interface HapticElement {
  /**
   * Child elements managed by this element.
   * Container elements maintain their children here.
   * Leaf elements return an empty list.
   */
  val children: MutableList<HapticElement>

  /**
   * Collects haptic events from this element and its children.
   *
   * @param startTimeMs The absolute start time for this element (relative to pattern start)
   * @return List of scheduled haptic events
   */
  fun collectEvents(startTimeMs: Long): List<ScheduledHapticEvent>

  /**
   * Returns the total duration of this element in milliseconds.
   *
   * This includes all haptic events AND delays, which is necessary for proper
   * timing calculation when nesting structures like Sequence inside Repeat.
   *
   * @param startTimeMs The absolute start time for this element (relative to pattern start)
   * @return Total duration from start time until all children complete
   */
  fun totalDurationMs(startTimeMs: Long): Long
}
