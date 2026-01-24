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
package io.github.compose.jindong.core.element

import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.model.IosHapticParameters
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class RepeatElementTest :
  FunSpec({
    test("should throw IllegalArgumentException when count is negative") {
      val exception = shouldThrow<IllegalArgumentException> {
        RepeatElement(count = -5)
      }
      exception.message shouldContain "count must be non-negative"
    }

    test("collectEvents should return empty list when no children") {
      val element = RepeatElement(count = 3)

      val events = element.collectEvents(startTimeMs = 0)

      events.shouldBeEmpty()
    }

    test("collectEvents should return empty list when count is 0") {
      val element = RepeatElement(count = 0)
      element.children.add(VibrationElement(durationMs = 100, intensity = HapticIntensity.STRONG))

      val events = element.collectEvents(startTimeMs = 0)

      events.shouldBeEmpty()
    }

    test("collectEvents should repeat single child correctly") {
      val element = RepeatElement(count = 3)
      element.children.add(VibrationElement(durationMs = 50, intensity = HapticIntensity.STRONG))

      val events = element.collectEvents(startTimeMs = 0)

      assertSoftly {
        events shouldHaveSize 3
        events[0].startTimeMs shouldBe 0
        events[1].startTimeMs shouldBe 50
        events[2].startTimeMs shouldBe 100
        events.forEach { it.durationMs shouldBe 50 }
        events.forEach { it.intensity shouldBe HapticIntensity.STRONG }
      }
    }

    test("collectEvents should repeat multiple children sequentially") {
      val element = RepeatElement(count = 2)
      element.children.add(VibrationElement(durationMs = 50, intensity = HapticIntensity.HIGH))
      element.children.add(VibrationElement(durationMs = 30, intensity = HapticIntensity.MEDIUM))

      val events = element.collectEvents(startTimeMs = 0)

      assertSoftly {
        events shouldHaveSize 4

        assertSoftly(events[0]) {
          startTimeMs shouldBe 0
          durationMs shouldBe 50
        }
        assertSoftly(events[1]) {
          startTimeMs shouldBe 50
          durationMs shouldBe 30
        }
        assertSoftly(events[2]) {
          startTimeMs shouldBe 80
          durationMs shouldBe 50
        }
        assertSoftly(events[3]) {
          startTimeMs shouldBe 130
          durationMs shouldBe 30
        }
      }
    }

    test("collectEvents should offset next iteration start time by delay duration") {
      val element = RepeatElement(count = 2)
      element.children.add(VibrationElement(durationMs = 50, intensity = HapticIntensity.STRONG))
      element.children.add(DelayElement(durationMs = 100))

      val events = element.collectEvents(startTimeMs = 0)

      assertSoftly {
        events shouldHaveSize 2
        assertSoftly(events[0]) {
          startTimeMs shouldBe 0
          durationMs shouldBe 50
        }
        assertSoftly(events[1]) {
          startTimeMs shouldBe 150
          durationMs shouldBe 50
        }
      }
    }

    test("collectEvents should not produce events for pure delay elements") {
      val element = RepeatElement(count = 3)
      element.children.add(DelayElement(durationMs = 100))

      val events = element.collectEvents(startTimeMs = 0)

      events.shouldBeEmpty()
    }

    test("collectEvents should preserve custom startTimeMs") {
      val element = RepeatElement(count = 2)
      element.children.add(VibrationElement(durationMs = 50, intensity = HapticIntensity.STRONG))

      val events = element.collectEvents(startTimeMs = 500)

      assertSoftly {
        events shouldHaveSize 2
        events[0].startTimeMs shouldBe 500
        events[1].startTimeMs shouldBe 550
      }
    }

    test("collectEvents should handle nested SequenceElement") {
      val innerSequence = SequenceElement()
      innerSequence.children.add(VibrationElement(durationMs = 30, intensity = HapticIntensity.MEDIUM))
      innerSequence.children.add(VibrationElement(durationMs = 20, intensity = HapticIntensity.MEDIUM))

      val element = RepeatElement(count = 2)
      element.children.add(innerSequence)

      val events = element.collectEvents(startTimeMs = 0)

      assertSoftly {
        events shouldHaveSize 4
        events[0].startTimeMs shouldBe 0
        events[1].startTimeMs shouldBe 30
        // starts at 50ms = 30 + 20
        events[2].startTimeMs shouldBe 50
        events[3].startTimeMs shouldBe 80
      }
    }

    test("collectEvents should preserve iOS parameters") {
      val iosParams = IosHapticParameters(sharpness = 0.9f)
      val element = RepeatElement(count = 2)
      element.children.add(
        VibrationElement(durationMs = 100, intensity = HapticIntensity.STRONG, iosParameters = iosParams),
      )

      val events = element.collectEvents(startTimeMs = 0)

      assertSoftly {
        events shouldHaveSize 2
        events[0].iosParameters?.sharpness shouldBe 0.9f
        events[1].iosParameters?.sharpness shouldBe 0.9f
      }
    }

    test("collectEvents should match documentation example - Repeat with Haptic and Delay") {
      val element = RepeatElement(count = 3)
      element.children.add(VibrationElement(durationMs = 50, intensity = HapticIntensity.HIGH))
      element.children.add(DelayElement(durationMs = 50))

      val events = element.collectEvents(startTimeMs = 0)

      assertSoftly {
        events shouldHaveSize 3
        events[0].startTimeMs shouldBe 0
        events[1].startTimeMs shouldBe 100
        events[2].startTimeMs shouldBe 200
        events.forEach { it.durationMs shouldBe 50 }
      }
    }

    test("collectEvents with count = 1 should execute children once") {
      val element = RepeatElement(count = 1)
      element.children.add(VibrationElement(durationMs = 100, intensity = HapticIntensity.STRONG))

      val events = element.collectEvents(startTimeMs = 0)

      assertSoftly {
        events shouldHaveSize 1
        events[0].startTimeMs shouldBe 0
        events[0].durationMs shouldBe 100
      }
    }

    test("totalDurationMs should multiply children duration by count") {
      val element = RepeatElement(count = 2)
      element.children.add(VibrationElement(durationMs = 50, intensity = HapticIntensity.STRONG))
      element.children.add(DelayElement(durationMs = 100))

      // (50 haptic + 100 delay) * 2 = 300
      element.totalDurationMs(startTimeMs = 0) shouldBe 300
    }

    test("totalDurationMs should handle nested SequenceElement with delays") {
      val innerSequence = SequenceElement()
      innerSequence.children.add(VibrationElement(durationMs = 30, intensity = HapticIntensity.MEDIUM))
      innerSequence.children.add(DelayElement(durationMs = 20))

      val element = RepeatElement(count = 2)
      element.children.add(innerSequence)

      // (30 haptic + 20 delay) * 2 = 100
      element.totalDurationMs(startTimeMs = 0) shouldBe 100
    }

    test("totalDurationMs should return 0 when count is 0") {
      val element = RepeatElement(count = 0)
      element.children.add(VibrationElement(durationMs = 100, intensity = HapticIntensity.STRONG))

      element.totalDurationMs(startTimeMs = 0) shouldBe 0
    }
  })
