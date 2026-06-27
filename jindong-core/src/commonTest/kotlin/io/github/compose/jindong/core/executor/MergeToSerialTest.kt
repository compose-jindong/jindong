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

import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.model.IosHapticParameters
import io.github.compose.jindong.core.model.ScheduledHapticEvent
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

private fun event(
  startTimeMs: Long,
  durationMs: Long,
  intensity: HapticIntensity,
  sharpness: Float? = null,
): ScheduledHapticEvent = ScheduledHapticEvent(
  startTimeMs = startTimeMs,
  durationMs = durationMs,
  intensity = intensity,
  iosParameters = sharpness?.let { IosHapticParameters(sharpness = it) },
)

class MergeToSerialTest :
  FunSpec({
    test("single event produces a single segment preserving intensity") {
      val segments = mergeToSerial(
        listOf(event(startTimeMs = 0, durationMs = 100, intensity = HapticIntensity.HIGH)),
      )

      segments shouldHaveSize 1
      assertSoftly(segments.single()) {
        startTimeMs shouldBe 0L
        durationMs shouldBe 100L
        intensity shouldBe HapticIntensity.HIGH.value
      }
    }

    test("adjacent non-overlapping events produce gapless back-to-back segments") {
      val segments = mergeToSerial(
        listOf(
          event(startTimeMs = 0, durationMs = 25, intensity = HapticIntensity.MEDIUM),
          event(startTimeMs = 25, durationMs = 25, intensity = HapticIntensity.MEDIUM),
          event(startTimeMs = 50, durationMs = 25, intensity = HapticIntensity.MEDIUM),
        ),
      )

      segments shouldHaveSize 3
      segments.map { it.durationMs } shouldBe listOf(25L, 25L, 25L)
      segments.sumOf { it.durationMs } shouldBe 75L
      segments.all { it.intensity == HapticIntensity.MEDIUM.value } shouldBe true
    }

    test("overlapping events keep the higher intensity as winner (no summing)") {
      // [0,100) HIGH overlaps [50,150) MEDIUM.
      val segments = mergeToSerial(
        listOf(
          event(startTimeMs = 0, durationMs = 100, intensity = HapticIntensity.HIGH),
          event(startTimeMs = 50, durationMs = 100, intensity = HapticIntensity.MEDIUM),
        ),
      )

      segments shouldHaveSize 3
      assertSoftly {
        segments[0] shouldBe HapticSegment(0, 50, HapticIntensity.HIGH.value, 0.5f)
        segments[1] shouldBe HapticSegment(50, 50, HapticIntensity.HIGH.value, 0.5f)
        segments[2] shouldBe HapticSegment(100, 50, HapticIntensity.MEDIUM.value, 0.5f)
      }
      segments.none { it.isGap } shouldBe true
      // total spans the full [0,150) timeline, NOT a back-to-back 80ms compression.
      segments.sumOf { it.durationMs } shouldBe 150L
    }

    test("disjoint events preserve the gap between them as a 0f segment") {
      val segments = mergeToSerial(
        listOf(
          event(startTimeMs = 0, durationMs = 50, intensity = HapticIntensity.HIGH),
          event(startTimeMs = 100, durationMs = 50, intensity = HapticIntensity.HIGH),
        ),
      )

      segments shouldHaveSize 3
      assertSoftly {
        segments[0].intensity shouldBe HapticIntensity.HIGH.value
        segments[0].isGap shouldBe false
        segments[1] shouldBe HapticSegment(50, 50, 0f, 0.5f, isGap = true)
        segments[2].intensity shouldBe HapticIntensity.HIGH.value
      }
      segments.sumOf { it.durationMs } shouldBe 150L
    }

    test("fully contained event alternates the winner and carries its sharpness") {
      // [0,200) LOW wraps [50,100) HIGH (with custom sharpness).
      val segments = mergeToSerial(
        listOf(
          event(startTimeMs = 0, durationMs = 200, intensity = HapticIntensity.LIGHT, sharpness = 0.2f),
          event(startTimeMs = 50, durationMs = 50, intensity = HapticIntensity.HIGH, sharpness = 0.9f),
        ),
      )

      segments shouldHaveSize 3
      assertSoftly {
        segments[0] shouldBe HapticSegment(0, 50, HapticIntensity.LIGHT.value, 0.2f)
        segments[1] shouldBe HapticSegment(50, 50, HapticIntensity.HIGH.value, 0.9f)
        segments[2] shouldBe HapticSegment(100, 100, HapticIntensity.LIGHT.value, 0.2f)
      }
      segments.sumOf { it.durationMs } shouldBe 200L
    }

    test("a leading gap before the first event is preserved") {
      val segments = mergeToSerial(
        listOf(event(startTimeMs = 100, durationMs = 50, intensity = HapticIntensity.HIGH)),
      )

      segments shouldHaveSize 2
      segments[0] shouldBe HapticSegment(0, 100, 0f, 0.5f, isGap = true)
      segments[1].intensity shouldBe HapticIntensity.HIGH.value
      segments[1].isGap shouldBe false
      segments.sumOf { it.durationMs } shouldBe 150L
    }

    test("tied intensity resolves deterministically to the earlier input event") {
      val first = event(startTimeMs = 0, durationMs = 200, intensity = HapticIntensity.HIGH, sharpness = 0.1f)
      val second = event(startTimeMs = 50, durationMs = 50, intensity = HapticIntensity.HIGH, sharpness = 0.9f)

      val segments = mergeToSerial(listOf(first, second))

      // [50,100) is a tie (both HIGH); the earlier event (first) wins -> sharpness 0.1f.
      val tiedSegment = segments.single { it.startTimeMs == 50L }
      tiedSegment.sharpness shouldBe 0.1f
    }

    test("an active zero-intensity event is not flagged as a gap") {
      val segments = mergeToSerial(
        listOf(event(startTimeMs = 0, durationMs = 100, intensity = HapticIntensity.Custom(0.0f))),
      )

      segments shouldHaveSize 1
      assertSoftly(segments.single()) {
        intensity shouldBe 0f
        isGap shouldBe false
      }
    }

    test("operates independently of input ordering") {
      val unsorted = listOf(
        event(startTimeMs = 50, durationMs = 25, intensity = HapticIntensity.MEDIUM),
        event(startTimeMs = 0, durationMs = 25, intensity = HapticIntensity.MEDIUM),
        event(startTimeMs = 25, durationMs = 25, intensity = HapticIntensity.MEDIUM),
      )

      val segments = mergeToSerial(unsorted)

      segments.map { it.startTimeMs } shouldBe listOf(0L, 25L, 50L)
      segments.sumOf { it.durationMs } shouldBe 75L
    }
  })
