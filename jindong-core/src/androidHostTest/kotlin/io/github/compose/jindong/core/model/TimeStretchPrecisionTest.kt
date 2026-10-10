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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random

class TimeStretchPrecisionTest :
  FunSpec({
    test("integer scaling matches exact arithmetic across Float exponents and Long boundaries") {
      fun checkScale(value: Long, factor: Float) {
        val expected = BigDecimal(value).multiply(BigDecimal(factor.toDouble())).setScale(0, RoundingMode.HALF_UP)
        if (expected > BigDecimal(Long.MAX_VALUE)) {
          shouldThrow<IllegalArgumentException> { scaleTime(value, factor) }
        } else {
          scaleTime(value, factor) shouldBe expected.longValueExact()
        }
      }
      listOf(0L, 1L, 16_777_217L, 1L shl 53, Long.MAX_VALUE / 2, Long.MAX_VALUE).forEach { value ->
        listOf(Float.MIN_VALUE, Float.fromBits(0x00800000), 0.5f, 1f, 2f, 16_777_216f, Float.MAX_VALUE).forEach { factor ->
          checkScale(value, factor)
        }
      }
      val random = Random(103)
      repeat(2_000) {
        checkScale(random.nextLong(Long.MAX_VALUE), Float.fromBits(random.nextInt(1, 0x7f800000)))
      }
    }
  })
