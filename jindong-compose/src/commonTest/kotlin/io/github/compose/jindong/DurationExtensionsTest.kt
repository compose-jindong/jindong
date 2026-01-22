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
package io.github.compose.jindong

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class DurationExtensionsTest :
  FunSpec({
    context("milliseconds extensions") {
      test("Int.ms should create Duration in milliseconds") {
        assertSoftly {
          100.ms shouldBe 100.milliseconds
          0.ms shouldBe 0.milliseconds
          1500.ms shouldBe 1500.milliseconds
          Int.MAX_VALUE.ms shouldBe Int.MAX_VALUE.milliseconds
        }
      }

      test("Long.ms should create Duration in milliseconds") {
        assertSoftly {
          100L.ms shouldBe 100.milliseconds
          0L.ms shouldBe 0.milliseconds
          1500L.ms shouldBe 1500.milliseconds
          Long.MAX_VALUE.ms shouldBe Long.MAX_VALUE.milliseconds
        }
      }
    }
    context("conversion consistency") {
      test("500.ms should equal 0.5.seconds") {
        500.ms shouldBe 0.5.seconds
      }
    }
  })
