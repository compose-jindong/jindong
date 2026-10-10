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
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.math.abs
import kotlin.random.Random

private fun ramp(from: Float, to: Float, duration: Long = 100L, sharpness: Float = 0.5f): ScheduledHapticEvent = ScheduledHapticEvent(
  0L,
  duration,
  HapticIntensity.MEDIUM,
  eventType = HapticEventType.CONTINUOUS,
  sharpness = sharpness,
  intensityCurve = HapticCurve(listOf(HapticControlPoint(0L, from), HapticControlPoint(duration, to))),
)

class CurveMixingTest :
  FunSpec({
    test("crossing curves change the winning event and carry its sharpness") {
      val segments = mergeToSerial(listOf(ramp(1f, 0f, sharpness = 0.2f), ramp(0f, 1f, sharpness = 0.8f)))
      segments.map { it.durationMs } shouldBe listOf(50L, 50L)
      segments.map { it.intensity } shouldBe listOf(1f, 0.5f)
      segments.map { it.endIntensity } shouldBe listOf(0.5f, 1f)
      segments.map { it.sharpness } shouldBe listOf(0.2f, 0.8f)
    }

    test("equal curves retain input priority") {
      val segments = mergeToSerial(listOf(ramp(0f, 1f, sharpness = 0.2f), ramp(0f, 1f, sharpness = 0.8f)))
      segments.single().sharpness shouldBe 0.2f
    }

    test("constant curves retain input priority including signed zero") {
      for (value in listOf(0.5f, -0f)) {
        val first = ramp(value, value, sharpness = 0.2f)
        val second = ramp(value + 0f, value + 0f, sharpness = 0.8f)
        mergeToSerial(listOf(first, second)).single().sharpness shouldBe 0.2f
      }
    }

    test("hidden intersections do not split the winning curve") {
      val segments = mergeToSerial(listOf(ramp(0f, 0.5f), ramp(0.5f, 0f), ramp(1f, 1f)))
      segments.size shouldBe 1
      segments.single().intensity shouldBe 1f
      segments.single().endIntensity shouldBe 1f
    }

    test("fractional crossings preserve both neighboring millisecond boundaries") {
      val segments = mergeToSerial(listOf(ramp(1f, 0f, duration = 3L), ramp(0f, 1f, duration = 3L)))
      segments.map { it.startTimeMs } shouldBe listOf(0L, 1L, 2L)
      segments.sumOf { it.durationMs } shouldBe 3L
    }

    test("a point-only winner retains input priority at a fractional crossing midpoint") {
      val constant = ramp(0.5f, 0.5f, duration = 3L, sharpness = 0.2f)
      val falling = ramp(1f, 0f, duration = 3L, sharpness = 0.8f)
      val rising = ramp(0f, 1f, duration = 3L, sharpness = 0.9f)
      for (events in listOf(listOf(constant, falling, rising), listOf(falling, constant, rising), listOf(rising, falling, constant))) {
        val segments = mergeToSerial(events)
        segments.map { it.startTimeMs } shouldBe listOf(0L, 1L, 2L)
        segments[1].intensity shouldBe events.first().intensityCurve!!.valueAt(1L)
        segments[1].endIntensity shouldBe events.first().intensityCurve!!.valueAt(2L)
        segments[1].sharpness shouldBe events.first().sharpness
        segments[1].endSharpness shouldBe events.first().sharpness
      }
    }

    test("sharpness control points split the timeline even at constant intensity") {
      val event = ramp(1f, 1f).copy(
        eventType = HapticEventType.CONTINUOUS,
        sharpnessCurve = HapticCurve(listOf(HapticControlPoint(0, 0f), HapticControlPoint(25, 1f), HapticControlPoint(100, 0f))),
      )
      val segments = mergeToSerial(listOf(event))
      segments.map { it.durationMs } shouldBe listOf(25L, 75L)
      segments.map { it.sharpness } shouldBe listOf(0f, 1f)
      segments.map { it.endSharpness } shouldBe listOf(1f, 0f)
    }

    test("ramp completion at zero does not insert a new pulse into the following gap") {
      val falling = HapticSegment(0, 100, 1f, 0.5f, endIntensity = 0f)
      val gap = HapticSegment(100, 50, 0f, 0.5f, isGap = true)
      insertFallRamps(listOf(falling, gap)) shouldBe listOf(falling, gap)
    }

    test("fall ramp starts from the curve endpoint intensity") {
      val rising = HapticSegment(0, 100, 0f, 0.5f, endIntensity = 1f)
      val gap = HapticSegment(100, 50, 0f, 0.5f, isGap = true)
      val segments = insertFallRamps(listOf(rising, gap))
      segments[1].intensity shouldBe 0.5f
      segments.sumOf { it.durationMs } shouldBe 150L
    }

    test("large disjoint timelines use the active set instead of scanning every event") {
      val events = List(10_000) { index -> ramp(0f, 1f, duration = 1L).copy(startTimeMs = index * 2L) }
      mergeToSerial(events).sumOf { it.durationMs } shouldBe 19_999L
    }

    test("dense overlapping control points stop at the planning work budget") {
      val events = List(1_000) { index ->
        ramp(0f, 1f, duration = 2_000L).copy(
          eventType = HapticEventType.CONTINUOUS,
          sharpnessCurve = HapticCurve(listOf(HapticControlPoint(0, 0f), HapticControlPoint(index + 1L, 1f), HapticControlPoint(2_000, 0f))),
        )
      }
      shouldThrow<HapticPlanningLimitException> { mergeToSerial(events) }
    }

    test("maximum-size overlapping curves reuse the upper envelope across crossings") {
      val interval = 20_000L
      val iterations = 9
      val events = List(10_000) { index ->
        val x = index / 9_999.0
        val from = (0.3 - 0.25 * x * x).toFloat()
        val to = (0.3 - 0.25 * x * x + 0.5 * x).toFloat()
        ramp(from, to, duration = interval * iterations).copy(
          eventType = HapticEventType.CONTINUOUS,
          intensityCurve = HapticCurve(
            List(iterations + 1) { point -> HapticControlPoint(interval * point, if (point % 2 == 0) from else to) },
          ),
        )
      }
      val pattern = HapticPattern(events)
      val segments = mergeToSerial(pattern.events)
      segments.sumOf { it.durationMs } shouldBe pattern.durationMs
      (segments.size in 10_000..100_000) shouldBe true
      segments.zipWithNext().all { (left, right) -> left.startTimeMs + left.durationMs == right.startTimeMs } shouldBe true
    }

    test("random ramps agree with maximum-intensity mixing within the 1ms crossing bound") {
      val random = Random(59)
      repeat(150) {
        val duration = random.nextLong(5L, 100L)
        val events = List(random.nextInt(2, 12)) { ramp(random.nextFloat(), random.nextFloat(), duration) }
        val segments = mergeToSerial(events)
        val bound = events.maxOf { abs(it.intensityCurve!!.points.last().value - it.intensityCurve!!.points.first().value).toDouble() / duration } + 0.00001
        for (time in 0 until duration) {
          val segment = segments.first { time >= it.startTimeMs && time < it.startTimeMs + it.durationMs }
          val ratio = (time - segment.startTimeMs).toDouble() / segment.durationMs
          val mixed = segment.intensity + (segment.endIntensity - segment.intensity) * ratio
          val expected = events.maxOf { it.intensityCurve!!.valueAt(time).toDouble() }
          mixed shouldBe (expected plusOrMinus bound)
        }
        segments.sumOf { it.durationMs } shouldBe duration
      }
    }
  })
