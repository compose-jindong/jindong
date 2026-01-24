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
import io.github.compose.jindong.core.model.HapticPattern
import io.github.compose.jindong.core.model.IosHapticParameters
import io.github.compose.jindong.core.model.ScheduledHapticEvent
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class PatternElementTest :
  FunSpec({
    test("collectEvents should return empty list when pattern has no events") {
      val emptyPattern = HapticPattern(events = emptyList(), rootElement = SequenceElement())
      val element = PatternElement(pattern = emptyPattern)

      val events = element.collectEvents(startTimeMs = 0)

      events.shouldBeEmpty()
    }

    test("collectEvents should return events from pattern with single event") {
      val event = ScheduledHapticEvent(
        startTimeMs = 0,
        durationMs = 100,
        intensity = HapticIntensity.STRONG,
      )
      val pattern = HapticPattern(events = listOf(event), rootElement = SequenceElement())
      val element = PatternElement(pattern = pattern)

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

    test("collectEvents should offset events by startTimeMs") {
      val event = ScheduledHapticEvent(
        startTimeMs = 50,
        durationMs = 100,
        intensity = HapticIntensity.MEDIUM,
      )
      val pattern = HapticPattern(events = listOf(event), rootElement = SequenceElement())
      val element = PatternElement(pattern = pattern)

      val events = element.collectEvents(startTimeMs = 200)

      assertSoftly {
        events shouldHaveSize 1
        // Original startTime (50) + offset (200) = 250
        events.single().startTimeMs shouldBe 250
      }
    }

    test("collectEvents should offset multiple events correctly") {
      val event1 = ScheduledHapticEvent(
        startTimeMs = 0,
        durationMs = 50,
        intensity = HapticIntensity.HIGH,
      )
      val event2 = ScheduledHapticEvent(
        startTimeMs = 100,
        durationMs = 75,
        intensity = HapticIntensity.LIGHT,
      )
      val pattern = HapticPattern(events = listOf(event1, event2), rootElement = SequenceElement())
      val element = PatternElement(pattern = pattern)

      val events = element.collectEvents(startTimeMs = 500)

      assertSoftly {
        events shouldHaveSize 2
        events[0].startTimeMs shouldBe 500
        events[1].startTimeMs shouldBe 600
      }
    }

    test("collectEvents should preserve all event properties") {
      val iosParams = IosHapticParameters(sharpness = 0.8f, sustained = true)
      val event = ScheduledHapticEvent(
        startTimeMs = 25,
        durationMs = 150,
        intensity = HapticIntensity.Custom(0.6f),
        iosParameters = iosParams,
      )
      val pattern = HapticPattern(events = listOf(event), rootElement = SequenceElement())
      val element = PatternElement(pattern = pattern)

      val events = element.collectEvents(startTimeMs = 100)

      assertSoftly {
        events shouldHaveSize 1
        with(events.single()) {
          startTimeMs shouldBe 125
          durationMs shouldBe 150
          intensity.value shouldBe 0.6f
          iosParameters?.sharpness shouldBe 0.8f
          iosParameters?.sustained shouldBe true
        }
      }
    }

    test("children should always be empty for PatternElement") {
      val pattern = HapticPattern(events = emptyList(), rootElement = SequenceElement())
      val element = PatternElement(pattern = pattern)

      element.children.shouldBeEmpty()
    }

    test("totalDurationMs should return 0 for empty pattern") {
      val emptyPattern = HapticPattern(events = emptyList(), rootElement = SequenceElement())
      val element = PatternElement(pattern = emptyPattern)

      element.totalDurationMs(startTimeMs = 0) shouldBe 0
    }

    test("totalDurationMs should return max end time of events") {
      val event1 = ScheduledHapticEvent(
        startTimeMs = 0,
        durationMs = 100,
        intensity = HapticIntensity.HIGH,
      )
      val event2 = ScheduledHapticEvent(
        startTimeMs = 50,
        durationMs = 200,
        intensity = HapticIntensity.MEDIUM,
      )
      val pattern = HapticPattern(events = listOf(event1, event2), rootElement = SequenceElement())
      val element = PatternElement(pattern = pattern)

      // Event1 ends at 100, Event2 ends at 250, so total duration should be 250
      element.totalDurationMs(startTimeMs = 0) shouldBe 250
    }

    test("totalDurationMs should handle non-sequential events") {
      // Events with gaps between them
      val event1 = ScheduledHapticEvent(
        startTimeMs = 0,
        durationMs = 50,
        intensity = HapticIntensity.HIGH,
      )
      val event2 = ScheduledHapticEvent(
        startTimeMs = 200,
        durationMs = 50,
        intensity = HapticIntensity.HIGH,
      )
      val pattern = HapticPattern(events = listOf(event1, event2), rootElement = SequenceElement())
      val element = PatternElement(pattern = pattern)

      // Event1 ends at 50, Event2 ends at 250
      element.totalDurationMs(startTimeMs = 0) shouldBe 250
    }

    test("should support pattern composition - include pattern within sequence") {
      val innerEvent = ScheduledHapticEvent(
        startTimeMs = 0,
        durationMs = 50,
        intensity = HapticIntensity.MEDIUM,
      )
      val innerPattern = HapticPattern(events = listOf(innerEvent), rootElement = SequenceElement())

      val sequence = SequenceElement()
      sequence.children.add(VibrationElement(durationMs = 100, intensity = HapticIntensity.HIGH))
      sequence.children.add(PatternElement(pattern = innerPattern))

      val events = sequence.collectEvents(startTimeMs = 0)

      assertSoftly {
        events shouldHaveSize 2
        events[0].startTimeMs shouldBe 0
        events[0].durationMs shouldBe 100
        events[1].startTimeMs shouldBe 100 // After the first vibration
        events[1].durationMs shouldBe 50
      }
    }
  })
