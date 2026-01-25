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
import io.kotest.matchers.shouldBe

class HapticIntensityTest :
  FunSpec({
    context("Pre-defined levels") {
      test("LIGHT should have value 0.25") {
        HapticIntensity.LIGHT.value shouldBe 0.25f
      }
      test("MEDIUM should have value 0.5") {
        HapticIntensity.MEDIUM.value shouldBe 0.5f
      }
      test("STRONG should have value 0.75") {
        HapticIntensity.STRONG.value shouldBe 0.75f
      }
      test("HIGH should have value 1.0") {
        HapticIntensity.HIGH.value shouldBe 1.0f
      }
    }

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

      test("should preserve valid values") {
        HapticIntensity.Custom(0.0f).value shouldBe 0.0f
        HapticIntensity.Custom(0.5f).value shouldBe 0.5f
        HapticIntensity.Custom(1.0f).value shouldBe 1.0f
        HapticIntensity.Custom(0.33f).value shouldBe 0.33f
      }
    }
  })
