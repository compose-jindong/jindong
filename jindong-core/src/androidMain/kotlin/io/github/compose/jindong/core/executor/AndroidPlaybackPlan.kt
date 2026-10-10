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

import io.github.compose.jindong.core.model.HapticControlPoint
import io.github.compose.jindong.core.model.HapticCurve
import io.github.compose.jindong.core.model.HapticEventType
import io.github.compose.jindong.core.model.HapticPattern
import io.github.compose.jindong.core.model.ScheduledHapticEvent
import io.github.compose.jindong.core.model.checkedTimeAdd
import kotlin.math.abs

internal data class AndroidEnvelopeLimits(
  val maxControlPoints: Int,
  val minSegmentMs: Long,
  val maxSegmentMs: Long,
  val maxDurationMs: Long,
)

internal enum class AndroidPrimitive(val id: Int) {
  CLICK(1),
  TICK(7),
  LOW_TICK(8),
}

internal data class AndroidPlaybackCapabilities(
  val apiLevel: Int,
  val device: HapticDeviceCapabilities,
  val supportsEnvelopeEffects: Boolean = false,
  val envelopeLimits: AndroidEnvelopeLimits? = null,
  val primitiveDurationsMs: Map<AndroidPrimitive, Int> = emptyMap(),
)

internal data class AndroidEnvelopePoint(val timeMs: Long, val intensity: Float, val sharpness: Float)
internal data class AndroidPrimitiveStep(val primitive: AndroidPrimitive, val scale: Float, val delayMs: Int)

// SDK 36 parcels 512 StepSegments in about 47 KiB, leaving room below Binder's suggested 64 KiB.
internal const val MAX_ANDROID_WAVEFORM_SEGMENTS = 512

internal data class AndroidPlaybackPlan(
  val diagnostics: HapticPlaybackDiagnostics,
  val envelope: List<AndroidEnvelopePoint> = emptyList(),
  val primitives: List<AndroidPrimitiveStep> = emptyList(),
  val waveform: AndroidWaveform? = null,
)

internal data class AndroidWaveform(val timings: LongArray, val amplitudes: IntArray) {
  val durationMs: Long = timings.fold(0L) { total, timing -> checkedTimeAdd(total, timing, "native waveform duration") }

  override fun equals(other: Any?): Boolean = other is AndroidWaveform &&
    timings.contentEquals(other.timings) && amplitudes.contentEquals(other.amplitudes)

  override fun hashCode(): Int = 31 * timings.contentHashCode() + amplitudes.contentHashCode()
}

/** Selects one native effect for the whole timeline, with no platform calls or coroutine timers. */
internal fun planAndroidPlayback(pattern: HapticPattern, capabilities: AndroidPlaybackCapabilities): AndroidPlaybackPlan {
  try {
    return buildAndroidPlaybackPlan(pattern, capabilities)
  } catch (limit: HapticPlanningLimitException) {
    return AndroidPlaybackPlan(
      HapticPlaybackDiagnostics(
        HapticPlaybackBackend.UNSUPPORTED,
        capabilities.device,
        pattern.durationMs,
        0L,
        unsupportedReason = limit.message,
      ),
    )
  }
}

private fun buildAndroidPlaybackPlan(pattern: HapticPattern, capabilities: AndroidPlaybackCapabilities): AndroidPlaybackPlan {
  val audible = pattern.events.filter { it.isAudible() }
  fun diagnostics(backend: HapticPlaybackBackend, nativeDuration: Long = 0L, approximations: List<String> = emptyList(), reason: String? = null) = HapticPlaybackDiagnostics(backend, capabilities.device, pattern.durationMs, nativeDuration, approximations, reason)

  if (audible.isEmpty()) return AndroidPlaybackPlan(diagnostics(HapticPlaybackBackend.SILENT))
  if (!capabilities.device.supportsHaptics) {
    return AndroidPlaybackPlan(diagnostics(HapticPlaybackBackend.UNSUPPORTED, reason = "Device has no vibrator"))
  }

  val approximations = mutableListOf<String>()
  val hasTransients = audible.any { it.eventType == HapticEventType.TRANSIENT }
  val hasCurves = pattern.events.any { it.intensityCurve != null || it.sharpnessCurve != null }
  if (hasOverlap(audible)) {
    approximations += "Overlaps use maximum intensity; the winning event supplies sharpness."
    if (hasCurves) approximations += "Overlapping curve crossings use a millisecond grid (at most 1 ms timing error)."
  }

  if (capabilities.apiLevel >= 36 && capabilities.supportsEnvelopeEffects) {
    val envelope = try {
      envelopePoints(pattern, capabilities.envelopeLimits, approximations)
    } catch (limit: HapticPlanningLimitException) {
      approximations += "Envelope candidate rejected: ${limit.message}. Trying another native path."
      null
    }
    if (envelope != null) {
      if (hasTransients) {
        approximations += "Transients use triangular envelope pulses; peaks occur ${capabilities.envelopeLimits!!.minSegmentMs} ms after requested onsets."
      }
      return AndroidPlaybackPlan(diagnostics(HapticPlaybackBackend.ANDROID_ENVELOPE, envelope.last().timeMs, approximations), envelope = envelope)
    }
    approximations += "Envelope start/end, device point/time, or planning limits require fallback; no logical time was added."
  }

  if (capabilities.apiLevel >= 31 && audible.all { it.eventType == HapticEventType.TRANSIENT }) {
    val primitives = primitiveSteps(audible, capabilities.primitiveDurationsMs)
    if (primitives != null) {
      val last = audible.maxBy { it.startTimeMs }
      val duration = checkedTimeAdd(last.startTimeMs, capabilities.primitiveDurationsMs.getValue(primitiveFor(last.sharpness)).toLong(), "native primitive duration")
      approximations += "Sharpness is approximated by LOW_TICK below 1/3, CLICK below 2/3, and TICK otherwise; these are categorical textures."
      if (duration > pattern.durationMs) approximations += "Native primitive tails extend playback by ${duration - pattern.durationMs} ms beyond logical duration."
      return AndroidPlaybackPlan(diagnostics(HapticPlaybackBackend.ANDROID_PRIMITIVES, duration, approximations), primitives = primitives)
    }
    approximations += "Primitive support, duration, or spacing constraints require waveform fallback; requested onsets are preserved."
  }

  val pulseEvents = waveformEvents(pattern.events)
  if (hasTransients && hasOverlap(pulseEvents.filter { it.isAudible() }) && !hasOverlap(audible)) {
    approximations += "Transient waveform pulse widths overlap other events; maximum intensity composes their output."
  }
  val serial = mergeToSerial(pulseEvents)
  val segments = if (capabilities.device.supportsAmplitudeControl && !hasCurves) insertFallRamps(serial) else serial
  if (segments != serial) approximations += "Legacy fall-ramp compatibility borrows up to 8 ms from following gaps."
  if (hasTransients) approximations += "Transients use waveform pulses of at most 10 ms, shortened at the next transient onset."
  approximations += "Waveform playback drops sharpness."
  if (!capabilities.device.supportsAmplitudeControl) approximations += "No amplitude control: every positive intensity is approximated as ON at full amplitude."
  val waveform = sampledWaveform(segments, capabilities.device.supportsAmplitudeControl, approximations)
  if (waveform == null) return AndroidPlaybackPlan(diagnostics(HapticPlaybackBackend.SILENT))
  val timings = waveform.timings.toMutableList()
  val amplitudes = waveform.amplitudes.toMutableList()
  // Keep the existing single-step recognition primer, but never re-energize a curve's zero ending.
  val primer = pattern.events.size == 1 && audible.single().intensityCurve == null && amplitudes.last() > 0
  if (primer) {
    timings += listOf(1L, 1L)
    amplitudes += listOf(0, if (capabilities.device.supportsAmplitudeControl) 1 else 255)
  }
  timings += 1L
  amplitudes += 0
  if (timings.size > MAX_ANDROID_WAVEFORM_SEGMENTS) {
    throw HapticPlanningLimitException("Android waveform exceeds $MAX_ANDROID_WAVEFORM_SEGMENTS Binder transport segments")
  }
  val compatible = AndroidWaveform(timings.toLongArray(), amplitudes.toIntArray())
  approximations += "Device compatibility adds ${if (primer) 3 else 1} ms of native playback, including a trailing OFF segment${if (primer) " and recognition primer" else ""}."
  if (compatible.durationMs > pattern.durationMs) approximations += "Native waveform ends ${compatible.durationMs - pattern.durationMs} ms after logical duration."
  return AndroidPlaybackPlan(diagnostics(HapticPlaybackBackend.ANDROID_WAVEFORM, compatible.durationMs, approximations), waveform = compatible)
}

private fun ScheduledHapticEvent.isAudible(): Boolean = when (eventType) {
  HapticEventType.TRANSIENT -> intensity.value > 0f
  HapticEventType.CONTINUOUS -> durationMs > 0L && (intensityCurve?.points?.any { it.value > 0f } ?: (intensity.value > 0f))
}

private fun hasOverlap(events: List<ScheduledHapticEvent>): Boolean {
  var latestEnd = -1L
  var latestStart = -1L
  for (event in events.sortedBy { it.startTimeMs }) {
    if (event.startTimeMs < latestEnd || event.startTimeMs == latestStart) return true
    latestStart = event.startTimeMs
    latestEnd = maxOf(latestEnd, event.startTimeMs + event.durationMs)
  }
  return false
}

private fun envelopePoints(pattern: HapticPattern, limits: AndroidEnvelopeLimits?, approximations: MutableList<String>): List<AndroidEnvelopePoint>? {
  if (limits == null || limits.maxControlPoints <= 0 || limits.minSegmentMs <= 0L || limits.maxSegmentMs < limits.minSegmentMs || limits.maxDurationMs <= 0L) return null
  if (limits.minSegmentMs > Long.MAX_VALUE / 2L) return null
  val pulseWidth = limits.minSegmentMs * 2L
  val events = pattern.events.map { event ->
    if (event.eventType != HapticEventType.TRANSIENT || event.intensity.value == 0f) return@map event
    if (pattern.durationMs - event.startTimeMs < pulseWidth) return null
    event.copy(
      durationMs = pulseWidth,
      eventType = HapticEventType.CONTINUOUS,
      intensityCurve = HapticCurve(
        listOf(HapticControlPoint(0L, 0f), HapticControlPoint(limits.minSegmentMs, event.intensity.value), HapticControlPoint(pulseWidth, 0f)),
      ),
    )
  }
  val serial = mergeToSerial(events)
  if (serial.isEmpty() || serial.first().intensity != 0f || serial.last().endIntensity != 0f) return null
  val nextSharpness = FloatArray(serial.size)
  var following = serial.last().endSharpness
  for (index in serial.indices.reversed()) {
    val segment = serial[index]
    if (segment.intensity > 0f || segment.endIntensity > 0f) following = segment.sharpness
    nextSharpness[index] = following
  }
  val points = mutableListOf<AndroidEnvelopePoint>()
  var previousSharpness = nextSharpness.first()
  for ((index, segment) in serial.withIndex()) {
    val silent = segment.intensity == 0f && segment.endIntensity == 0f
    val startSharpness = if (silent) previousSharpness else segment.sharpness
    val endSharpness = if (silent) nextSharpness[index] else segment.endSharpness
    val start = AndroidEnvelopePoint(segment.startTimeMs, segment.intensity, startSharpness)
    if (points.isEmpty()) {
      points += start
    } else if (points.last() != start) {
      return null
    }
    points += AndroidEnvelopePoint(segment.startTimeMs + segment.durationMs, segment.endIntensity, endSharpness)
    previousSharpness = endSharpness
    while (points.size >= 3 && collinear(points[points.size - 3], points[points.size - 2], points.last())) points.removeAt(points.size - 2)
  }
  if (points.last().timeMs > limits.maxDurationMs) return null
  val bounded = mutableListOf(points.first())
  for ((start, end) in points.zipWithNext()) {
    val duration = end.timeMs - start.timeMs
    val count = (duration - 1L) / limits.maxSegmentMs + 1L
    if (count > limits.maxControlPoints - bounded.size + 1L || duration / count < limits.minSegmentMs) return null
    for (index in 1..count.toInt()) {
      val offset = duration / count * index + (duration % count * index) / count
      val fraction = offset.toDouble() / duration
      bounded += AndroidEnvelopePoint(start.timeMs + offset, interpolate(start.intensity, end.intensity, fraction), interpolate(start.sharpness, end.sharpness, fraction))
    }
  }
  if (hasOverlap(events.filter { it.isAudible() }) && !hasOverlap(pattern.events.filter { it.isAudible() })) {
    approximations += "Transient envelope pulse widths overlap other events; maximum intensity composes their output."
    approximations += "Pulse curve crossings use a millisecond grid (at most 1 ms timing error)."
  }
  return bounded
}

private fun collinear(first: AndroidEnvelopePoint, middle: AndroidEnvelopePoint, last: AndroidEnvelopePoint): Boolean {
  val left = (middle.timeMs - first.timeMs).toDouble()
  val whole = (last.timeMs - first.timeMs).toDouble()
  return (middle.intensity.toDouble() - first.intensity) * whole == (last.intensity.toDouble() - first.intensity) * left &&
    (middle.sharpness.toDouble() - first.sharpness) * whole == (last.sharpness.toDouble() - first.sharpness) * left
}

internal fun primitiveFor(sharpness: Float): AndroidPrimitive = when {
  sharpness < 1f / 3f -> AndroidPrimitive.LOW_TICK
  sharpness < 2f / 3f -> AndroidPrimitive.CLICK
  else -> AndroidPrimitive.TICK
}

private fun primitiveSteps(events: List<ScheduledHapticEvent>, durations: Map<AndroidPrimitive, Int>): List<AndroidPrimitiveStep>? {
  val result = mutableListOf<AndroidPrimitiveStep>()
  var previousEnd = 0L
  for (event in events.sortedBy { it.startTimeMs }) {
    val primitive = primitiveFor(event.sharpness)
    val duration = durations[primitive]?.takeIf { it > 0 } ?: return null
    val delay = event.startTimeMs - previousEnd
    if (delay < 0L || delay > Int.MAX_VALUE) return null
    result += AndroidPrimitiveStep(primitive, event.intensity.value, delay.toInt())
    previousEnd = checkedTimeAdd(event.startTimeMs, duration.toLong(), "native primitive end")
  }
  return result
}

private fun waveformEvents(events: List<ScheduledHapticEvent>): List<ScheduledHapticEvent> {
  val starts = events.filter { it.eventType == HapticEventType.TRANSIENT && it.intensity.value > 0f }.map { it.startTimeMs }.distinct().sorted()
  val widths = starts.mapIndexed { index, start -> start to minOf(10L, starts.getOrNull(index + 1)?.minus(start) ?: 10L) }.toMap()
  return events.map { event ->
    if (event.eventType == HapticEventType.TRANSIENT && event.intensity.value > 0f) {
      event.copy(durationMs = widths.getValue(event.startTimeMs), eventType = HapticEventType.CONTINUOUS)
    } else {
      event
    }
  }
}

private fun sampledWaveform(segments: List<HapticSegment>, amplitudeControl: Boolean, approximations: MutableList<String>): AndroidWaveform? {
  require(segments.size <= 200_000) { "Android waveform exceeds 200000 timeline segments" }
  val timings = mutableListOf<Long>()
  val amplitudes = mutableListOf<Int>()
  var sampleCount = 0
  var maxError = 0f
  var largestSampleMs = 0L
  for ((index, segment) in segments.withIndex()) {
    val varying = segment.intensity != segment.endIntensity
    val desiredCount = if (varying) (segment.durationMs - 1L) / 8L + 1L else 1L
    val count = minOf(desiredCount, (200_000 - sampleCount - (segments.size - index - 1)).toLong()).toInt()
    require(count > 0) { "Android waveform exceeds 200000 samples" }
    sampleCount += count
    for (sample in 0 until count) {
      val start = segment.durationMs / count * sample + (segment.durationMs % count * sample) / count
      val end = segment.durationMs / count * (sample + 1L) + (segment.durationMs % count * (sample + 1L)) / count
      val from = interpolate(segment.intensity, segment.endIntensity, start.toDouble() / segment.durationMs)
      val to = interpolate(segment.intensity, segment.endIntensity, end.toDouble() / segment.durationMs)
      val intensity = maxOf(from, to)
      val amplitude = when {
        intensity == 0f -> 0
        !amplitudeControl -> 255
        else -> (intensity * 255).toInt().coerceIn(1, 255)
      }
      // Android stores each waveform duration in an Int, even though createWaveform takes Longs.
      if (amplitudes.lastOrNull() == amplitude && timings.last() <= Int.MAX_VALUE.toLong() - (end - start)) {
        timings[timings.lastIndex] += end - start
      } else {
        timings += end - start
        amplitudes += amplitude
      }
      if (varying) {
        maxError = maxOf(maxError, abs(to - from))
        largestSampleMs = maxOf(largestSampleMs, end - start)
      }
    }
  }
  if (amplitudes.none { it > 0 }) return null
  if (largestSampleMs > 0L) approximations += "Intensity curves use peak-preserving sample-and-hold; boundaries and zero intervals are retained (largest slice $largestSampleMs ms, intensity error <= $maxError plus 1/255 quantization)."
  return AndroidWaveform(timings.toLongArray(), amplitudes.toIntArray())
}

private fun interpolate(start: Float, end: Float, fraction: Double): Float = (start + (end.toDouble() - start) * fraction).toFloat()
