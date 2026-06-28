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

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll

private fun active(startTimeMs: Long, durationMs: Long, intensity: Float): HapticSegment = HapticSegment(startTimeMs = startTimeMs, durationMs = durationMs, intensity = intensity, sharpness = 0.5f, isGap = false)

private fun gap(startTimeMs: Long, durationMs: Long): HapticSegment = HapticSegment(startTimeMs = startTimeMs, durationMs = durationMs, intensity = 0f, sharpness = 0.5f, isGap = true)

class InsertFallRampsTest :
  FunSpec({
    test("active to gap inserts a decaying ramp step borrowed from the gap front") {
      // STRONG (0.75) active for 100ms, then a 16ms gap.
      val input = listOf(
        active(startTimeMs = 0, durationMs = 100, intensity = 0.75f),
        gap(startTimeMs = 100, durationMs = 16),
      )

      val output = insertFallRamps(input)

      // active (untouched) + 1 ramp step (8ms @ 0.375) + shrunken gap (8ms @ 0).
      output shouldHaveSize 3
      assertSoftly {
        output[0] shouldBe input[0]
        with(output[1]) {
          startTimeMs shouldBe 100L
          durationMs shouldBe 8L
          intensity shouldBe (0.375f plusOrMinus 1e-6f)
          isGap shouldBe false
        }
        with(output[2]) {
          startTimeMs shouldBe 108L
          durationMs shouldBe 8L
          intensity shouldBe 0f
          isGap shouldBe true
        }
      }
    }

    test("total duration is conserved (ramp is borrowed, never added)") {
      val input = listOf(
        active(startTimeMs = 0, durationMs = 100, intensity = 0.75f),
        gap(startTimeMs = 100, durationMs = 50),
        active(startTimeMs = 150, durationMs = 100, intensity = 1.0f),
        gap(startTimeMs = 250, durationMs = 30),
      )

      val output = insertFallRamps(input)

      output.sumOf { it.durationMs } shouldBe input.sumOf { it.durationMs }
    }

    test("ramp shrinks to fit a gap smaller than the full ramp window") {
      // 10ms gap (< FALL_RAMP_MS 16) -> effective ramp 10ms, step 5ms.
      val input = listOf(
        active(startTimeMs = 0, durationMs = 100, intensity = 0.5f),
        gap(startTimeMs = 100, durationMs = 10),
      )

      val output = insertFallRamps(input)

      output shouldHaveSize 3
      assertSoftly {
        output[1].durationMs shouldBe 5L
        output[1].intensity shouldBe (0.25f plusOrMinus 1e-6f)
        output[1].isGap shouldBe false
        output[2].durationMs shouldBe 5L
        output[2].isGap shouldBe true
      }
      output.sumOf { it.durationMs } shouldBe 110L
    }

    test("gap at or below the minimum is left untouched") {
      val input = listOf(
        active(startTimeMs = 0, durationMs = 100, intensity = 0.75f),
        gap(startTimeMs = 100, durationMs = 4),
      )

      val output = insertFallRamps(input)

      output shouldBe input
    }

    test("active to active transition gets no ramp") {
      val input = listOf(
        active(startTimeMs = 0, durationMs = 50, intensity = 0.75f),
        active(startTimeMs = 50, durationMs = 50, intensity = 0.5f),
      )

      val output = insertFallRamps(input)

      output shouldBe input
    }

    test("leading gap before the first active is not ramped") {
      val input = listOf(
        gap(startTimeMs = 0, durationMs = 100),
        active(startTimeMs = 100, durationMs = 50, intensity = 0.75f),
      )

      val output = insertFallRamps(input)

      output shouldBe input
    }

    test("multiple active-to-gap boundaries each get their own ramp") {
      val input = listOf(
        active(startTimeMs = 0, durationMs = 100, intensity = 1.0f),
        gap(startTimeMs = 100, durationMs = 50),
        active(startTimeMs = 150, durationMs = 100, intensity = 0.5f),
        gap(startTimeMs = 250, durationMs = 50),
      )

      val output = insertFallRamps(input)

      // Each gap splits into [ramp step, gap], so 2 actives + 2 ramps + 2 gaps = 6.
      output shouldHaveSize 6
      output.count { !it.isGap && it.intensity > 0f && it.durationMs == 8L } shouldBe 2
      output.sumOf { it.durationMs } shouldBe input.sumOf { it.durationMs }
    }

    test("a single segment is returned unchanged") {
      val input = listOf(active(startTimeMs = 0, durationMs = 100, intensity = 0.75f))

      insertFallRamps(input) shouldBe input
    }

    // Property tests over randomly built timelines. The duration-conservation invariant must hold
    // regardless of gap parity, so odd gaps (where ramp splitting has an integer-division remainder
    // the leftover gap must absorb) are exercised alongside even ones.
    test("duration is conserved for any timeline") {
      checkAll(timelines()) { input ->
        insertFallRamps(input).sumOf { it.durationMs } shouldBe input.sumOf { it.durationMs }
      }
    }

    test("active segments are preserved and ramp steps stay within their gap") {
      checkAll(timelines()) { input ->
        val output = insertFallRamps(input)
        // Every original active segment survives untouched (ramps only ever borrow from gaps).
        output.filter { !it.isGap && it in input } shouldContainAll input.filter { !it.isGap }
        // Output stays contiguous and non-negative: no ramp ever overruns its gap.
        output.forEach { it.durationMs shouldBeGreaterThan 0L }
        output.zipWithNext { a, b -> b.startTimeMs shouldBe a.startTimeMs + a.durationMs }
      }
    }
  })

/**
 * Generates merge-to-serial-shaped timelines: contiguous segments alternating active/gap with
 * cumulative start times, mixing even and odd gap durations and gaps below/above [MIN_RAMP_MS].
 */
private fun timelines(): Arb<List<HapticSegment>> = arbitrary { rs ->
  val count = Arb.int(1..6).bind()
  var cursor = 0L
  var wasGap = true // so the first segment can be active
  buildList {
    repeat(count) {
      val makeGap = if (wasGap) false else Arb.boolean().bind()
      val durationMs = Arb.long(1L..60L).bind()
      add(
        if (makeGap) {
          gap(startTimeMs = cursor, durationMs = durationMs)
        } else {
          active(startTimeMs = cursor, durationMs = durationMs, intensity = Arb.float(0f..1f).bind())
        },
      )
      cursor += durationMs
      wasGap = makeGap
    }
  }
}
