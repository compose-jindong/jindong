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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private fun rise(): HapticCurve = HapticCurve(
  listOf(
    HapticControlPoint(0L, 0f),
    HapticControlPoint(80L, 0.8f),
    HapticControlPoint(300L, 0f),
  ),
)

private fun curved(start: Long = 0L): ScheduledHapticEvent = ScheduledHapticEvent(
  startTimeMs = start,
  durationMs = 300L,
  intensity = HapticIntensity.STRONG,
  eventType = HapticEventType.CONTINUOUS,
  sharpness = 0.6f,
  intensityCurve = rise(),
  sharpnessCurve = HapticCurve(listOf(HapticControlPoint(0L, 0.2f), HapticControlPoint(300L, 0.8f))),
)

class HapticCurveTest :
  FunSpec({
    test("curve snapshots points and interpolates absolute normalized values") {
      val input = rise().points.toMutableList()
      val curve = HapticCurve(input)
      val hash = curve.hashCode()
      input.clear()
      curve shouldBe rise()
      curve.hashCode() shouldBe hash
      (curve.points is MutableList<*>) shouldBe false
      curve.durationMs shouldBe 300L
      curve.valueAt(-1L) shouldBe 0f
      curve.valueAt(0L) shouldBe 0f
      curve.valueAt(40L) shouldBe 0.4f
      curve.valueAt(80L) shouldBe 0.8f
      curve.valueAt(190L) shouldBe 0.4f
      curve.valueAt(Long.MAX_VALUE) shouldBe 0f
    }

    test("curve rejects invalid values, endpoints, ordering, and excessive allocation") {
      listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -0.1f, 1.1f).forEach {
        shouldThrow<IllegalArgumentException> { HapticControlPoint(0L, it) }
      }
      shouldThrow<IllegalArgumentException> { HapticControlPoint(-1L, 0f) }
      listOf(
        emptyList(),
        listOf(HapticControlPoint(0L, 0f)),
        listOf(HapticControlPoint(1L, 0f), HapticControlPoint(2L, 1f)),
        listOf(HapticControlPoint(0L, 0f), HapticControlPoint(0L, 1f)),
        listOf(HapticControlPoint(0L, 0f), HapticControlPoint(2L, 1f), HapticControlPoint(1L, 0f)),
      ).forEach { shouldThrow<IllegalArgumentException> { HapticCurve(it) } }
      val oversized = object : AbstractList<HapticControlPoint>() {
        override val size: Int get() = 100_001
        override fun get(index: Int): HapticControlPoint = error("must reject before copying")
      }
      shouldThrow<IllegalArgumentException> { HapticCurve(oversized) }
    }

    test("explicit event shape and curve endpoints are validated") {
      shouldThrow<IllegalArgumentException> {
        ScheduledHapticEvent(0L, 0L, HapticIntensity.MEDIUM, eventType = HapticEventType.CONTINUOUS)
      }
      shouldThrow<IllegalArgumentException> {
        ScheduledHapticEvent(0L, 1L, HapticIntensity.MEDIUM, eventType = HapticEventType.TRANSIENT)
      }
      shouldThrow<IllegalArgumentException> {
        ScheduledHapticEvent(0L, 0L, HapticIntensity.MEDIUM, eventType = HapticEventType.TRANSIENT, intensityCurve = rise())
      }
      shouldThrow<IllegalArgumentException> { curved().copy(durationMs = 299L) }
      listOf(Float.NaN, Float.POSITIVE_INFINITY, -0.1f, 1.1f).forEach {
        shouldThrow<IllegalArgumentException> { curved().copy(eventType = HapticEventType.CONTINUOUS, sharpness = it) }
      }
      val legacy = ScheduledHapticEvent(0L, 0L, HapticIntensity.MEDIUM)
      legacy.eventType shouldBe HapticEventType.CONTINUOUS
      legacy.copy(startTimeMs = 1L).durationMs shouldBe 0L
      HapticPattern(listOf(legacy)).scaleIntensity(0f).events.single().durationMs shouldBe 0L
      curved().copy(startTimeMs = 10L) shouldBe curved(10L)
      val (start, duration, intensity, ios) = curved()
      listOf(start, duration, intensity, ios) shouldBe listOf(0L, 300L, HapticIntensity.STRONG, null)
      (curved() == curved().copy(eventType = HapticEventType.CONTINUOUS, sharpness = 0.7f)) shouldBe false
    }

    test("scaling changes fixed intensity and curve but preserves sharpness and timing") {
      val original = HapticPattern(listOf(curved(10L)), 400L)
      val scaled = original.scaleIntensity(2f)
      scaled.durationMs shouldBe 400L
      scaled.events.single().let {
        it.intensity.value shouldBe 1f
        it.intensityCurve!!.points.map { point -> point.value } shouldBe listOf(0f, 1f, 0f)
        it.sharpnessCurve shouldBe original.events.single().sharpnessCurve
        it.sharpness shouldBe 0.6f
      }
      original.scaleIntensity(0f).events.single().intensityCurve!!.points.map { it.value } shouldBe listOf(0f, 0f, 0f)
    }

    test("stretch uses absolute point boundaries before restoring event relative times") {
      val shortCurve = HapticCurve(listOf(HapticControlPoint(0L, 0f), HapticControlPoint(3L, 0f)))
      val event = ScheduledHapticEvent(1L, 3L, HapticIntensity.HIGH, eventType = HapticEventType.CONTINUOUS, intensityCurve = shortCurve)
      HapticPattern(listOf(event), 5L).timeStretch(0.5f).let {
        it.durationMs shouldBe 3L
        it.events.single().startTimeMs shouldBe 1L
        it.events.single().durationMs shouldBe 1L
      }
    }

    test("stretch rejects rounded duplicate curve points") {
      shouldThrow<IllegalArgumentException> {
        val curve = HapticCurve(listOf(HapticControlPoint(0L, 0f), HapticControlPoint(1L, 1f), HapticControlPoint(3L, 0f)))
        val event = ScheduledHapticEvent(1L, 3L, HapticIntensity.HIGH, eventType = HapticEventType.CONTINUOUS, intensityCurve = curve)
        HapticPattern(listOf(event), 5L).timeStretch(0.5f)
      }
      HapticPattern(listOf(curved(1L)), 400L).timeStretch(2f).events.single().let {
        it.startTimeMs shouldBe 2L
        it.durationMs shouldBe 600L
        it.intensityCurve!!.points.map { point -> point.timeMs } shouldBe listOf(0L, 160L, 600L)
        it.sharpnessCurve!!.durationMs shouldBe 600L
      }
    }

    test("reverse reflects both curves and transients and twice restores input order") {
      val transient = ScheduledHapticEvent(40L, 0L, HapticIntensity.HIGH, eventType = HapticEventType.TRANSIENT)
      val input = HapticPattern(listOf(curved(10L), transient, curved(10L).copy(durationMs = 300L)), 400L)
      val reversed = input.reversed()
      reversed.events.map { it.startTimeMs } shouldBe listOf(90L, 360L, 90L)
      reversed.events.first().intensityCurve!!.points shouldBe listOf(
        HapticControlPoint(0L, 0f),
        HapticControlPoint(220L, 0.8f),
        HapticControlPoint(300L, 0f),
      )
      reversed.events.first().sharpnessCurve!!.points.map { it.value } shouldBe listOf(0.8f, 0.2f)
      reversed.reversed() shouldBe input
    }

    test("then repeat and old copy retain rich event values and enforce aggregate control point limits") {
      val pattern = HapticPattern(listOf(curved()), 400L)
      (pattern then pattern).events.map { it.startTimeMs } shouldBe listOf(0L, 400L)
      pattern.repeated(2).events[1] shouldBe curved(400L)
      val many = HapticCurve(List(11) { HapticControlPoint(it.toLong(), 0.5f) })
      val event = ScheduledHapticEvent(0L, 10L, HapticIntensity.HIGH, eventType = HapticEventType.CONTINUOUS, intensityCurve = many)
      val dense = HapticPattern(List(5_000) { event })
      shouldThrow<IllegalArgumentException> { dense.repeated(2) }
      shouldThrow<IllegalArgumentException> { dense then dense }
      shouldThrow<IllegalArgumentException> { HapticPattern(List(10_000) { event }) }
    }
  })
