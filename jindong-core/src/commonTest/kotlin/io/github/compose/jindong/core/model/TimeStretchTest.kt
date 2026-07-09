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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.float
import io.kotest.property.checkAll
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToLong

private fun factors(): Arb<Float> = Arb.float(0.1f..5f)

class TimeStretchTest :
  FunSpec({
    test("stretching by 1 is the identity") {
      checkAll(patterns()) { pattern ->
        pattern.timeStretch(1f) shouldBe pattern
      }
    }

    test("stretching preserves event count and never yields a negative duration") {
      checkAll(patterns(), factors()) { pattern, factor ->
        val stretched = pattern.timeStretch(factor)
        stretched.events.size shouldBe pattern.events.size
        stretched.events.forEach { it.durationMs shouldBeGreaterThanOrEqual 0L }
      }
    }

    test("composing two stretches equals stretching by the product, within rounding drift") {
      checkAll(patterns(), factors(), factors()) { pattern, a, b ->
        val composed = pattern.timeStretch(a).timeStretch(b)
        val combined = pattern.timeStretch(a * b)
        // The composed path rounds each edge with factor a (±0.5) and then scales that error by b and
        // rounds again (±0.5), so per-edge drift is bounded by b * 0.5 + 0.5. Round up and add a 1ms
        // float-slack margin; duration is the difference of two edges, so it gets twice the bound.
        val edgeTolerance = ceil(b * 0.5 + 0.5).toLong() + 1L
        composed.events.zip(combined.events).forEach { (c, k) ->
          abs(c.startTimeMs - k.startTimeMs) shouldBeLessThanOrEqual edgeTolerance
          abs(c.durationMs - k.durationMs) shouldBeLessThanOrEqual 2 * edgeTolerance
        }
      }
    }

    test("stretched span equals the rounded scaled span, within rounding drift") {
      checkAll(patterns(), factors()) { pattern, factor ->
        val expected = (pattern.spanMs() * factor).roundToLong()
        abs(pattern.timeStretch(factor).spanMs() - expected) shouldBeLessThanOrEqual 1L
      }
    }
  })
