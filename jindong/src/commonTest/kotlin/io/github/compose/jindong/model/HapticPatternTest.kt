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
package io.github.compose.jindong.model

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.collections.get

class HapticPatternTest :
  FunSpec({
    test("should create empty pattern") {
      val pattern = HapticPattern.Empty

      pattern.events.shouldBeEmpty()
    }

    test("should preserve events list") {
      val events = listOf(
        ScheduledHapticEvent(
          startTimeMs = 0,
          durationMs = 100,
          intensity = HapticIntensity.STRONG,
        ),
        ScheduledHapticEvent(
          startTimeMs = 150,
          durationMs = 200,
          intensity = HapticIntensity.HIGH,
        ),
      )

      val pattern = HapticPattern(events)

      assertSoftly {
        pattern.events shouldHaveSize 2
        with(pattern.events[0]) {
          startTimeMs shouldBe 0
          durationMs shouldBe 100
          intensity shouldBe HapticIntensity.STRONG
        }
        with(pattern.events[1]) {
          startTimeMs shouldBe 150
          durationMs shouldBe 200
          intensity shouldBe HapticIntensity.HIGH
        }
      }
    }
  })
