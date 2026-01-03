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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class HapticIntensityTest :
  FunSpec({
    context("Custom intensity") {
      test("should coerce values above 1.0 to 1.0") {
        HapticIntensity.Custom(1.5f).value shouldBe 1.0f
        HapticIntensity.Custom(2.0f).value shouldBe 1.0f
        HapticIntensity.Custom(100f).value shouldBe 1.0f
      }

      test("should coerce values below 0.0 to 0.0") {
        HapticIntensity.Custom(-0.1f).value shouldBe 0.0f
        HapticIntensity.Custom(-1.0f).value shouldBe 0.0f
        HapticIntensity.Custom(-100f).value shouldBe 0.0f
      }

      test("should support equality comparison") {
        HapticIntensity.Custom(0.5f) shouldBe HapticIntensity.Custom(0.5f)
      }

      test("should have different instances for different values") {
        val custom1 = HapticIntensity.Custom(0.3f)
        val custom2 = HapticIntensity.Custom(0.7f)
        (custom1 == custom2) shouldBe false
      }
    }
  })
