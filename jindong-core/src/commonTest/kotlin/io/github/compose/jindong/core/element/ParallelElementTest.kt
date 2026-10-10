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

import io.github.compose.jindong.core.dsl.buildHapticPattern
import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.ms
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ParallelElementTest :
  FunSpec({
    test("branches share their start and use the longest duration") {
      val pattern = buildHapticPattern {
        delay(30.ms)
        parallel {
          haptic(100.ms)
          haptic(150.ms)
        }
        haptic(20.ms)
      }
      pattern.events.map { it.startTimeMs } shouldBe listOf(30L, 30L, 180L)
      pattern.durationMs shouldBe 200L
    }

    test("nested sequences preserve trailing silence and branch input order") {
      val clip = buildHapticPattern {
        haptic(50.ms)
        delay(100.ms)
      }
      val pattern = buildHapticPattern {
        repeat(2) {
          parallel {
            clip(clip)
            sequence {
              delay(20.ms)
              haptic(30.ms, HapticIntensity.HIGH)
            }
            parallel {
              delay(100.ms)
              haptic(10.ms)
            }
          }
        }
      }
      pattern.events.map { it.startTimeMs } shouldBe listOf(0L, 20L, 0L, 150L, 170L, 150L)
      pattern.durationMs shouldBe 300L
    }

    test("empty and silent branches retain silence without allocating events") {
      buildHapticPattern { parallel {} }.durationMs shouldBe 0L
      val pattern = buildHapticPattern {
        parallel {
          sequence {}
          delay(150.ms)
          delay(100.ms)
        }
      }
      pattern.events shouldBe emptyList()
      pattern.durationMs shouldBe 150L
    }

    test("parallel rejects overflow and expansion before generating repeated events") {
      val parallel = ParallelElement()
      parallel.children += DelayElement(Long.MAX_VALUE)
      shouldThrow<IllegalArgumentException> { parallel.collectEvents(1L) }
      shouldThrow<IllegalArgumentException> {
        buildHapticPattern {
          repeat(10_000) {
            parallel {
              haptic(1.ms)
              haptic(1.ms)
            }
          }
        }
      }
    }
  })
