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

import io.github.compose.jindong.core.dsl.HapticPatternScope
import io.github.compose.jindong.core.dsl.buildHapticPattern
import io.github.compose.jindong.core.dsl.validateIndexedRepeatCount
import io.github.compose.jindong.core.element.DelayElement
import io.github.compose.jindong.core.element.RepeatElement
import io.github.compose.jindong.core.element.SequenceElement
import io.github.compose.jindong.core.element.VibrationElement
import io.github.compose.jindong.core.executor.rawSpanMs
import io.github.compose.jindong.core.ms
import io.github.compose.jindong.core.toHapticMilliseconds
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.nanoseconds

private fun tick(): HapticPattern = buildHapticPattern {
  haptic(50.ms)
  delay(50.ms)
}

class LogicalDurationTest :
  FunSpec({
    test("builder and include preserve trailing silence and delay-only patterns") {
      val tick = tick()
      tick.durationMs shouldBe 100L
      tick.rawSpanMs() shouldBe 50L
      buildHapticPattern {
        clip(tick)
        include(tick)
      }.let { pattern ->
        pattern.events.map { it.startTimeMs } shouldBe listOf(0L, 100L)
        pattern.durationMs shouldBe 200L
      }
      val silence = buildHapticPattern { delay(100.ms) }
      silence.events shouldBe emptyList()
      silence.durationMs shouldBe 100L
      silence.rawSpanMs() shouldBe 0L
      buildHapticPattern {
        include(silence)
        haptic(50.ms)
      }.events.single().startTimeMs shouldBe 100L
      (tick then buildHapticPattern { delay(30.ms) }).durationMs shouldBe 130L
      (silence then tick).events.single().startTimeMs shouldBe 100L
    }

    test("all transforms preserve complete logical timelines") {
      val tick = tick()
      tick.repeated(0) shouldBe HapticPattern.Empty
      tick.repeated(2).let { pattern ->
        pattern.events.map { it.startTimeMs } shouldBe listOf(0L, 100L)
        pattern.durationMs shouldBe 200L
      }
      tick.reversed().events.single().startTimeMs shouldBe 50L
      tick.reversed().durationMs shouldBe 100L
      tick.reversed().reversed() shouldBe tick
      tick.timeStretch(2f).let { pattern ->
        pattern.durationMs shouldBe 200L
        pattern.events.single().durationMs shouldBe 100L
      }
      tick.scaleIntensity(0f).durationMs shouldBe 100L
      buildHapticPattern {
        include(tick.reversed())
        include(tick.timeStretch(2f))
        include(tick.scaleIntensity(0f))
      }.let { pattern ->
        pattern.events.map { it.startTimeMs } shouldBe listOf(50L, 100L, 300L)
        pattern.durationMs shouldBe 400L
      }
    }

    test("events-only construction uses event end and explicit duration affects value equality") {
      val events = tick().events
      HapticPattern(events).durationMs shouldBe 50L
      val a = HapticPattern(events, 100L)
      val b = HapticPattern(events, 101L)
      (a == b) shouldBe false
      a.hashCode() shouldBe HapticPattern(events, 100L).hashCode()
      setOf(a, b).size shouldBe 2
      a.copy() shouldBe a
      val (copiedEvents, duration) = a
      copiedEvents shouldBe events
      duration shouldBe 100L
      shouldThrow<IllegalArgumentException> { HapticPattern(events, 49L) }
      shouldThrow<IllegalArgumentException> { HapticPattern(emptyList(), -1L) }
    }

    test("compiled values are independent of caller lists and builder trees") {
      val input = tick().events.toMutableList()
      val pattern = HapticPattern(input, 100L)
      val originalHash = pattern.hashCode()
      input.clear()
      pattern.events.size shouldBe 1
      pattern.hashCode() shouldBe originalHash
      (pattern.events is MutableList<*>) shouldBe false
      lateinit var scope: HapticPatternScope
      val built = buildHapticPattern {
        scope = this
        haptic(50.ms)
        delay(50.ms)
      }
      scope.rootElement.children.clear()
      built shouldBe pattern
      buildHapticPattern { include(built) } shouldBe pattern
    }

    test("duration rounding is nearest millisecond and rejects invalid vibration inputs") {
      499.microseconds.toHapticMilliseconds() shouldBe 0L
      500.microseconds.toHapticMilliseconds() shouldBe 1L
      1499.microseconds.toHapticMilliseconds() shouldBe 1L
      1500.microseconds.toHapticMilliseconds() shouldBe 2L
      buildHapticPattern {
        haptic(500.microseconds)
        delay(1500.microseconds)
      }.durationMs shouldBe 3L
      buildHapticPattern { delay(0.ms) } shouldBe HapticPattern.Empty
      shouldThrow<IllegalArgumentException> { buildHapticPattern { haptic(499.microseconds) } }
        .message shouldContain "at least 1ms"
      listOf(-1.nanoseconds, Duration.INFINITE, -Duration.INFINITE).forEach { duration ->
        shouldThrow<IllegalArgumentException> { buildHapticPattern { delay(duration) } }
        shouldThrow<IllegalArgumentException> { buildHapticPattern { haptic(duration) } }
      }
    }

    test("direct event and arithmetic overflow are rejected") {
      shouldThrow<IllegalArgumentException> { ScheduledHapticEvent(-1L, 1L, HapticIntensity.HIGH) }
      shouldThrow<IllegalArgumentException> { ScheduledHapticEvent(0L, -1L, HapticIntensity.HIGH) }
      shouldThrow<IllegalArgumentException> { ScheduledHapticEvent(Long.MAX_VALUE, 1L, HapticIntensity.HIGH) }
      val longest = HapticPattern(emptyList(), Long.MAX_VALUE)
      shouldThrow<IllegalArgumentException> { longest then HapticPattern(emptyList(), 1L) }
      shouldThrow<IllegalArgumentException> { longest.repeated(2) }
      shouldThrow<IllegalArgumentException> { longest.timeStretch(2f) }
      longest.timeStretch(1f) shouldBe longest
      val sequence = SequenceElement().apply {
        children.add(DelayElement(Long.MAX_VALUE))
        children.add(DelayElement(1L))
      }
      shouldThrow<IllegalArgumentException> { sequence.collectEvents(0L) }
      val repeat = RepeatElement(2).apply { children.add(DelayElement(Long.MAX_VALUE)) }
      shouldThrow<IllegalArgumentException> { repeat.collectEvents(0L) }
    }

    test("repeat expansion is checked before allocation while huge silent repetitions stay cheap") {
      val tick = tick()
      val oversized = object : AbstractList<ScheduledHapticEvent>() {
        override val size: Int get() = 10_001
        override fun get(index: Int): ScheduledHapticEvent = error("must reject before reading events")
      }
      shouldThrow<IllegalArgumentException> { HapticPattern(oversized) }
      shouldThrow<IllegalArgumentException> { HapticPattern(oversized, 0L) }
      tick.repeated(10_000).events.size shouldBe 10_000
      shouldThrow<IllegalArgumentException> { tick.repeated(Int.MAX_VALUE) }.message shouldContain "limit 10000"
      shouldThrow<IllegalArgumentException> {
        buildHapticPattern { repeat(Int.MAX_VALUE) { haptic(1.ms) } }
      }.message shouldContain "Repeat"
      shouldThrow<IllegalArgumentException> {
        buildHapticPattern { repeat(101) { repeat(100) { haptic(1.ms) } } }
      }
      buildHapticPattern { repeat(Int.MAX_VALUE) { delay(1.ms) } }.durationMs shouldBe Int.MAX_VALUE.toLong()
      HapticPattern(emptyList(), 1L).repeated(Int.MAX_VALUE).durationMs shouldBe Int.MAX_VALUE.toLong()
      val repeat = RepeatElement(0).apply { children.add(VibrationElement(1L, HapticIntensity.HIGH)) }
      repeat.collectEvents(0L) shouldBe emptyList()
      shouldThrow<IllegalArgumentException> { buildHapticPattern { repeatWithIndex(-1) {} } }
    }

    test("indexed repeat rejects excessive counts before invoking the block") {
      listOf(-1, 10_001, Int.MAX_VALUE).forEach { count ->
        var invoked = false
        shouldThrow<IllegalArgumentException> {
          buildHapticPattern {
            repeatWithIndex(count) {
              invoked = true
              error("must reject before invoking the block")
            }
          }
        }.message shouldContain "indexed repeat count"
        invoked shouldBe false
      }
      validateIndexedRepeatCount(10_000)
      buildHapticPattern { repeatWithIndex(0) { error("zero iterations") } } shouldBe HapticPattern.Empty
    }

    test("stretch uses one rounded time axis and retains precise large integer boundaries") {
      val events = listOf(
        ScheduledHapticEvent(0L, 3L, HapticIntensity.HIGH),
        ScheduledHapticEvent(3L, 3L, HapticIntensity.HIGH),
      )
      HapticPattern(events, 7L).timeStretch(0.5f).let { pattern ->
        pattern.events.map { it.startTimeMs to it.durationMs } shouldBe listOf(0L to 2L, 2L to 1L)
        pattern.durationMs shouldBe 4L
      }
      val halfMax = Long.MAX_VALUE / 2
      HapticPattern(emptyList(), halfMax).timeStretch(2f).durationMs shouldBe Long.MAX_VALUE - 1L
      HapticPattern(emptyList(), Long.MAX_VALUE).timeStretch(0.5f).durationMs shouldBe (1L shl 62)
      scaleTime(16_777_217L, 2f) shouldBe 33_554_434L
      scaleTime(Long.MAX_VALUE, Float.MIN_VALUE) shouldBe 0L
      scaleTime(1L, 0.5f) shouldBe 1L
      scaleTime(1L, 16_777_216f) shouldBe 16_777_216L
      scaleTime(0L, Float.MAX_VALUE) shouldBe 0L
      shouldThrow<IllegalArgumentException> { scaleTime(1L, Float.MAX_VALUE) }
      shouldThrow<IllegalArgumentException> { tick().timeStretch(0.001f) }
    }

    test("non-finite transform factors and custom intensities are rejected") {
      listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).forEach { value ->
        shouldThrow<IllegalArgumentException> { tick().timeStretch(value) }
        shouldThrow<IllegalArgumentException> { tick().scaleIntensity(value) }
        shouldThrow<IllegalArgumentException> { HapticIntensity.Custom(value) }
      }
    }
  })
