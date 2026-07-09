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

import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.model.HapticPattern
import io.github.compose.jindong.core.model.ScheduledHapticEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private fun pattern(vararg events: ScheduledHapticEvent): HapticPattern = HapticPattern(events.toList())

private fun event(startTimeMs: Long, durationMs: Long): ScheduledHapticEvent = ScheduledHapticEvent(
  startTimeMs = startTimeMs,
  durationMs = durationMs,
  intensity = HapticIntensity.HIGH,
)

class RawSpanMsTest :
  FunSpec({
    test("overlapping events span to the latest event end") {
      // [0,100) overlaps [50,150); the latest end is 150.
      val span = pattern(
        event(startTimeMs = 0, durationMs = 100),
        event(startTimeMs = 50, durationMs = 100),
      ).rawSpanMs()

      span shouldBe 150L
    }

    test("an empty pattern has a zero span") {
      HapticPattern.Empty.rawSpanMs() shouldBe 0L
    }

    test("a single event spans its own duration") {
      pattern(event(startTimeMs = 0, durationMs = 100)).rawSpanMs() shouldBe 100L
    }

    test("a fully contained event does not shorten the outer span") {
      // [0,200) wraps [50,100); the span stays 200 (the outer end), not the inner one.
      val span = pattern(
        event(startTimeMs = 0, durationMs = 200),
        event(startTimeMs = 50, durationMs = 50),
      ).rawSpanMs()

      span shouldBe 200L
    }
  })
