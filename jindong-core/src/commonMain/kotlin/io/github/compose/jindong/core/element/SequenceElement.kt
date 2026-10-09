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

/**
 * A container element that executes its children sequentially.
 *
 * Each child starts after the previous child's complete logical duration, including silence.
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
class SequenceElement : HapticElement {
  override val children: MutableList<HapticElement> = mutableListOf()

  override fun collectEvents(startTimeMs: Long): List<ScheduledHapticEvent> {
    checkedTimeAdd(startTimeMs, totalDurationMs(startTimeMs), "Sequence end")
    val count = expandedEventCount()
    return buildList(count) {
      var cursor = startTimeMs
      children.forEach { child ->
        addAll(child.collectEvents(cursor))
        cursor = checkedTimeAdd(cursor, child.totalDurationMs(cursor), "Sequence child end")
      }
    }
  }

  override fun totalDurationMs(startTimeMs: Long): Long = children.fold(0L) { duration, child ->
    checkedTimeAdd(duration, child.totalDurationMs(startTimeMs), "Sequence duration")
  }
}
