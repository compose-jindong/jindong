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
@file:Suppress("ktlint:standard:filename")

package io.github.compose.jindong.core.executor

import io.github.compose.jindong.core.model.ScheduledHapticEvent

/**
 * A single non-overlapping slice of a serialized haptic timeline.
 *
 * @property startTimeMs Absolute start of this segment.
 * @property durationMs Length of this segment, always greater than 0.
 * @property intensity Winning event's intensity (NOT a sum of overlapping events); 0f when a gap.
 * @property sharpness Carried from the same winning event (iOS Core Haptics parameter).
 * @property isGap True when no event is active here. Distinct from an active event whose intensity
 *   happens to be 0f (e.g. `Custom(0.0)`), which must still floor to a non-zero amplitude.
 */
internal data class HapticSegment(
  val startTimeMs: Long,
  val durationMs: Long,
  val intensity: Float,
  val sharpness: Float,
  val isGap: Boolean = false,
)

private const val DEFAULT_SHARPNESS = 0.5f

// Fall-ramp parameters. On LRA actuators a hard amplitude drop to 0 rings out for 50ms+
// because createWaveform renders pure steps (no interpolation); a short stepped fall ramp
// borrowed from the front of the following gap masks that ringing.
private const val FALL_RAMP_MS = 16L
private const val FALL_RAMP_STEPS = 2
private const val MIN_RAMP_MS = 4L

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

/**
 * Softens every `active -> gap` transition with a short stepped fall ramp, masking the LRA
 * ringing a hard amplitude drop to 0 would otherwise cause.
 *
 * The ramp is borrowed from the FRONT of the following gap (the active segment is never touched),
 * so the total duration and every active segment's timing are preserved. Each ramp step is marked
 * `isGap = false` so quantization floors its non-zero intensity to a real amplitude; only the
 * remaining true gap stays at 0. Transitions other than active->gap (active->active, gap->active,
 * leading gap) are left untouched.
 *
 * Invariant: `insertFallRamps(s).sumOf { it.durationMs } == s.sumOf { it.durationMs }`.
 */
internal fun insertFallRamps(segments: List<HapticSegment>): List<HapticSegment> {
  if (segments.size < 2) return segments

  val result = mutableListOf<HapticSegment>()
  var i = 0
  while (i < segments.size) {
    val current = segments[i]
    val next = segments.getOrNull(i + 1)

    val isActiveToGap = !current.isGap && next != null && next.isGap && next.durationMs > MIN_RAMP_MS
    if (next == null || !isActiveToGap) {
      result += current
      i++
      continue
    }

    result += current
    result += rampThenGap(fromIntensity = current.intensity, gap = next)
    i += 2 // current and the gap are both consumed here
  }

  return result
}

/**
 * Replaces [gap] with `[ramp steps..., shrunken gap]`, stepping the amplitude down from
 * [fromIntensity] to 0 across the front [FALL_RAMP_MS] (or the whole gap if shorter).
 * Duration is conserved: the borrowed window plus the leftover gap equal `gap.durationMs`.
 */
private fun rampThenGap(fromIntensity: Float, gap: HapticSegment): List<HapticSegment> {
  val effectiveRampMs = minOf(FALL_RAMP_MS, gap.durationMs)
  val stepMs = effectiveRampMs / FALL_RAMP_STEPS
  if (stepMs <= 0L) return listOf(gap) // not enough room to split; leave the gap intact

  val out = mutableListOf<HapticSegment>()
  var cursor = gap.startTimeMs
  // Steps 1..(STEPS-1) carry a decaying non-zero amplitude; the final step folds into the gap.
  for (step in 1 until FALL_RAMP_STEPS) {
    val stepIntensity = fromIntensity * (FALL_RAMP_STEPS - step) / FALL_RAMP_STEPS
    out += HapticSegment(
      startTimeMs = cursor,
      durationMs = stepMs,
      intensity = stepIntensity,
      sharpness = gap.sharpness,
      isGap = false,
    )
    cursor += stepMs
  }

  // Remaining gap absorbs both the last ramp slot and any integer-division remainder, conserving sum.
  out += gap.copy(startTimeMs = cursor, durationMs = gap.durationMs - (cursor - gap.startTimeMs))
  return out
}
