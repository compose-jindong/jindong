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

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class DelayNodeTest :
  FunSpec({
    test("collectEvents should return empty list") {
      val node = DelayNode(durationMs = 100)

      val events = node.collectEvents(startTimeMs = 0)

      events.shouldBeEmpty()
    }

    test("collectEvents should return empty list regardless of startTimeMs") {
      val node = DelayNode(durationMs = 200)

      val events = node.collectEvents(startTimeMs = 500)

      events.shouldBeEmpty()
    }

    test("children should always be empty for leaf node") {
      val node = DelayNode(durationMs = 150)

      node.children.shouldBeEmpty()
    }

    test("durationMs should be preserved") {
      val node = DelayNode(durationMs = 300)

      node.durationMs shouldBe 300
    }

    test("should support zero duration") {
      val node = DelayNode(durationMs = 0)

      assertSoftly {
        node.durationMs shouldBe 0
        node.collectEvents(startTimeMs = 0).shouldBeEmpty()
      }
    }

    test("totalDurationMs should return durationMs") {
      val node = DelayNode(durationMs = 150)

      node.totalDurationMs(startTimeMs = 0) shouldBe 150
    }

    test("totalDurationMs should be independent of startTimeMs") {
      val node = DelayNode(durationMs = 200)

      assertSoftly {
        node.totalDurationMs(startTimeMs = 0) shouldBe 200
        node.totalDurationMs(startTimeMs = 500) shouldBe 200
        node.totalDurationMs(startTimeMs = 1000) shouldBe 200
      }
    }

    test("should throw IllegalArgumentException when durationMs is negative") {
      val exception = shouldThrow<IllegalArgumentException> {
        DelayNode(durationMs = -100)
      }
      exception.message shouldContain "durationMs must be non-negative"
    }
  })
