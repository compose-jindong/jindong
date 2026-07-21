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
import io.kotest.property.checkAll

class ReversedTest :
  FunSpec({
    test("reversing twice is the identity") {
      checkAll(patterns()) { pattern ->
        pattern.reversed().reversed() shouldBe pattern
      }
    }

    test("reversing preserves the span") {
      checkAll(patterns()) { pattern ->
        pattern.reversed().spanMs() shouldBe pattern.spanMs()
      }
    }

    test("reversing preserves event count and durations") {
      checkAll(patterns()) { pattern ->
        val reversed = pattern.reversed()
        reversed.events.size shouldBe pattern.events.size
        reversed.events.map { it.durationMs }.sorted() shouldBe pattern.events.map { it.durationMs }.sorted()
      }
    }
  })
