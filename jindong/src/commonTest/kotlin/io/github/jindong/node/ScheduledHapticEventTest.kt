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
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.milliseconds

class ScheduledHapticEventTest :
  FunSpec({
    test("should use default values when only required parameters are provided") {
      val event = ScheduledHapticEvent(
        startTimeMs = 0,
        durationMs = 100,
        intensity = 0.8f,
      )

      assertSoftly(event) {
        startTimeMs shouldBe 0
        durationMs shouldBe 100
        intensity shouldBe 0.8f
        iosParameters.shouldBeNull()
      }
    }

    test("should preserve all specified parameter values") {
      val iosParams = IosHapticParameters(
        sharpness = 0.9f,
        attackTime = 50.milliseconds,
        decayTime = 100.milliseconds,
        releaseTime = 200.milliseconds,
        sustained = true,
      )

      val event = ScheduledHapticEvent(
        startTimeMs = 100,
        durationMs = 200,
        intensity = 0.5f,
        iosParameters = iosParams,
      )

      assertSoftly(event) {
        startTimeMs shouldBe 100
        durationMs shouldBe 200
        intensity shouldBe 0.5f
        iosParameters shouldBe iosParams
        iosParameters?.sharpness shouldBe 0.9f
      }
    }
  })
