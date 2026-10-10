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
package io.github.compose.jindong.core.dsl

import io.github.compose.jindong.core.model.HapticEventType
import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.ms
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds

class RichHapticPatternTest :
  FunSpec({
    test("celebration compiles transient rhythm and shared intensity and sharpness curves") {
      val rise = hapticCurve {
        point(0.ms, 0f)
        point(80.ms, 0.8f)
        point(300.ms, 0f)
      }
      val texture = hapticCurve {
        point(0.ms, 0.2f)
        point(300.ms, 0.8f)
      }
      val pattern = buildHapticPattern {
        repeat(3) {
          transient(HapticIntensity.STRONG, sharpness = 0.9f)
          delay(80.ms)
        }
        continuous(300.ms, intensityCurve = rise, sharpnessCurve = texture)
      }
      pattern.durationMs shouldBe 540L
      pattern.events.map { it.startTimeMs } shouldBe listOf(0L, 80L, 160L, 240L)
      pattern.events.map { it.durationMs } shouldBe listOf(0L, 0L, 0L, 300L)
      pattern.events.map { it.eventType } shouldBe List(3) { HapticEventType.TRANSIENT } + HapticEventType.CONTINUOUS
      pattern.events.last().intensityCurve shouldBe rise
      pattern.events.last().sharpnessCurve shouldBe texture
      buildHapticPattern {
        delay(100.ms)
        clip(pattern)
        include(pattern)
      }.let {
        it.durationMs shouldBe 1180L
        it.events[3].intensityCurve shouldBe rise
        it.events[3].startTimeMs shouldBe 340L
        it.events[7].startTimeMs shouldBe 880L
      }
    }

    test("instantaneous events preserve input priority and do not advance sequence time") {
      val pattern = buildHapticPattern {
        transient(HapticIntensity.HIGH)
        transient(HapticIntensity.LIGHT)
        continuous(1.ms)
      }
      pattern.events.map { it.startTimeMs } shouldBe listOf(0L, 0L, 0L)
      pattern.events.map { it.intensity } shouldBe listOf(HapticIntensity.HIGH, HapticIntensity.LIGHT, HapticIntensity.MEDIUM)
      pattern.durationMs shouldBe 1L
      buildHapticPattern { transient() }.durationMs shouldBe 0L
      shouldThrow<IllegalArgumentException> { buildHapticPattern { repeat(10_001) { transient() } } }
    }

    test("builder rejects nonfinite or negative durations and rounded duplicate points") {
      listOf(0.ms, (-1).ms, Duration.INFINITE, 499.microseconds).forEach {
        shouldThrow<IllegalArgumentException> { buildHapticPattern { continuous(it) } }
      }
      shouldThrow<IllegalArgumentException> { buildHapticPattern { transient(sharpness = Float.NaN) } }
      shouldThrow<IllegalArgumentException> { hapticCurve { point(Duration.INFINITE, 0f) } }
      shouldThrow<IllegalArgumentException> { hapticCurve { point((-1).ms, 0f) } }
      shouldThrow<IllegalArgumentException> {
        hapticCurve {
          point(0.ms, 0f)
          point(499.microseconds, 1f)
        }
      }
      val short = hapticCurve {
        point(0.ms, 0f)
        point(10.ms, 1f)
      }
      shouldThrow<IllegalArgumentException> { buildHapticPattern { continuous(11.ms, intensityCurve = short) } }
    }
  })
