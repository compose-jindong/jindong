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

import io.github.compose.jindong.core.model.checkedEventCount

/** Check expansion before Repeat allocates or traverses every repetition. */
internal fun HapticElement.expandedEventCount(): Int = when (this) {
  is VibrationElement -> 1

  is DelayElement -> 0

  is PatternElement -> pattern.events.size

  is RepeatElement -> if (count == 0) {
    0
  } else {
    checkedEventCount(children.expandedEventCount().toLong() * count, "Repeat")
  }

  is SequenceElement -> children.expandedEventCount()

  is ParallelElement -> children.expandedEventCount()

  else -> checkedEventCount(collectEvents(0L).size.toLong(), "custom element")
}

private fun List<HapticElement>.expandedEventCount(): Int = fold(0) { count, child ->
  checkedEventCount(count.toLong() + child.expandedEventCount(), "Sequence/Repeat child")
}
