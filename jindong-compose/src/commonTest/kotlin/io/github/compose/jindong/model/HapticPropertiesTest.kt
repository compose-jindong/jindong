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
package io.github.compose.jindong.model

import io.github.compose.jindong.ms
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class HapticPropertiesTest :
  FunSpec({
    context("sharpness") {
      test("creates properties with sharpness") {
        val properties = HapticProperties.sharpness(0.9f)
        properties.sharpness shouldBe 0.9f
        properties.attackTime shouldBe null
      }
    }

    context("envelope") {
      test("creates properties with envelope") {
        val properties = HapticProperties.envelope(
          attack = 50.ms,
          decay = 30.ms,
          release = 20.ms,
        )
        properties.sharpness shouldBe null
        properties.attackTime shouldBe 50.ms
        properties.decayTime shouldBe 30.ms
        properties.releaseTime shouldBe 20.ms
      }

      test("envelope should have non-negative times") {
        shouldThrow<IllegalArgumentException> {
          HapticProperties.envelope(attack = (-50).ms)
        }
      }
    }

    context("chaining") {
      test("sharpness then envelope") {
        val properties = HapticProperties
          .sharpness(0.9f)
          .envelope(attack = 50.ms)

        properties.sharpness shouldBe 0.9f
        properties.attackTime shouldBe 50.ms
      }

      test("envelope then sharpness") {
        val properties = HapticProperties
          .envelope(attack = 50.ms)
          .sharpness(0.9f)

        properties.sharpness shouldBe 0.9f
        properties.attackTime shouldBe 50.ms
      }
    }

    context("toIosHapticParameters") {
      test("converts with defaults") {
        val properties = HapticProperties.sharpness(0.8f)
        val iosParams = properties.toIosHapticParameters()

        iosParams.sharpness shouldBe 0.8f
        iosParams.attackTime shouldBe null
      }

      test("uses default sharpness when null") {
        val properties = HapticProperties.envelope(attack = 50.ms)
        val iosParams = properties.toIosHapticParameters()

        iosParams.sharpness shouldBe 0.5f
        iosParams.attackTime shouldBe 50.ms
      }
    }
  })
