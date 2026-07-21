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

/**
 * Generates arbitrary haptic patterns for property tests: 0..5 events, each with a positive duration
 * (1..200ms), intensities in [0, 1], and interior offsets that may overlap or leave gaps — exactly
 * what the transforms must tolerate.
 *
 * The timeline is anchored at 0 (the earliest event starts at 0), which is what compiled patterns
 * look like: `buildHapticPattern` collects events from `startTimeMs = 0`. `reversed()` reflects on
 * `[0, span]`, so its involution `reversed().reversed() == p` holds precisely for anchored patterns;
 * a leading gap would let a reflection fold events off the front and lose the anchor.
 */
internal fun patterns(): Arb<HapticPattern> = arbitrary { rs ->
  val count = Arb.int(0..5).bind()
  val offsets = List(count) { Arb.long(0L..500L).bind() }
  val floor = offsets.minOrNull() ?: 0L
  val events = offsets.map { offset ->
    ScheduledHapticEvent(
      startTimeMs = offset - floor,
      durationMs = Arb.long(1L..200L).bind(),
      intensity = HapticIntensity.Custom(Arb.float(0f..1f).bind()),
    )
  }
  HapticPattern(events)
}
