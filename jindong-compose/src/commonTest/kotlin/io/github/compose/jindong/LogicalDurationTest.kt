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
import io.github.compose.jindong.core.model.reversed
import io.github.compose.jindong.core.ms
import io.github.compose.jindong.core.toHapticMilliseconds
import io.github.compose.jindong.dsl.Clip
import io.github.compose.jindong.dsl.Delay
import io.github.compose.jindong.dsl.Haptic
import io.github.compose.jindong.dsl.Repeat
import io.github.compose.jindong.dsl.RepeatWithIndex
import io.github.compose.jindong.dsl.Sequence
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds

class LogicalDurationTest :
  FunSpec({
    test("Compose Clip preserves trailing silence and transformed values") {
      val tick = buildHapticPattern {
        haptic(50.ms)
        delay(50.ms)
      }
      val pattern = compilePattern {
        Clip(tick)
        Clip(tick.reversed())
      }
      pattern.events.map { it.startTimeMs } shouldBe listOf(0L, 150L)
      pattern.durationMs shouldBe 200L
    }

    test("Compose delay-only and nested repetitions match the core builder") {
      compilePattern { Delay(100.ms) } shouldBe buildHapticPattern { delay(100.ms) }
      val compiled = compilePattern {
        Repeat(2) {
          Sequence {
            Haptic(50.ms)
            Delay(50.ms)
          }
        }
        Delay(30.ms)
      }
      compiled shouldBe buildHapticPattern {
        repeat(2) {
          sequence {
            haptic(50.ms)
            delay(50.ms)
          }
        }
        delay(30.ms)
      }
      compiled.durationMs shouldBe 230L
    }

    test("Compose indexed repeat rejects excessive counts before invoking content") {
      listOf(-1, 10_001, Int.MAX_VALUE).forEach { count ->
        var invoked = false
        shouldThrow<IllegalArgumentException> {
          compilePattern {
            RepeatWithIndex(count) {
              invoked = true
              error("must reject before invoking content")
            }
          }
        }
        invoked shouldBe false
      }
    }

    test("Compose Duration conversion shares validation and rounding with the core DSL") {
      val compiled = compilePattern {
        Haptic(500.microseconds)
        Delay(1500.microseconds)
      }
      compiled.events.single().durationMs shouldBe 500.microseconds.toHapticMilliseconds()
      compiled.durationMs shouldBe 3L
      shouldThrow<IllegalArgumentException> { compilePattern { Haptic(499.microseconds) } }
      shouldThrow<IllegalArgumentException> { compilePattern { Delay(Duration.INFINITE) } }
    }
  })
