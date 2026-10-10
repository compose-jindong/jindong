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
package io.github.compose.jindong.core.model

/** Appends [other] after this pattern's complete timeline, including silence. */
public infix fun HapticPattern.then(other: HapticPattern): HapticPattern {
  val duration = checkedTimeAdd(durationMs, other.durationMs, "then")
  checkedEventCount(events.size.toLong() + other.events.size, "then")
  return HapticPattern(
    events + other.events.map { event ->
      event.copy(startTimeMs = checkedTimeAdd(durationMs, event.startTimeMs, "then event"))
    },
    duration,
  )
}

/** Operator alias for [then]. */
public operator fun HapticPattern.plus(other: HapticPattern): HapticPattern = this then other
