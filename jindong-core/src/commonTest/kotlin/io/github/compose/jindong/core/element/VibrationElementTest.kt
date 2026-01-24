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

class VibrationElementTest :
  FunSpec({
    test("collectEvents should return single event with given startTimeMs") {
      val element = VibrationElement(durationMs = 100, intensity = HapticIntensity.STRONG)

      val events = element.collectEvents(startTimeMs = 50)

      events shouldHaveSize 1
      assertSoftly(events.single()) {
        startTimeMs shouldBe 50
        durationMs shouldBe 100
        intensity shouldBe HapticIntensity.STRONG
      }
    }

    test("children should always be empty for leaf element") {
      val element = VibrationElement(durationMs = 100, intensity = HapticIntensity.MEDIUM)

      element.children.shouldBeEmpty()
    }

    test("collectEvents should preserve intensity and duration values") {
      val iosParams = IosHapticParameters(sharpness = 0.7f)
      val element = VibrationElement(
        durationMs = 250,
        intensity = HapticIntensity.LIGHT,
        iosParameters = iosParams,
      )

      val events = element.collectEvents(startTimeMs = 0)

      events shouldHaveSize 1
      assertSoftly(events.single()) {
        durationMs shouldBe 250
        intensity shouldBe HapticIntensity.LIGHT
        iosParameters?.sharpness shouldBe 0.7f
      }
    }

    test("collectEvents should include iOS parameters when provided") {
      val iosParams = IosHapticParameters(sustained = true)
      val element = VibrationElement(
        durationMs = 100,
        intensity = HapticIntensity.HIGH,
        iosParameters = iosParams,
      )

      val events = element.collectEvents(startTimeMs = 0)

      events.single().iosParameters shouldBe iosParams
    }

    test("totalDurationMs should return durationMs") {
      val element = VibrationElement(durationMs = 250, intensity = HapticIntensity.MEDIUM)

      element.totalDurationMs(startTimeMs = 0) shouldBe 250
    }

    test("totalDurationMs should be independent of startTimeMs") {
      val element = VibrationElement(durationMs = 150, intensity = HapticIntensity.STRONG)

      assertSoftly {
        element.totalDurationMs(startTimeMs = 0) shouldBe 150
        element.totalDurationMs(startTimeMs = 100) shouldBe 150
        element.totalDurationMs(startTimeMs = 500) shouldBe 150
      }
    }

    test("should throw IllegalArgumentException when durationMs is negative") {
      val exception = shouldThrow<IllegalArgumentException> {
        VibrationElement(
          durationMs = -100,
          intensity = HapticIntensity.LIGHT,
        )
      }
      exception.message shouldContain "durationMs must be non-negative"
    }
  })
