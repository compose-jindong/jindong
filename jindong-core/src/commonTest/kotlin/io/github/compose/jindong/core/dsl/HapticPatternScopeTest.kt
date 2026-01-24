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

import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.model.IosHapticParameters
import io.github.compose.jindong.core.ms
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class HapticPatternScopeTest :
  FunSpec({
    test("buildHapticPattern should create empty pattern when no elements added") {
      val pattern = buildHapticPattern { }

      pattern.events.shouldBeEmpty()
    }

    test("buildHapticPattern should create pattern with single haptic") {
      val pattern = buildHapticPattern {
        haptic(100.ms)
      }

      assertSoftly {
        pattern.events shouldHaveSize 1
        with(pattern.events.single()) {
          startTimeMs shouldBe 0
          durationMs shouldBe 100
          intensity shouldBe HapticIntensity.MEDIUM // default
        }
      }
    }

    test("buildHapticPattern should create pattern with custom intensity") {
      val pattern = buildHapticPattern {
        haptic(50.ms, HapticIntensity.STRONG)
      }

      assertSoftly {
        pattern.events shouldHaveSize 1
        pattern.events.single().intensity shouldBe HapticIntensity.STRONG
      }
    }

    test("buildHapticPattern should handle delay between haptics") {
      val pattern = buildHapticPattern {
        haptic(100.ms)
        delay(50.ms)
        haptic(100.ms)
      }

      assertSoftly {
        pattern.events shouldHaveSize 2
        pattern.events[0].startTimeMs shouldBe 0
        pattern.events[1].startTimeMs shouldBe 150 // 100 + 50 delay
      }
    }

    test("buildHapticPattern should handle repeat block") {
      val pattern = buildHapticPattern {
        repeat(3) {
          haptic(50.ms)
        }
      }

      assertSoftly {
        pattern.events shouldHaveSize 3
        pattern.events[0].startTimeMs shouldBe 0
        pattern.events[1].startTimeMs shouldBe 50
        pattern.events[2].startTimeMs shouldBe 100
      }
    }

    test("buildHapticPattern should handle repeat with delay") {
      val pattern = buildHapticPattern {
        repeat(2) {
          haptic(50.ms)
          delay(25.ms)
        }
      }

      assertSoftly {
        pattern.events shouldHaveSize 2
        pattern.events[0].startTimeMs shouldBe 0
        pattern.events[1].startTimeMs shouldBe 75 // 50 + 25 delay
      }
    }

    test("buildHapticPattern should handle repeatWithIndex") {
      val pattern = buildHapticPattern {
        repeatWithIndex(3) { index ->
          val intensity = HapticIntensity.Custom(1.0f - (index * 0.25f))
          haptic(50.ms, intensity)
        }
      }

      assertSoftly {
        pattern.events shouldHaveSize 3
        pattern.events[0].intensity.value shouldBe 1.0f
        pattern.events[1].intensity.value shouldBe 0.75f
        pattern.events[2].intensity.value shouldBe 0.5f
      }
    }

    test("buildHapticPattern should handle sequence block") {
      val pattern = buildHapticPattern {
        sequence {
          haptic(100.ms)
          haptic(50.ms)
        }
      }

      assertSoftly {
        pattern.events shouldHaveSize 2
        pattern.events[0].startTimeMs shouldBe 0
        pattern.events[1].startTimeMs shouldBe 100
      }
    }

    test("buildHapticPattern should handle nested structures") {
      val pattern = buildHapticPattern {
        haptic(50.ms)
        repeat(2) {
          delay(25.ms)
          haptic(30.ms)
        }
      }

      assertSoftly {
        pattern.events shouldHaveSize 3
        pattern.events[0].startTimeMs shouldBe 0
        pattern.events[0].durationMs shouldBe 50
        pattern.events[1].startTimeMs shouldBe 75 // 50 + 25 delay
        pattern.events[1].durationMs shouldBe 30
        pattern.events[2].startTimeMs shouldBe 130 // 75 + 30 + 25 delay
        pattern.events[2].durationMs shouldBe 30
      }
    }

    test("buildHapticPattern should handle include") {
      val clickPattern = buildHapticPattern {
        haptic(50.ms, HapticIntensity.MEDIUM)
      }

      val composedPattern = buildHapticPattern {
        include(clickPattern)
        delay(100.ms)
        include(clickPattern)
      }

      assertSoftly {
        composedPattern.events shouldHaveSize 2
        composedPattern.events[0].startTimeMs shouldBe 0
        composedPattern.events[1].startTimeMs shouldBe 150 // 50 + 100 delay
      }
    }

    test("buildHapticPattern should preserve iOS parameters") {
      val iosParams = IosHapticParameters(sharpness = 0.8f, sustained = true)
      val pattern = buildHapticPattern {
        haptic(100.ms, HapticIntensity.STRONG, iosParams)
      }

      assertSoftly {
        pattern.events shouldHaveSize 1
        pattern.events.single().iosParameters shouldBe iosParams
      }
    }

    test("buildHapticPattern complex example from documentation") {
      val pattern = buildHapticPattern {
        haptic(100.ms)
        delay(50.ms)
        repeat(2) {
          haptic(30.ms, HapticIntensity.STRONG)
        }
      }

      assertSoftly {
        pattern.events shouldHaveSize 3
        pattern.events[0].startTimeMs shouldBe 0
        pattern.events[0].durationMs shouldBe 100
        pattern.events[1].startTimeMs shouldBe 150
        pattern.events[1].durationMs shouldBe 30
        pattern.events[2].startTimeMs shouldBe 180
        pattern.events[2].durationMs shouldBe 30
      }
    }
  })
