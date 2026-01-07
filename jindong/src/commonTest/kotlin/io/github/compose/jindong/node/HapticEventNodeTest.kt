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
package io.github.compose.jindong.node

import io.github.compose.jindong.model.HapticIntensity
import io.github.compose.jindong.model.IosHapticParameters
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class HapticEventNodeTest :
  FunSpec({
    test("collectEvents should return single event with given startTimeMs") {
      val node = HapticEventNode(durationMs = 100, intensity = HapticIntensity.STRONG)

      val events = node.collectEvents(startTimeMs = 50)

      events shouldHaveSize 1
      assertSoftly(events.single()) {
        startTimeMs shouldBe 50
        durationMs shouldBe 100
        intensity shouldBe HapticIntensity.STRONG
      }
    }

    test("children should always be empty for leaf node") {
      val node = HapticEventNode(durationMs = 100, intensity = HapticIntensity.MEDIUM)

      node.children.shouldBeEmpty()
    }

    test("collectEvents should preserve intensity and duration values") {
      val iosParams = IosHapticParameters(sharpness = 0.7f)
      val node = HapticEventNode(
        durationMs = 250,
        intensity = HapticIntensity.LIGHT,
        iosParameters = iosParams,
      )

      val events = node.collectEvents(startTimeMs = 0)

      events shouldHaveSize 1
      assertSoftly(events.single()) {
        durationMs shouldBe 250
        intensity shouldBe HapticIntensity.LIGHT
        iosParameters?.sharpness shouldBe 0.7f
      }
    }

    test("collectEvents should include iOS parameters when provided") {
      val iosParams = IosHapticParameters(sustained = true)
      val node = HapticEventNode(
        durationMs = 100,
        intensity = HapticIntensity.HIGH,
        iosParameters = iosParams,
      )

      val events = node.collectEvents(startTimeMs = 0)

      events.single().iosParameters shouldBe iosParams
    }

    test("totalDurationMs should return durationMs") {
      val node = HapticEventNode(durationMs = 250, intensity = HapticIntensity.MEDIUM)

      node.totalDurationMs(startTimeMs = 0) shouldBe 250
    }

    test("totalDurationMs should be independent of startTimeMs") {
      val node = HapticEventNode(durationMs = 150, intensity = HapticIntensity.STRONG)

      assertSoftly {
        node.totalDurationMs(startTimeMs = 0) shouldBe 150
        node.totalDurationMs(startTimeMs = 100) shouldBe 150
        node.totalDurationMs(startTimeMs = 500) shouldBe 150
      }
    }
  })
