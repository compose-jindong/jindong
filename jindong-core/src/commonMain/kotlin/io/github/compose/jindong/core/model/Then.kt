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

/**
 * Returns a new pattern that plays this pattern, then [other] appended after it.
 *
 * Every event of [other] is shifted by this pattern's span, so the two timelines never overlap and
 * `span(a then b) == span(a) + span(b)`. Intensity and iOS parameters are carried unchanged. The
 * empty pattern is the identity for [then].
 */
public infix fun HapticPattern.then(other: HapticPattern): HapticPattern {
  val offset = spanMs()
  return copy(
    events = events + other.events.map { event ->
      event.copy(startTimeMs = event.startTimeMs + offset)
    },
  )
}

/**
 * Operator alias for [then], so patterns can be concatenated with `a + b`.
 */
public operator fun HapticPattern.plus(other: HapticPattern): HapticPattern = this then other
