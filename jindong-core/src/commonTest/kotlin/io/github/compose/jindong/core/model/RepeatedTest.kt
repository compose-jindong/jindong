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
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

private fun repeatCounts(): Arb<Int> = Arb.int(0..5)

class RepeatedTest :
  FunSpec({
    test("repeating once is the identity") {
      checkAll(patterns()) { pattern ->
        pattern.repeated(1) shouldBe pattern
      }
    }

    test("repeating zero times is the empty pattern") {
      checkAll(patterns()) { pattern ->
        pattern.repeated(0) shouldBe HapticPattern.Empty
      }
    }

    test("repeated event count is the source count times the repeat count") {
      checkAll(patterns(), repeatCounts()) { pattern, times ->
        pattern.repeated(times).events.size shouldBe pattern.events.size * times
      }
    }

    test("repeated equals folding then n times") {
      checkAll(patterns(), repeatCounts()) { pattern, times ->
        val folded = (1 until times).fold(
          if (times == 0) HapticPattern.Empty else pattern,
        ) { acc, _ -> acc then pattern }
        pattern.repeated(times) shouldBe folded
      }
    }
  })
