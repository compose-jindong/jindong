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

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long

/** Generates overlapping timelines with leading/trailing silence and positive event durations. */
internal fun patterns(minDurationMs: Long = 1L): Arb<HapticPattern> = arbitrary { rs ->
  val count = Arb.int(0..5).bind()
  val offsets = List(count) { Arb.long(0L..500L).bind() }
  val events = offsets.map { offset ->
    ScheduledHapticEvent(
      startTimeMs = offset,
      durationMs = Arb.long(minDurationMs..200L).bind(),
      intensity = HapticIntensity.Custom(Arb.float(0f..1f).bind()),
    )
  }
  HapticPattern(events, durationMs = (events.maxOfOrNull { it.startTimeMs + it.durationMs } ?: 0L) + Arb.long(0L..200L).bind())
}
