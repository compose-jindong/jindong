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
package io.github.compose.jindong.core.executor

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

/**
 * Deterministic coverage of the shared, pull-based expiry decision both platform handles delegate to.
 * A [TestTimeSource] makes "natural completion" testable without real waiting — the previous
 * callback-less handle could never detect this and reported a false-positive active state forever.
 */
class HandleExpiryTest :
  FunSpec({

    test("is not expired right after creation") {
      val time = TestTimeSource()
      val expiry = HandleExpiry(totalDurationMs = 100L, timeSource = time)

      expiry.isExpired shouldBe false
    }

    test("is not expired while elapsed time is below the total duration") {
      val time = TestTimeSource()
      val expiry = HandleExpiry(totalDurationMs = 100L, timeSource = time)

      time += 99.milliseconds

      expiry.isExpired shouldBe false
    }

    // The regression guard: without time-based expiry this stays false forever (false positive).
    test("becomes expired once elapsed time reaches the total duration") {
      val time = TestTimeSource()
      val expiry = HandleExpiry(totalDurationMs = 100L, timeSource = time)

      time += 100.milliseconds

      expiry.isExpired shouldBe true
    }

    test("stays expired after the total duration has passed") {
      val time = TestTimeSource()
      val expiry = HandleExpiry(totalDurationMs = 100L, timeSource = time)

      time += 500.milliseconds

      expiry.isExpired shouldBe true
    }

    test("a zero total duration is expired from the start") {
      val time = TestTimeSource()
      val expiry = HandleExpiry(totalDurationMs = 0L, timeSource = time)

      expiry.isExpired shouldBe true
    }

    test("a negative total duration is treated as zero and expired from the start") {
      val time = TestTimeSource()
      val expiry = HandleExpiry(totalDurationMs = -10L, timeSource = time)

      expiry.isExpired shouldBe true
    }
  })
