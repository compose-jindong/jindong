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
package io.github.jindong.node

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class RepeatNodeTest :
  FunSpec({
    test("collectEvents should return empty list when no children") {
      val node = RepeatNode(count = 3)

      val events = node.collectEvents(startTimeMs = 0)

      events.shouldBeEmpty()
    }

    test("collectEvents should return empty list when count is 0") {
      val node = RepeatNode(count = 0)
      node.children.add(HapticEventNode(durationMs = 100, intensity = 0.8f))

      val events = node.collectEvents(startTimeMs = 0)

      events.shouldBeEmpty()
    }

    test("collectEvents should return empty list when count is negative") {
      val node = RepeatNode(count = -1)
      node.children.add(HapticEventNode(durationMs = 100, intensity = 0.8f))

      val events = node.collectEvents(startTimeMs = 0)

      events.shouldBeEmpty()
    }

    test("collectEvents should repeat single child correctly") {
      val node = RepeatNode(count = 3)
      node.children.add(HapticEventNode(durationMs = 50, intensity = 0.8f))

      val events = node.collectEvents(startTimeMs = 0)

      assertSoftly {
        events shouldHaveSize 3
        events[0].startTimeMs shouldBe 0
        events[1].startTimeMs shouldBe 50
        events[2].startTimeMs shouldBe 100
        events.forEach { it.durationMs shouldBe 50 }
        events.forEach { it.intensity shouldBe 0.8f }
      }
    }

    test("collectEvents should repeat multiple children sequentially") {
      val node = RepeatNode(count = 2)
      node.children.add(HapticEventNode(durationMs = 50, intensity = 1.0f))
      node.children.add(HapticEventNode(durationMs = 30, intensity = 0.5f))

      val events = node.collectEvents(startTimeMs = 0)

      assertSoftly {
        events shouldHaveSize 4

        assertSoftly(events[0]) {
          startTimeMs shouldBe 0
          durationMs shouldBe 50
        }
        assertSoftly(events[1]) {
          startTimeMs shouldBe 50
          durationMs shouldBe 30
        }
        assertSoftly(events[2]) {
          startTimeMs shouldBe 80
          durationMs shouldBe 50
        }
        assertSoftly(events[3]) {
          startTimeMs shouldBe 130
          durationMs shouldBe 30
        }
      }
    }

    test("collectEvents should handle DelayNode by advancing time only") {
      val node = RepeatNode(count = 2)
      node.children.add(HapticEventNode(durationMs = 50, intensity = 0.8f))
      node.children.add(DelayNode(durationMs = 100))

      val events = node.collectEvents(startTimeMs = 0)

      assertSoftly {
        events shouldHaveSize 2
        assertSoftly(events[0]) {
          startTimeMs shouldBe 0
          durationMs shouldBe 50
        }
        assertSoftly(events[1]) {
          startTimeMs shouldBe 150
          durationMs shouldBe 50
        }
      }
    }

    test("collectEvents should handle only DelayNodes") {
      val node = RepeatNode(count = 3)
      node.children.add(DelayNode(durationMs = 100))

      val events = node.collectEvents(startTimeMs = 0)

      events.shouldBeEmpty()
    }

    test("collectEvents should preserve custom startTimeMs") {
      val node = RepeatNode(count = 2)
      node.children.add(HapticEventNode(durationMs = 50, intensity = 0.8f))

      val events = node.collectEvents(startTimeMs = 500)

      assertSoftly {
        events shouldHaveSize 2
        events[0].startTimeMs shouldBe 500
        events[1].startTimeMs shouldBe 550
      }
    }

    test("collectEvents should handle nested SequenceNode") {
      val innerSequence = SequenceNode()
      innerSequence.children.add(HapticEventNode(durationMs = 30, intensity = 0.5f))
      innerSequence.children.add(HapticEventNode(durationMs = 20, intensity = 0.5f))

      val node = RepeatNode(count = 2)
      node.children.add(innerSequence)

      val events = node.collectEvents(startTimeMs = 0)

      assertSoftly {
        events shouldHaveSize 4
        events[0].startTimeMs shouldBe 0
        events[1].startTimeMs shouldBe 30
        // starts at 50ms = 30 + 20)
        events[2].startTimeMs shouldBe 50
        events[3].startTimeMs shouldBe 80
      }
    }

    test("collectEvents should preserve iOS parameters") {
      val iosParams = IosHapticParameters(sharpness = 0.9f)
      val node = RepeatNode(count = 2)
      node.children.add(
        HapticEventNode(durationMs = 100, intensity = 0.8f, iosParameters = iosParams),
      )

      val events = node.collectEvents(startTimeMs = 0)

      assertSoftly {
        events shouldHaveSize 2
        events[0].iosParameters?.sharpness shouldBe 0.9f
        events[1].iosParameters?.sharpness shouldBe 0.9f
      }
    }

    test("collectEvents should match documentation example - Repeat with Haptic and Delay") {
      // Example from docs:
      // Repeat(count = 3) {
      //     Haptic(50.ms)
      //     Delay(50.ms)
      // }
      val node = RepeatNode(count = 3)
      node.children.add(HapticEventNode(durationMs = 50, intensity = 1.0f))
      node.children.add(DelayNode(durationMs = 50))

      val events = node.collectEvents(startTimeMs = 0)

      assertSoftly {
        events shouldHaveSize 3
        events[0].startTimeMs shouldBe 0
        events[1].startTimeMs shouldBe 100
        events[2].startTimeMs shouldBe 200
        events.forEach { it.durationMs shouldBe 50 }
      }
    }

    test("collectEvents with count = 1 should execute children once") {
      val node = RepeatNode(count = 1)
      node.children.add(HapticEventNode(durationMs = 100, intensity = 0.8f))

      val events = node.collectEvents(startTimeMs = 0)

      assertSoftly {
        events shouldHaveSize 1
        events[0].startTimeMs shouldBe 0
        events[0].durationMs shouldBe 100
      }
    }
  })
