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
import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.model.reversed
import io.github.compose.jindong.core.ms
import io.github.compose.jindong.dsl.Clip
import io.github.compose.jindong.dsl.Continuous
import io.github.compose.jindong.dsl.Delay
import io.github.compose.jindong.dsl.Repeat
import io.github.compose.jindong.dsl.Transient
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RichHapticPatternTest :
  FunSpec({
    test("Compose rich feedback and nested Clip compile to the same common model as core") {
      val rise = hapticCurve {
        point(0.ms, 0f)
        point(80.ms, 0.8f)
        point(300.ms, 0f)
      }
      val texture = hapticCurve {
        point(0.ms, 0.2f)
        point(300.ms, 0.8f)
      }
      val clip = buildHapticPattern {
        continuous(300.ms, sharpness = 0.6f, intensityCurve = rise, sharpnessCurve = texture)
        delay(20.ms)
      }.reversed()
      val compiled = compilePattern {
        Repeat(3) {
          Transient(HapticIntensity.STRONG, sharpness = 0.9f)
          Delay(80.ms)
        }
        Continuous(300.ms, sharpness = 0.6f, intensityCurve = rise, sharpnessCurve = texture)
        Clip(clip)
      }
      compiled shouldBe buildHapticPattern {
        repeat(3) {
          transient(HapticIntensity.STRONG, sharpness = 0.9f)
          delay(80.ms)
        }
        continuous(300.ms, sharpness = 0.6f, intensityCurve = rise, sharpnessCurve = texture)
        clip(clip)
      }
      compiled.durationMs shouldBe 860L
    }

    test("Compose preserves equal-time priority and rejects invalid rich values") {
      compilePattern {
        Transient(HapticIntensity.HIGH)
        Transient(HapticIntensity.LIGHT)
      }
        .events.map { it.intensity } shouldBe listOf(HapticIntensity.HIGH, HapticIntensity.LIGHT)
      shouldThrow<IllegalArgumentException> { compilePattern { Continuous(0.ms) } }
      shouldThrow<IllegalArgumentException> { compilePattern { Transient(sharpness = Float.NaN) } }
    }
  })
