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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class SequenceNodeTest :
  FunSpec({
    test("collectEvents should return empty list when no children") {
      val node = SequenceNode()

      val events = node.collectEvents(startTimeMs = 0)

      events.shouldBeEmpty()
    }

    test("collectEvents should return events from single child") {
      val node = SequenceNode()
      node.children.add(HapticEventNode(durationMs = 100, intensity = 0.8f))

      val events = node.collectEvents(startTimeMs = 0)

      events shouldHaveSize 1
      events[0].startTimeMs shouldBe 0
      events[0].durationMs shouldBe 100
      events[0].intensity shouldBe 0.8f
    }

    test("collectEvents should execute children sequentially") {
      val node = SequenceNode()
      node.children.add(HapticEventNode(durationMs = 100, intensity = 0.8f))
      node.children.add(HapticEventNode(durationMs = 50, intensity = 0.5f))

      val events = node.collectEvents(startTimeMs = 0)

      events shouldHaveSize 2
      events[0].startTimeMs shouldBe 0
      events[0].durationMs shouldBe 100
      events[1].startTimeMs shouldBe 100 // starts after first ends
      events[1].durationMs shouldBe 50
    }

    test("collectEvents should handle DelayNode by advancing time only") {
      val node = SequenceNode()
      node.children.add(HapticEventNode(durationMs = 100, intensity = 0.8f))
      node.children.add(DelayNode(durationMs = 50))
      node.children.add(HapticEventNode(durationMs = 200, intensity = 0.5f))

      val events = node.collectEvents(startTimeMs = 0)

      events shouldHaveSize 2 // only 2 haptic events
      events[0].startTimeMs shouldBe 0
      events[0].durationMs shouldBe 100
      events[1].startTimeMs shouldBe 150 // 100 + 50 delay
      events[1].durationMs shouldBe 200
    }

    test("collectEvents should preserve custom startTimeMs") {
      val node = SequenceNode()
      node.children.add(HapticEventNode(durationMs = 100, intensity = 0.8f))
      node.children.add(HapticEventNode(durationMs = 50, intensity = 0.5f))

      val events = node.collectEvents(startTimeMs = 500)

      events shouldHaveSize 2
      events[0].startTimeMs shouldBe 500
      events[1].startTimeMs shouldBe 600
    }

    test("collectEvents should handle multiple delays") {
      val node = SequenceNode()
      node.children.add(DelayNode(durationMs = 100))
      node.children.add(DelayNode(durationMs = 50))
      node.children.add(HapticEventNode(durationMs = 30, intensity = 1.0f))

      val events = node.collectEvents(startTimeMs = 0)

      events shouldHaveSize 1
      events[0].startTimeMs shouldBe 150 // 100 + 50
      events[0].durationMs shouldBe 30
    }

    test("collectEvents should handle only DelayNodes") {
      val node = SequenceNode()
      node.children.add(DelayNode(durationMs = 100))
      node.children.add(DelayNode(durationMs = 200))

      val events = node.collectEvents(startTimeMs = 0)

      events.shouldBeEmpty()
    }

    test("collectEvents should handle complex sequence from documentation example") {
      // Example from docs:
      // Sequence {
      //     Haptic(100ms)   // starts at 0ms
      //     Delay(50ms)     // advances to 150ms
      //     Haptic(200ms)   // starts at 150ms
      // }
      val node = SequenceNode()
      node.children.add(HapticEventNode(durationMs = 100, intensity = 1.0f))
      node.children.add(DelayNode(durationMs = 50))
      node.children.add(HapticEventNode(durationMs = 200, intensity = 1.0f))

      val events = node.collectEvents(startTimeMs = 0)

      events shouldHaveSize 2
      // First haptic
      events[0].startTimeMs shouldBe 0
      events[0].durationMs shouldBe 100
      // Second haptic (after 100ms haptic + 50ms delay)
      events[1].startTimeMs shouldBe 150
      events[1].durationMs shouldBe 200
    }

    test("collectEvents should handle nested SequenceNode") {
      val innerSequence = SequenceNode()
      innerSequence.children.add(HapticEventNode(durationMs = 50, intensity = 0.5f))
      innerSequence.children.add(HapticEventNode(durationMs = 50, intensity = 0.5f))

      val outerSequence = SequenceNode()
      outerSequence.children.add(HapticEventNode(durationMs = 100, intensity = 1.0f))
      outerSequence.children.add(innerSequence)

      val events = outerSequence.collectEvents(startTimeMs = 0)

      events shouldHaveSize 3
      events[0].startTimeMs shouldBe 0
      events[1].startTimeMs shouldBe 100
      events[2].startTimeMs shouldBe 150
    }

    test("collectEvents should preserve iOS parameters") {
      val iosParams = IosHapticParameters(sharpness = 0.9f)
      val node = SequenceNode()
      node.children.add(
        HapticEventNode(durationMs = 100, intensity = 0.8f, iosParameters = iosParams),
      )
      node.children.add(DelayNode(durationMs = 50))

      val events = node.collectEvents(startTimeMs = 0)

      events shouldHaveSize 1
      events[0].iosParameters?.sharpness shouldBe 0.9f
    }

    test("collectEvents with zero duration delay should not affect timing") {
      val node = SequenceNode()
      node.children.add(HapticEventNode(durationMs = 100, intensity = 0.8f))
      node.children.add(DelayNode(durationMs = 0))
      node.children.add(HapticEventNode(durationMs = 100, intensity = 0.8f))

      val events = node.collectEvents(startTimeMs = 0)

      events shouldHaveSize 2
      events[0].startTimeMs shouldBe 0
      events[1].startTimeMs shouldBe 100 // no additional delay
    }
  })
