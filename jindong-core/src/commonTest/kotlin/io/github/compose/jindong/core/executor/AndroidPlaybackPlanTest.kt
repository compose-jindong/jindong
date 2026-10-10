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
import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.model.HapticPattern
import io.github.compose.jindong.core.model.ScheduledHapticEvent
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private fun curve(vararg points: Pair<Long, Float>): HapticCurve = HapticCurve(points.map { HapticControlPoint(it.first, it.second) })

private fun continuous(start: Long = 0L, intensity: Float = 0.75f, intensityCurve: HapticCurve = curve(0L to 0f, 80L to 0.8f, 300L to 0f)): ScheduledHapticEvent = ScheduledHapticEvent(
  start,
  intensityCurve.durationMs,
  HapticIntensity.Custom(intensity),
  eventType = HapticEventType.CONTINUOUS,
  sharpness = 0.6f,
  intensityCurve = intensityCurve,
)

private fun transient(start: Long = 0L, intensity: Float = 0.75f, sharpness: Float = 0.9f): ScheduledHapticEvent = ScheduledHapticEvent(
  start,
  0L,
  HapticIntensity.Custom(intensity),
  eventType = HapticEventType.TRANSIENT,
  sharpness = sharpness,
)

private fun capabilities(
  api: Int = 36,
  envelope: Boolean = true,
  limits: AndroidEnvelopeLimits = AndroidEnvelopeLimits(16, 20L, 1000L, 16_000L),
  amplitude: Boolean = true,
  primitives: Map<AndroidPrimitive, Int> = emptyMap(),
): AndroidPlaybackCapabilities = AndroidPlaybackCapabilities(
  api,
  HapticDeviceCapabilities(true, amplitude, envelope),
  limits,
  primitives,
)

private fun AndroidWaveform.amplitudeAt(timeMs: Long): Int {
  var cursor = 0L
  for (index in timings.indices) {
    cursor += timings[index]
    if (timeMs < cursor) return amplitudes[index]
  }
  return 0
}

class AndroidPlaybackPlanTest :
  FunSpec({
    test("envelope merges intensity and sharpness point times and uses normalized values") {
      val event = continuous().copy(
        eventType = HapticEventType.CONTINUOUS,
        sharpnessCurve = curve(0L to 0.2f, 100L to 0.7f, 300L to 0.8f),
      )
      val plan = planAndroidPlayback(HapticPattern(listOf(event)), capabilities())
      plan.diagnostics.backend shouldBe HapticPlaybackBackend.ANDROID_ENVELOPE
      plan.envelope.map { it.timeMs } shouldBe listOf(0L, 80L, 100L, 300L)
      plan.envelope.first().sharpness shouldBe 0.2f
      plan.envelope[1].intensity shouldBe 0.8f
      plan.envelope.last().intensity shouldBe 0f
      plan.envelope.last().sharpness shouldBe 0.8f
      plan.diagnostics.estimatedNativeDurationMs shouldBe 300L
      planAndroidPlayback(HapticPattern(listOf(event)), capabilities()) shouldBe plan
    }

    test("API and actual support both gate envelope playback") {
      val pattern = HapticPattern(listOf(continuous()))
      planAndroidPlayback(pattern, capabilities(api = 35)).diagnostics.backend shouldBe HapticPlaybackBackend.ANDROID_WAVEFORM
      planAndroidPlayback(pattern, capabilities(envelope = false)).diagnostics.backend shouldBe HapticPlaybackBackend.ANDROID_WAVEFORM
      planAndroidPlayback(pattern, capabilities().copy(envelopeLimits = null)).diagnostics.backend shouldBe HapticPlaybackBackend.ANDROID_WAVEFORM
    }

    test("actual minimum segment point count and total duration limits cause whole pattern fallback") {
      val pattern = HapticPattern(listOf(continuous()))
      listOf(
        AndroidEnvelopeLimits(16, 100L, 1000L, 16_000L),
        AndroidEnvelopeLimits(1, 20L, 1000L, 16_000L),
        AndroidEnvelopeLimits(16, 20L, 1000L, 299L),
        AndroidEnvelopeLimits(16, 0L, 1000L, 16_000L),
      ).forEach {
        val plan = planAndroidPlayback(pattern, capabilities(limits = it))
        plan.diagnostics.backend shouldBe HapticPlaybackBackend.ANDROID_WAVEFORM
        plan.diagnostics.logicalDurationMs shouldBe 300L
        plan.diagnostics.approximations.any { message -> "Envelope" in message } shouldBe true
      }
    }

    test("long linear segments split within actual maximum without changing the timeline") {
      val plan = planAndroidPlayback(HapticPattern(listOf(continuous())), capabilities(limits = AndroidEnvelopeLimits(16, 20L, 40L, 640L)))
      plan.diagnostics.backend shouldBe HapticPlaybackBackend.ANDROID_ENVELOPE
      plan.envelope.zipWithNext().all { (start, end) -> end.timeMs - start.timeMs in 20L..40L } shouldBe true
      plan.envelope.last().timeMs shouldBe 300L
      plan.envelope.size shouldBe 9
    }

    test("collinear points can be removed exactly to fit device point count") {
      val event = continuous(intensityCurve = curve(0L to 0f, 25L to 0.25f, 50L to 0.5f, 100L to 1f, 200L to 0f))
      val plan = planAndroidPlayback(HapticPattern(listOf(event)), capabilities(limits = AndroidEnvelopeLimits(2, 20L, 1000L, 2000L)))
      plan.diagnostics.backend shouldBe HapticPlaybackBackend.ANDROID_ENVELOPE
      plan.envelope.map { it.timeMs } shouldBe listOf(0L, 100L, 200L)
    }

    test("nonzero start or end cannot silently gain an envelope ramp") {
      listOf(curve(0L to 1f, 100L to 0f), curve(0L to 0f, 100L to 1f)).forEach {
        val plan = planAndroidPlayback(HapticPattern(listOf(continuous(intensityCurve = it))), capabilities())
        plan.diagnostics.backend shouldBe HapticPlaybackBackend.ANDROID_WAVEFORM
        plan.diagnostics.logicalDurationMs shouldBe 100L
        plan.diagnostics.estimatedNativeDurationMs shouldBe 101L
      }
    }

    test("all zero curves replace positive constants and produce no compatibility pulse") {
      val pattern = HapticPattern(listOf(continuous(intensity = 1f, intensityCurve = curve(0L to 0f, 100L to 0f)), transient(50L, intensity = 0f)), 200L)
      val plan = planAndroidPlayback(pattern, capabilities())
      plan.diagnostics.backend shouldBe HapticPlaybackBackend.SILENT
      plan.diagnostics.logicalDurationMs shouldBe 200L
      plan.diagnostics.estimatedNativeDurationMs shouldBe 0L
      plan.waveform shouldBe null
      plan.envelope shouldBe emptyList()
    }

    test("sparse mixed transient and curved continuous pattern uses envelope with diagnosed pulse timing") {
      val pattern = HapticPattern(listOf(transient(), transient(80L), transient(160L), continuous(240L)))
      val plan = planAndroidPlayback(pattern, capabilities())
      plan.diagnostics.backend shouldBe HapticPlaybackBackend.ANDROID_ENVELOPE
      plan.envelope.filter { it.intensity == 0.75f }.map { it.timeMs } shouldBe listOf(20L, 100L, 180L)
      plan.envelope.last().timeMs shouldBe 540L
      plan.diagnostics.approximations.any { "20 ms after" in it } shouldBe true
    }

    test("mixed input falls back as one waveform when envelope cannot represent it") {
      val pattern = HapticPattern(listOf(transient(), ScheduledHapticEvent(5L, 100L, HapticIntensity.MEDIUM)))
      val plan = planAndroidPlayback(pattern, capabilities())
      plan.diagnostics.backend shouldBe HapticPlaybackBackend.ANDROID_WAVEFORM
      (plan.waveform!!.amplitudeAt(0L) > 0) shouldBe true
      plan.waveform.amplitudeAt(20L) shouldBe 127
      plan.diagnostics.approximations.any { "Transients" in it } shouldBe true
      plan.diagnostics.approximations.any { "pulse widths overlap" in it } shouldBe true
    }

    test("primitive delays subtract native widths and zero intensity never becomes scale zero") {
      val pattern = HapticPattern(listOf(transient(), transient(40L, intensity = 0f), transient(100L)))
      val plan = planAndroidPlayback(pattern, capabilities(api = 31, envelope = false, primitives = mapOf(AndroidPrimitive.TICK to 12)))
      plan.diagnostics.backend shouldBe HapticPlaybackBackend.ANDROID_PRIMITIVES
      plan.primitives.map { it.delayMs } shouldBe listOf(0, 88)
      plan.primitives.all { it.scale > 0f } shouldBe true
      plan.diagnostics.logicalDurationMs shouldBe 100L
      plan.diagnostics.estimatedNativeDurationMs shouldBe 112L
    }

    test("primitive backend requires API31 all selected primitives and positive reported durations") {
      val pattern = HapticPattern(listOf(transient(sharpness = 0f), transient(100L, sharpness = 1f)))
      listOf(
        capabilities(api = 30, envelope = false, primitives = mapOf(AndroidPrimitive.LOW_TICK to 12, AndroidPrimitive.TICK to 12)),
        capabilities(api = 31, envelope = false, primitives = mapOf(AndroidPrimitive.LOW_TICK to 12)),
        capabilities(api = 31, envelope = false, primitives = mapOf(AndroidPrimitive.LOW_TICK to 12, AndroidPrimitive.TICK to 0)),
      ).forEach { planAndroidPlayback(pattern, it).diagnostics.backend shouldBe HapticPlaybackBackend.ANDROID_WAVEFORM }
    }

    test("dense primitives fall back without moving the second onset") {
      val pattern = HapticPattern(listOf(transient(intensity = 0.25f), transient(5L, intensity = 1f)))
      val plan = planAndroidPlayback(pattern, capabilities(api = 31, envelope = false, primitives = mapOf(AndroidPrimitive.TICK to 12)))
      plan.diagnostics.backend shouldBe HapticPlaybackBackend.ANDROID_WAVEFORM
      plan.waveform!!.amplitudeAt(0L) shouldBe 63
      plan.waveform.amplitudeAt(5L) shouldBe 255
      plan.diagnostics.approximations.any { "requested onsets are preserved" in it } shouldBe true
    }

    test("sharpness maps to categorical native textures with an explicit approximation") {
      listOf(0f, 0.5f, 1f).map(::primitiveFor) shouldBe listOf(AndroidPrimitive.LOW_TICK, AndroidPrimitive.CLICK, AndroidPrimitive.TICK)
      val pattern = HapticPattern(listOf(transient(sharpness = 0f), transient(100L, sharpness = 0.5f), transient(200L, sharpness = 1f)))
      val plan = planAndroidPlayback(pattern, capabilities(api = 31, envelope = false, primitives = AndroidPrimitive.entries.associateWith { 10 }))
      plan.primitives.map { it.primitive } shouldBe listOf(AndroidPrimitive.LOW_TICK, AndroidPrimitive.CLICK, AndroidPrimitive.TICK)
      plan.diagnostics.approximations.any { "categorical textures" in it } shouldBe true
    }

    test("short curves retain peaks boundaries and exact zero intervals") {
      val event = continuous(start = 5L, intensityCurve = curve(0L to 0f, 1L to 1f, 2L to 0f, 4L to 0f))
      val plan = planAndroidPlayback(HapticPattern(listOf(event), 20L), capabilities(envelope = false))
      val waveform = plan.waveform!!
      waveform.amplitudeAt(4L) shouldBe 0
      waveform.amplitudeAt(5L) shouldBe 255
      waveform.amplitudeAt(6L) shouldBe 255
      waveform.amplitudeAt(7L) shouldBe 0
      waveform.amplitudeAt(8L) shouldBe 0
      waveform.amplitudes.last() shouldBe 0
      plan.diagnostics.estimatedNativeDurationMs shouldBe 10L
      plan.diagnostics.approximations.any { "sample-and-hold" in it } shouldBe true
    }

    test("no amplitude control uses explicit ON OFF approximation and reports sharpness loss") {
      val pattern = HapticPattern(listOf(ScheduledHapticEvent(0L, 10L, HapticIntensity.LIGHT), ScheduledHapticEvent(20L, 10L, HapticIntensity.MEDIUM)))
      val plan = planAndroidPlayback(pattern, capabilities(envelope = false, amplitude = false))
      plan.waveform!!.amplitudes.toList() shouldBe listOf(255, 0, 255, 0)
      plan.diagnostics.approximations.any { "ON at full amplitude" in it } shouldBe true
      plan.diagnostics.approximations.any { "drops sharpness" in it } shouldBe true
    }

    test("overlap reports composition and fractional curve crossing timing approximation") {
      val pattern = HapticPattern(listOf(continuous(), continuous(20L)))
      val plan = planAndroidPlayback(pattern, capabilities(envelope = false))
      plan.diagnostics.approximations.any { "maximum intensity" in it } shouldBe true
      plan.diagnostics.approximations.any { "1 ms timing error" in it } shouldBe true
    }

    test("unsupported hardware differs from silence and legacy zero duration stays silent") {
      val noHardware = capabilities().copy(device = HapticDeviceCapabilities(false, false))
      planAndroidPlayback(HapticPattern(listOf(transient())), noHardware).diagnostics.backend shouldBe HapticPlaybackBackend.UNSUPPORTED
      planAndroidPlayback(HapticPattern(listOf(ScheduledHapticEvent(0L, 0L, HapticIntensity.HIGH))), capabilities()).diagnostics.backend shouldBe HapticPlaybackBackend.SILENT
    }

    test("mix planning budget failures become explicit unsupported diagnostics") {
      val longCurve = curve(0L to 0f, 5000L to 1f, 10_000L to 0f)
      val pattern = HapticPattern(List(1100) { continuous(start = it.toLong(), intensityCurve = longCurve) })
      val plan = planAndroidPlayback(pattern, capabilities(envelope = false))
      plan.diagnostics.backend shouldBe HapticPlaybackBackend.UNSUPPORTED
      plan.diagnostics.unsupportedReason!!.contains("active-event visits") shouldBe true
      plan.waveform shouldBe null
    }

    test("an envelope candidate planning limit can fall back to a representable waveform") {
      val events = List(1000) { ScheduledHapticEvent(0L, 16_000L, HapticIntensity.Custom(0.1f)) } +
        List(200) { transient(it * 80L) }
      val plan = planAndroidPlayback(HapticPattern(events), capabilities())
      plan.diagnostics.backend shouldBe HapticPlaybackBackend.ANDROID_WAVEFORM
      plan.diagnostics.approximations.any { "Envelope candidate rejected" in it } shouldBe true
      (plan.waveform!!.amplitudeAt(0L) > 0) shouldBe true
    }

    test("unrepresentable native pulse and compatibility duration fail before native start") {
      shouldThrow<IllegalArgumentException> {
        planAndroidPlayback(HapticPattern(listOf(transient(Long.MAX_VALUE))), capabilities(envelope = false))
      }
      shouldThrow<IllegalArgumentException> {
        planAndroidPlayback(HapticPattern(listOf(ScheduledHapticEvent(0L, Long.MAX_VALUE, HapticIntensity.HIGH))), capabilities(envelope = false))
      }
    }
  })
