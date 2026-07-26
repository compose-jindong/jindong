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

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe

/**
 * PoC migration of the former Kotest `HapticIntensityTest` (FunSpec) to TestBalloon.
 *
 * The data-driven cases previously sat as several `shouldBe` calls inside one test body, where the
 * first failing input aborts the rest and the test name says nothing about which input broke. Each
 * input is now its own test: a plain Kotlin `for` loop over `test(...)` is all TestBalloon needs for
 * parameterization. Assertions stay on kotest-assertions-core (`shouldBe`), so only the spec engine
 * changed.
 */
val HapticIntensityTest by testSuite {
  testSuite("Pre-defined levels") {
    val levels = listOf(
      Triple("LIGHT", HapticIntensity.LIGHT, 0.25f),
      Triple("MEDIUM", HapticIntensity.MEDIUM, 0.5f),
      Triple("STRONG", HapticIntensity.STRONG, 0.75f),
      Triple("HIGH", HapticIntensity.HIGH, 1.0f),
    )
    for ((name, level, expected) in levels) {
      test("$name should have value $expected") {
        level.value shouldBe expected
      }
    }
  }

  testSuite("Custom intensity") {
    for (input in listOf(1.5f, 2.0f, 100f)) {
      test("should coerce $input above 1.0 to 1.0") {
        HapticIntensity.Custom(input).value shouldBe 1.0f
      }
    }
    for (input in listOf(-0.1f, -1.0f, -100f)) {
      test("should coerce $input below 0.0 to 0.0") {
        HapticIntensity.Custom(input).value shouldBe 0.0f
      }
    }
    for (input in listOf(0.0f, 0.5f, 1.0f, 0.33f)) {
      test("should preserve valid value $input") {
        HapticIntensity.Custom(input).value shouldBe input
      }
    }
  }
}
