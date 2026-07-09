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

class ThenTest :
  FunSpec({
    test("then is associative on the resulting event timeline") {
      checkAll(patterns(), patterns(), patterns()) { a, b, c ->
        val left = (a then b) then c
        val right = a then (b then c)
        left.events shouldBe right.events
      }
    }

    test("the empty pattern is a left and right identity for then") {
      checkAll(patterns()) { pattern ->
        (HapticPattern.Empty then pattern).events shouldBe pattern.events
        (pattern then HapticPattern.Empty).events shouldBe pattern.events
      }
    }

    test("appended span is the sum of the two spans") {
      checkAll(patterns(), patterns()) { a, b ->
        (a then b).spanMs() shouldBe a.spanMs() + b.spanMs()
      }
    }

    test("appended event count is the sum of the two counts") {
      checkAll(patterns(), patterns()) { a, b ->
        (a then b).events.size shouldBe a.events.size + b.events.size
      }
    }

    test("plus is an alias for then") {
      checkAll(patterns(), patterns()) { a, b ->
        (a + b) shouldBe (a then b)
      }
    }
  })
