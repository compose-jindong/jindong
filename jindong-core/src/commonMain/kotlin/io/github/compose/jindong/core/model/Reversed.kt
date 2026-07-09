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
 * Returns a new pattern with events mirrored across the pattern span.
 *
 * Each event is reflected on `[0, span]`: `newStart = span - (start + duration)`, keeping its
 * duration. Intensity and iOS parameters are unchanged. Interior gaps make the mirror overlap
 * neighbors; overlaps are left as-is and resolved at playback by the serial merge (highest intensity
 * wins), so this stays a pure timeline reflection. Compiled patterns are anchored at 0, so reflecting
 * twice recovers the original; a leading gap (earliest event not at 0) would fold events off the
 * front and is not a shape the builder produces.
 */
public fun HapticPattern.reversed(): HapticPattern {
  val span = spanMs()
  return copy(
    events = events.map { event ->
      event.copy(startTimeMs = span - (event.startTimeMs + event.durationMs))
    },
  )
}
