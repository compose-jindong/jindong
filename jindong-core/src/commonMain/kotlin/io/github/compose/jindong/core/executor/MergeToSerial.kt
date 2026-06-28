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
package io.github.compose.jindong.core.executor

import io.github.compose.jindong.core.model.ScheduledHapticEvent

private const val DEFAULT_SHARPNESS = 0.5f

/**
 * Flattens potentially overlapping [events] into a gap-filled serial timeline.
 *
 * Boundaries are every event start and end, so each sub-interval is either fully covered by an
 * event or empty. Overlaps resolve to the highest-intensity event (ties keep input order for
 * determinism) instead of summing, matching how a single vibrator motor can only render one
 * amplitude at a time. Gaps (including a leading gap before the first event) become 0f segments.
 *
 * Invariant: the sum of output [HapticSegment.durationMs] equals `events.maxOf { start + dur }`.
 */
internal fun mergeToSerial(events: List<ScheduledHapticEvent>): List<HapticSegment> {
  if (events.isEmpty()) return emptyList()

  val boundaries = buildList {
    add(0L)
    for (event in events) {
      add(event.startTimeMs)
      add(event.startTimeMs + event.durationMs)
    }
  }.distinct().sorted()

  val segments = mutableListOf<HapticSegment>()

  for (i in 0 until boundaries.size - 1) {
    val start = boundaries[i]
    val end = boundaries[i + 1]
    if (start == end) continue // drop zero-length slices

    // Boundaries cover every endpoint, so an active event fully spans [start, end).
    val winner = events
      .filter { it.startTimeMs <= start && it.startTimeMs + it.durationMs >= end }
      .maxByOrNull { it.intensity.value }

    segments += HapticSegment(
      startTimeMs = start,
      durationMs = end - start,
      intensity = winner?.intensity?.value ?: 0f,
      sharpness = winner?.iosParameters?.sharpness ?: DEFAULT_SHARPNESS,
      isGap = winner == null,
    )
  }

  return segments
}
