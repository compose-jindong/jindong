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
package io.github.compose.jindong.sample

import io.github.compose.jindong.core.model.HapticEventType
import io.github.compose.jindong.sample.screens.celebrationFeedback
import io.github.compose.jindong.sample.screens.correctFeedback
import io.github.compose.jindong.sample.screens.errorFeedback
import kotlin.test.Test
import kotlin.test.assertEquals

class RichFeedbackPatternsTest {
  @Test
  fun correctFeedbackKeepsThreeImpactsAndTrailingSilence() {
    assertEquals(240L, correctFeedback.durationMs)
    assertEquals(listOf(0L, 80L, 160L), correctFeedback.events.map { it.startTimeMs })
    assertEquals(List(3) { 0L }, correctFeedback.events.map { it.durationMs })
    assertEquals(List(3) { HapticEventType.TRANSIENT }, correctFeedback.events.map { it.eventType })
    assertEquals(List(3) { 0.9f }, correctFeedback.events.map { it.sharpness })
  }

  @Test
  fun celebrationKeepsSharedCurveTimesAndTexture() {
    assertEquals(540L, celebrationFeedback.durationMs)
    assertEquals(correctFeedback.events, celebrationFeedback.events.take(3))
    val continuous = celebrationFeedback.events.last()
    assertEquals(HapticEventType.CONTINUOUS, continuous.eventType)
    assertEquals(240L, continuous.startTimeMs)
    assertEquals(300L, continuous.durationMs)
    assertEquals(listOf(0L, 80L, 300L), continuous.intensityCurve!!.points.map { it.timeMs })
    assertEquals(listOf(0f, 0.8f, 0f), continuous.intensityCurve!!.points.map { it.value })
    assertEquals(listOf(0.2f, 0.8f), continuous.sharpnessCurve!!.points.map { it.value })
  }

  @Test
  fun errorOverlapsAtZeroAndPreservesFinalPause() {
    assertEquals(400L, errorFeedback.durationMs)
    assertEquals(listOf(0L, 0L), errorFeedback.events.map { it.startTimeMs })
    assertEquals(listOf(HapticEventType.CONTINUOUS, HapticEventType.TRANSIENT), errorFeedback.events.map { it.eventType })
    assertEquals(300L, errorFeedback.events.maxOf { it.startTimeMs + it.durationMs })
    assertEquals(0f, errorFeedback.events.first().intensityCurve!!.points.last().value)
  }
}
