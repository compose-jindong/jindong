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
import io.github.compose.jindong.core.model.checkedTimeAdd
import kotlin.math.ceil
import kotlin.math.floor

private const val MAX_MIX_SEGMENTS = 100_000
private const val MAX_MIX_ACTIVE_VISITS = 1_000_000L

internal class HapticPlanningLimitException(message: String) : IllegalArgumentException(message)

/**
 * Mixes overlapping continuous events by maximum intensity, carrying the winner's sharpness.
 * Input order breaks ties. Curve points and intensity crossings split the serial track so a winner
 * cannot mask a stronger event later in the same interval. Fractional crossings use a 1ms grid;
 * native planners report this approximation. Transients must first become backend-specific pulses.
 */
internal fun mergeToSerial(events: List<ScheduledHapticEvent>): List<HapticSegment> {
  val continuous = events.filter { it.durationMs > 0L }
  if (continuous.isEmpty()) return emptyList()

  val boundaries = buildSet {
    add(0L)
    for (event in continuous) {
      add(event.startTimeMs)
      add(checkedTimeAdd(event.startTimeMs, event.durationMs, "mix event end"))
      for (curve in listOfNotNull(event.intensityCurve, event.sharpnessCurve)) {
        curve.points.forEach { add(checkedTimeAdd(event.startTimeMs, it.timeMs, "mix curve point")) }
      }
    }
  }.sorted()

  if (boundaries.size > MAX_MIX_SEGMENTS + 1) throw HapticPlanningLimitException("Mixing exceeds $MAX_MIX_SEGMENTS serial intervals")
  val byStart = continuous.withIndex().sortedWith(compareBy<IndexedValue<ScheduledHapticEvent>> { it.value.startTimeMs }.thenBy { it.index })
  val active = mutableListOf<IndexedValue<ScheduledHapticEvent>>()
  var cursor = 0
  var visits = 0L
  return buildList {
    for (i in 0 until boundaries.lastIndex) {
      val start = boundaries[i]
      val end = boundaries[i + 1]
      visits += active.size
      active.removeAll { it.value.startTimeMs + it.value.durationMs <= start }
      while (cursor < byStart.size && byStart[cursor].value.startTimeMs <= start) active += byStart[cursor++]
      visits += active.size
      if (visits > MAX_MIX_ACTIVE_VISITS) throw HapticPlanningLimitException("Mixing exceeds $MAX_MIX_ACTIVE_VISITS active-event visits")
      if (active.isEmpty()) {
        add(HapticSegment(start, end - start, 0f, 0.5f, isGap = true))
        continue
      }
      val lines = active.map { (index, event) ->
        val initial = event.intensityAt(start).toDouble()
        MixingLine(event, index, initial, event.intensityAt(end).toDouble() - initial)
      }
      val hull = upperEnvelope(lines)
      val splits = crossingBoundaries(hull, start, end)
      var hullCursor = 0
      for (split in 0 until splits.lastIndex) {
        val from = splits[split]
        val to = splits[split + 1]
        val midpoint = ((from - start).toDouble() + (to - from).toDouble() / 2.0) / (end - start).toDouble()
        var winner = hull[hullCursor].line
        var intensity = winner.initial + winner.delta * midpoint
        while (hullCursor < hull.lastIndex && hull[hullCursor + 1].startsAt <= midpoint) {
          hullCursor++
          val candidate = hull[hullCursor].line
          val candidateIntensity = candidate.initial + candidate.delta * midpoint
          if (candidateIntensity > intensity || (candidateIntensity == intensity && candidate.index < winner.index)) {
            winner = candidate
            intensity = candidateIntensity
          }
        }
        val event = winner.event
        if (size >= MAX_MIX_SEGMENTS) throw HapticPlanningLimitException("Mixing exceeds $MAX_MIX_SEGMENTS serial intervals")
        add(
          HapticSegment(
            startTimeMs = from,
            durationMs = to - from,
            intensity = event.intensityAt(from),
            sharpness = event.sharpnessAt(from),
            endIntensity = event.intensityAt(to),
            endSharpness = event.sharpnessAt(to),
          ),
        )
      }
    }
  }
}

private fun ScheduledHapticEvent.intensityAt(timeMs: Long): Float = intensityCurve?.valueAt(timeMs - startTimeMs) ?: intensity.value

private fun ScheduledHapticEvent.sharpnessAt(timeMs: Long): Float = sharpnessCurve?.valueAt(timeMs - startTimeMs) ?: sharpness

private data class MixingLine(val event: ScheduledHapticEvent, val index: Int, val initial: Double, val delta: Double)

private data class WinningLine(val line: MixingLine, val startsAt: Double)

/** The upper envelope finds relevant crossings without comparing every pair of overlapping events. */
private fun upperEnvelope(lines: List<MixingLine>): List<WinningLine> {
  if (lines.size == 1 || lines.all { it.delta == 0.0 }) {
    val winner = lines.maxWith(compareBy<MixingLine> { it.initial + it.delta * 0.5 }.thenBy { -it.index })
    return listOf(WinningLine(winner, Double.NEGATIVE_INFINITY))
  }
  val hull = mutableListOf<WinningLine>()
  val sorted = lines.sortedWith(compareBy<MixingLine> { it.delta }.thenByDescending { it.initial }.thenBy { it.index })
    .distinctBy { it.delta }
  for (line in sorted) {
    var crossing = Double.NEGATIVE_INFINITY
    while (hull.isNotEmpty()) {
      val previous = hull.last()
      crossing = (previous.line.initial - line.initial) / (line.delta - previous.line.delta)
      // Keep point-only winners so an exact midpoint tie still follows input order.
      if (crossing >= previous.startsAt) break
      hull.removeAt(hull.lastIndex)
    }
    hull += WinningLine(line, if (hull.isEmpty()) Double.NEGATIVE_INFINITY else crossing)
  }
  return hull
}

private fun crossingBoundaries(hull: List<WinningLine>, start: Long, end: Long): List<Long> {
  val width = end - start
  return buildSet {
    add(start)
    add(end)
    for (entry in hull) {
      if (entry.startsAt > 0.0 && entry.startsAt < 1.0) {
        val offset = entry.startsAt * width.toDouble()
        add(checkedTimeAdd(start, floor(offset).toLong().coerceIn(0L, width), "mix crossing"))
        add(checkedTimeAdd(start, ceil(offset).toLong().coerceIn(0L, width), "mix crossing"))
      }
    }
  }.sorted()
}
