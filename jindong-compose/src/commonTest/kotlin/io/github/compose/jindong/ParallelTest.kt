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

import io.github.compose.jindong.core.dsl.buildHapticPattern
import io.github.compose.jindong.core.dsl.hapticCurve
import io.github.compose.jindong.core.ms
import io.github.compose.jindong.dsl.Clip
import io.github.compose.jindong.dsl.Continuous
import io.github.compose.jindong.dsl.Delay
import io.github.compose.jindong.dsl.Haptic
import io.github.compose.jindong.dsl.Parallel
import io.github.compose.jindong.dsl.Repeat
import io.github.compose.jindong.dsl.Sequence
import io.github.compose.jindong.dsl.Transient
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ParallelTest :
  FunSpec({
    test("Parallel preserves independent curves and zero-duration transient branches in both DSLs") {
      val rise = hapticCurve {
        point(0.ms, 0f)
        point(100.ms, 1f)
        point(300.ms, 0f)
      }
      val fall = hapticCurve {
        point(0.ms, 1f)
        point(300.ms, 0f)
      }
      val composed = compilePattern {
        Parallel {
          Continuous(300.ms, intensityCurve = rise, sharpness = 0.2f)
          Continuous(300.ms, intensityCurve = fall, sharpness = 0.8f)
          Sequence {
            Transient()
            Delay(80.ms)
            Transient()
          }
        }
        Delay(100.ms)
      }
      composed shouldBe buildHapticPattern {
        parallel {
          continuous(300.ms, intensityCurve = rise, sharpness = 0.2f)
          continuous(300.ms, intensityCurve = fall, sharpness = 0.8f)
          sequence {
            transient()
            delay(80.ms)
            transient()
          }
        }
        delay(100.ms)
      }
      composed.durationMs shouldBe 400L
      composed.events.map { it.startTimeMs } shouldBe listOf(0L, 0L, 0L, 80L)
      compilePattern {
        Parallel {
          Transient()
          Transient()
        }
      }.durationMs shouldBe 0L
    }

    test("Compose Parallel matches nested core branches including Clip and silence") {
      val clip = buildHapticPattern {
        haptic(50.ms)
        delay(100.ms)
      }
      val composed = compilePattern {
        Delay(30.ms)
        Repeat(2) {
          Parallel {
            Clip(clip)
            Sequence {
              Delay(20.ms)
              Haptic(30.ms)
            }
            Parallel {
              Delay(100.ms)
              Haptic(10.ms)
            }
          }
        }
      }
      composed shouldBe buildHapticPattern {
        delay(30.ms)
        repeat(2) {
          parallel {
            clip(clip)
            sequence {
              delay(20.ms)
              haptic(30.ms)
            }
            parallel {
              delay(100.ms)
              haptic(10.ms)
            }
          }
        }
      }
      composed.durationMs shouldBe 330L
    }

    test("Compose empty and silent Parallel match the core builder") {
      compilePattern { Parallel {} } shouldBe buildHapticPattern { parallel {} }
      compilePattern {
        Parallel {
          Sequence {}
          Delay(100.ms)
          Delay(150.ms)
        }
      } shouldBe buildHapticPattern {
        parallel {
          sequence {}
          delay(100.ms)
          delay(150.ms)
        }
      }
    }
  })
