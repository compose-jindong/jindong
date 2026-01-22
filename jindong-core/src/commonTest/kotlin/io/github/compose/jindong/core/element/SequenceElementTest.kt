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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class SequenceElementTest :
  FunSpec({
    test("collectEvents should return empty list when no children") {
      val element = SequenceElement()

      val events = element.collectEvents(startTimeMs = 0)

      events.shouldBeEmpty()
    }

    test("collectEvents should return events from single child") {
      val element = SequenceElement()
      element.children.add(VibrationElement(durationMs = 100, intensity = HapticIntensity.STRONG))

      val events = element.collectEvents(startTimeMs = 0)

      assertSoftly {
        events shouldHaveSize 1
        with(events.single()) {
          startTimeMs shouldBe 0
          durationMs shouldBe 100
          intensity shouldBe HapticIntensity.STRONG
        }
      }
    }

    test("collectEvents should execute children sequentially") {
      val element = SequenceElement()
      element.children.add(VibrationElement(durationMs = 100, intensity = HapticIntensity.STRONG))
      element.children.add(VibrationElement(durationMs = 50, intensity = HapticIntensity.MEDIUM))

      val events = element.collectEvents(startTimeMs = 0)

      assertSoftly {
        events shouldHaveSize 2
        with(events[0]) {
          startTimeMs shouldBe 0
          durationMs shouldBe 100
        }
        with(events[1]) {
          startTimeMs shouldBe 100
          durationMs shouldBe 50
        }
      }
    }

    test("collectEvents should handle DelayElement by advancing time only") {
      val element = SequenceElement()
      element.children.add(VibrationElement(durationMs = 100, intensity = HapticIntensity.STRONG))
      element.children.add(DelayElement(durationMs = 50))
      element.children.add(VibrationElement(durationMs = 200, intensity = HapticIntensity.MEDIUM))

      val events = element.collectEvents(startTimeMs = 0)

      assertSoftly {
        events shouldHaveSize 2
        with(events[0]) {
          startTimeMs shouldBe 0
          durationMs shouldBe 100
        }
        with(events[1]) {
          startTimeMs shouldBe 150
          durationMs shouldBe 200
        }
      }
    }

    test("collectEvents should preserve custom startTimeMs") {
      val element = SequenceElement()
      element.children.add(VibrationElement(durationMs = 100, intensity = HapticIntensity.STRONG))
      element.children.add(VibrationElement(durationMs = 50, intensity = HapticIntensity.MEDIUM))

      val events = element.collectEvents(startTimeMs = 500)

      assertSoftly {
        events shouldHaveSize 2
        events[0].startTimeMs shouldBe 500
        events[1].startTimeMs shouldBe 600
      }
    }

    test("collectEvents should preserve iOS parameters") {
      val iosParams = IosHapticParameters(sharpness = 0.9f)
      val element = SequenceElement()
      element.children.add(
        VibrationElement(
          durationMs = 100,
          intensity = HapticIntensity.STRONG,
          iosParameters = iosParams,
        ),
      )
      element.children.add(DelayElement(durationMs = 50))

      val events = element.collectEvents(startTimeMs = 0)

      events shouldHaveSize 1
      events[0].iosParameters?.sharpness shouldBe 0.9f
    }

    test("totalDurationMs should sum all children durations sequentially") {
      val element = SequenceElement()
      element.children.add(VibrationElement(durationMs = 100, intensity = HapticIntensity.STRONG))
      element.children.add(DelayElement(durationMs = 50))
      element.children.add(VibrationElement(durationMs = 200, intensity = HapticIntensity.MEDIUM))

      // 100 (haptic) + 50 (delay) + 200 (haptic) = 350
      element.totalDurationMs(startTimeMs = 0) shouldBe 350
    }

    test("totalDurationMs should return 0 for empty sequence") {
      val element = SequenceElement()

      element.totalDurationMs(startTimeMs = 0) shouldBe 0
    }
  })
