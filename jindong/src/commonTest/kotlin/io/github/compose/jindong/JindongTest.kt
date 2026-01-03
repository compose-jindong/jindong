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
package io.github.compose.jindong

import io.github.compose.jindong.dsl.Haptic
import io.github.compose.jindong.model.HapticIntensity
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.start
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class JindongTest :
  FunSpec({
    test("single Haptic produces one event starting at 0ms") {
      val pattern = compilePattern {
        Haptic(duration = 100.ms)
      }

      pattern.events shouldHaveSize 1
      assertSoftly(pattern.events[0]) {
        startTimeMs shouldBe 0
        durationMs shouldBe 100
      }
    }

    test("multiple Haptics execute sequentially without overlap") {
      val pattern = compilePattern {
        Haptic(duration = 50.ms)
        Haptic(duration = 100.ms)
        Haptic(duration = 75.ms)
      }

      pattern.events shouldHaveSize 3

      // First haptic starts at 0ms
      assertSoftly(pattern.events[0]) {
        startTimeMs shouldBe 0
        durationMs shouldBe 50
      }

      // Second haptic starts at 50ms (after first ends)
      assertSoftly(pattern.events[1]) {
        startTimeMs shouldBe 50
        durationMs shouldBe 100
      }

      // Third haptic starts at 150ms (after second ends)
      assertSoftly(pattern.events[2]) {
        startTimeMs shouldBe 150
        durationMs shouldBe 75
      }
    }

    test("empty block produces no events") {
      val pattern = compilePattern { }

      pattern.events.shouldBeEmpty()
    }

    test("each Haptic preserves its own intensity") {
      val pattern = compilePattern {
        Haptic(duration = 50.ms, intensity = HapticIntensity.LIGHT)
        Haptic(duration = 50.ms, intensity = HapticIntensity.STRONG)
      }

      pattern.events[0].intensity shouldBe HapticIntensity.LIGHT
      pattern.events[1].intensity shouldBe HapticIntensity.STRONG
    }
  })
