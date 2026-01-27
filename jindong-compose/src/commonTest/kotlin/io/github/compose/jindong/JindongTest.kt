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

import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.ms
import io.github.compose.jindong.dsl.Delay
import io.github.compose.jindong.dsl.Haptic
import io.github.compose.jindong.dsl.Repeat
import io.github.compose.jindong.dsl.RepeatWithIndex
import io.github.compose.jindong.dsl.Sequence
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class JindongTest :
  FunSpec({
    context("Haptic") {
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
    }

    context("Delay") {
      test("Delay between haptic events") {
        val pattern = compilePattern {
          Haptic(duration = 50.ms)
          Delay(duration = 100.ms)
          Haptic(duration = 50.ms)
        }

        pattern.events shouldHaveSize 2

        // First haptic starts at 0ms
        assertSoftly(pattern.events[0]) {
          startTimeMs shouldBe 0
          durationMs shouldBe 50
        }

        // Second haptic starts at 150ms (50ms haptic + 100ms delay)
        assertSoftly(pattern.events[1]) {
          startTimeMs shouldBe 150
          durationMs shouldBe 50
        }
      }

      test("Delay at start of pattern") {
        val pattern = compilePattern {
          Delay(duration = 100.ms)
          Haptic(duration = 50.ms)
        }

        pattern.events shouldHaveSize 1
        pattern.events[0].startTimeMs shouldBe 100
      }
    }

    context("Repeat") {
      test("simple repeat") {
        val pattern = compilePattern {
          Repeat(count = 3) {
            Haptic(duration = 50.ms)
          }
        }

        pattern.events shouldHaveSize 3

        // Events at 0ms, 50ms, 100ms
        pattern.events[0].startTimeMs shouldBe 0
        pattern.events[1].startTimeMs shouldBe 50
        pattern.events[2].startTimeMs shouldBe 100
      }

      test("repeat with delay after haptic") {
        val pattern = compilePattern {
          Repeat(count = 2) {
            Haptic(duration = 50.ms)
            Delay(duration = 50.ms)
          }
        }

        pattern.events shouldHaveSize 2

        // Events at 0ms and 100ms (50ms haptic + 50ms delay per iteration)
        pattern.events[0].startTimeMs shouldBe 0
        pattern.events[1].startTimeMs shouldBe 100
      }

      test("repeat with delay before haptic") {
        val pattern = compilePattern {
          Repeat(count = 2) {
            Delay(duration = 50.ms)
            Haptic(duration = 50.ms)
          }
        }

        pattern.events shouldHaveSize 2

        // First iteration: delay 50ms, then haptic at 50ms
        // Second iteration: delay 50ms (starting at 100ms), then haptic at 150ms
        pattern.events[0].startTimeMs shouldBe 50
        pattern.events[1].startTimeMs shouldBe 150
      }

      test("repeat with multiple delays") {
        val pattern = compilePattern {
          Repeat(count = 2) {
            Delay(duration = 25.ms)
            Haptic(duration = 50.ms)
            Delay(duration = 25.ms)
          }
        }

        pattern.events shouldHaveSize 2

        // First iteration: delay 25ms, haptic at 25ms (ends at 75ms), delay 25ms (ends at 100ms)
        // Second iteration: delay 25ms (ends at 125ms), haptic at 125ms
        pattern.events[0].startTimeMs shouldBe 25
        pattern.events[1].startTimeMs shouldBe 125
      }

      test("zero count repeat produces no events") {
        val pattern = compilePattern {
          Repeat(count = 0) {
            Haptic(duration = 50.ms)
          }
        }

        pattern.events.shouldBeEmpty()
      }
    }

    context("Sequence") {
      test("explicit sequence grouping") {
        val pattern = compilePattern {
          Sequence {
            Haptic(duration = 50.ms)
            Haptic(duration = 100.ms)
          }
        }

        pattern.events shouldHaveSize 2

        // Events at 0ms and 50ms
        pattern.events[0].startTimeMs shouldBe 0
        pattern.events[1].startTimeMs shouldBe 50
      }

      test("nested sequence in repeat") {
        val pattern = compilePattern {
          Repeat(count = 2) {
            Sequence {
              Haptic(duration = 50.ms)
              Haptic(duration = 50.ms)
            }
          }
        }

        pattern.events shouldHaveSize 4

        // First iteration: events at 0ms, 50ms
        // Second iteration: events at 100ms, 150ms
        pattern.events[0].startTimeMs shouldBe 0
        pattern.events[1].startTimeMs shouldBe 50
        pattern.events[2].startTimeMs shouldBe 100
        pattern.events[3].startTimeMs shouldBe 150
      }

      context("RepeatWithIndex") {
        test("index provides 0-based incrementing values") {
          val capturedIndices = mutableListOf<Int>()
          val pattern = compilePattern {
            RepeatWithIndex(count = 5) { index ->
              capturedIndices.add(index)
              Haptic(duration = 50.ms)
            }
          }

          capturedIndices shouldBe listOf(0, 1, 2, 3, 4)
          pattern.events shouldHaveSize 5
        }

        test("nested sequence with delay - timing propagation") {
          val pattern = compilePattern {
            RepeatWithIndex(count = 2) { index ->
              Sequence {
                Haptic(duration = 50.ms)
                Delay(duration = 50.ms)
              }
            }
          }

          pattern.events shouldHaveSize 2

          // Iteration 0: Haptic at 0ms, Delay until 100ms
          // Iteration 1: Haptic at 100ms, Delay until 200ms
          pattern.events[0].startTimeMs shouldBe 0
          pattern.events[1].startTimeMs shouldBe 100
        }

        test("index can determine duration dynamically") {
          val pattern = compilePattern {
            RepeatWithIndex(count = 3) { index ->
              Haptic(duration = (50 + index * 50).ms)
            }
          }

          pattern.events shouldHaveSize 3

          // Durations: 50ms, 100ms, 150ms
          assertSoftly {
            pattern.events[0].durationMs shouldBe 50
            pattern.events[1].durationMs shouldBe 100
            pattern.events[2].durationMs shouldBe 150
          }

          // Start times: 0ms, 50ms, 150ms
          assertSoftly {
            pattern.events[0].startTimeMs shouldBe 0
            pattern.events[1].startTimeMs shouldBe 50
            pattern.events[2].startTimeMs shouldBe 150
          }
        }

        test("zero count produces no events") {
          val pattern = compilePattern {
            RepeatWithIndex(count = 0) { index ->
              Haptic(duration = 50.ms)
            }
          }

          pattern.events.shouldBeEmpty()
        }
      }
    }
  })
