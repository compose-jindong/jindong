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
package io.github.compose.jindong.core.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.float
import io.kotest.property.checkAll

private fun factors(): Arb<Float> = Arb.float(0f..4f)

class ScaleIntensityTest :
  FunSpec({
    test("scaled intensity always stays within [0, 1]") {
      checkAll(patterns(), factors()) { pattern, factor ->
        pattern.scaleIntensity(factor).events.forEach { event ->
          event.intensity.value shouldBeGreaterThanOrEqual 0f
          event.intensity.value shouldBeLessThanOrEqual 1f
        }
      }
    }

    test("scaling by 1 preserves every intensity value") {
      checkAll(patterns()) { pattern ->
        val scaled = pattern.scaleIntensity(1f)
        scaled.events.zip(pattern.events).forEach { (s, p) ->
          s.intensity.value shouldBe (p.intensity.value plusOrMinus 1e-6f)
        }
      }
    }

    test("scaling leaves timing and iOS parameters untouched") {
      checkAll(patterns(), factors()) { pattern, factor ->
        val scaled = pattern.scaleIntensity(factor)
        scaled.events.zip(pattern.events).forEach { (s, p) ->
          s.startTimeMs shouldBe p.startTimeMs
          s.durationMs shouldBe p.durationMs
          s.iosParameters shouldBe p.iosParameters
        }
      }
    }
  })
