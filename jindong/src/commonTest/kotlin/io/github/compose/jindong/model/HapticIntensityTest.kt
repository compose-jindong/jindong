package io.github.compose.jindong.model

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
    }
  })
