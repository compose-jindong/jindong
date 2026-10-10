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

import io.github.compose.jindong.core.model.HapticControlPoint
import io.github.compose.jindong.core.model.HapticCurve
import io.github.compose.jindong.core.model.HapticEventType
import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.model.HapticPattern
import io.github.compose.jindong.core.model.ScheduledHapticEvent
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class IosPlaybackPlanTest :
  FunSpec({
    val capable = HapticDeviceCapabilities(true, true)

    test("zero duration transient is audible while legacy zero duration continuous is silent") {
      val transient = richEvent(40, 0, HapticEventType.TRANSIENT)
      val legacy = ScheduledHapticEvent(0, 0, HapticIntensity.STRONG)
      val plan = HapticPattern(listOf(legacy, transient), 100).iosPlaybackPlan(capable)
      plan.players.single().events.single().eventType shouldBe HapticEventType.TRANSIENT
      plan.players.single().events.single().startTimeMs shouldBe 40L
      plan.diagnostics.logicalDurationMs shouldBe 100L
      plan.diagnostics.estimatedNativeDurationMs shouldBe 50L
      plan.diagnostics.approximations.any { it.contains("hardware-defined") } shouldBe true
    }

    test("curves replace fixed intensity and normalize native bases") {
      val rise = HapticCurve(listOf(HapticControlPoint(0, 0f), HapticControlPoint(100, 0.8f)))
      val event = richEvent(25, 100, intensity = 0f, intensityCurve = rise, sharpnessCurve = rise)
      val plan = HapticPattern(listOf(event)).iosPlaybackPlan(capable)
      val player = plan.players.single()
      player.events.single().intensity shouldBe 1f
      player.events.single().sharpness shouldBe 0f
      player.curves.map { it.parameter } shouldBe listOf(IosCurveParameter.INTENSITY, IosCurveParameter.SHARPNESS)
      player.curves.map { it.startTimeMs } shouldBe listOf(25L, 25L)
      player.curves.first().points shouldBe rise.points
    }

    test("independent curves are isolated from constant and overlapping events") {
      val rise = HapticCurve(listOf(HapticControlPoint(0, 0f), HapticControlPoint(100, 1f)))
      val fall = HapticCurve(listOf(HapticControlPoint(0, 1f), HapticControlPoint(100, 0f)))
      val pattern = HapticPattern(
        listOf(
          richEvent(0, 100, intensityCurve = rise),
          richEvent(30, 0, HapticEventType.TRANSIENT, sharpness = 0.9f),
          richEvent(25, 100, intensityCurve = fall),
          richEvent(50, 100, sharpness = 0.2f),
        ),
      )
      val plan = pattern.iosPlaybackPlan(capable)
      plan.players.size shouldBe 3
      plan.players[0].curves.single().points shouldBe rise.points
      plan.players[1].events.size shouldBe 2
      plan.players[1].curves shouldBe emptyList()
      plan.players[1].events.map { it.sharpness } shouldBe listOf(0.9f, 0.2f)
      plan.players[2].curves.single().points shouldBe fall.points
      pattern.iosPlaybackPlan(capable) shouldBe plan
    }

    test("silent curves do not create players or scheduling pulses") {
      val zero = HapticCurve(listOf(HapticControlPoint(0, 0f), HapticControlPoint(100, 0f)))
      val pattern = HapticPattern(listOf(richEvent(0, 100, intensityCurve = zero)), 150)
      val plan = pattern.iosPlaybackPlan(capable)
      plan.players shouldBe emptyList()
      plan.diagnostics.backend shouldBe HapticPlaybackBackend.SILENT
      plan.diagnostics.logicalDurationMs shouldBe 150L
      plan.diagnostics.estimatedNativeDurationMs shouldBe 0L
      plan.diagnostics.approximations shouldBe emptyList()
    }

    test("unsupported devices preserve the diagnostic logical duration without native output") {
      val plan = HapticPattern(listOf(richEvent(0, 100))).iosPlaybackPlan(HapticDeviceCapabilities(false, false))
      plan.players shouldBe emptyList()
      plan.diagnostics.backend shouldBe HapticPlaybackBackend.UNSUPPORTED
      plan.diagnostics.logicalDurationMs shouldBe 100L
      plan.diagnostics.estimatedNativeDurationMs shouldBe 0L
      plan.diagnostics.unsupportedReason shouldBe "Core Haptics is unavailable on this device"
    }

    test("continuous events are split at the native 30 second limit while retaining one curve") {
      val curve = HapticCurve(listOf(HapticControlPoint(0, 0f), HapticControlPoint(60_001, 1f)))
      val plan = HapticPattern(listOf(richEvent(10, 60_001, intensityCurve = curve))).iosPlaybackPlan(capable)
      val player = plan.players.single()
      player.events.map { it.startTimeMs } shouldBe listOf(10L, 30_010L, 60_010L)
      player.events.map { it.durationMs } shouldBe listOf(30_000L, 30_000L, 1L)
      player.curves.single().points shouldBe curve.points
      plan.diagnostics.approximations.any { it.contains("split") } shouldBe true
      shouldThrow<IllegalArgumentException> {
        HapticPattern(listOf(richEvent(0, Long.MAX_VALUE))).iosPlaybackPlan(capable)
      }
    }
  })

private fun richEvent(
  start: Long,
  duration: Long,
  type: HapticEventType = HapticEventType.CONTINUOUS,
  intensity: Float = 0.6f,
  sharpness: Float = 0.5f,
  intensityCurve: HapticCurve? = null,
  sharpnessCurve: HapticCurve? = null,
): ScheduledHapticEvent = ScheduledHapticEvent(
  startTimeMs = start,
  durationMs = duration,
  intensity = HapticIntensity.Custom(intensity),
  eventType = type,
  sharpness = sharpness,
  intensityCurve = intensityCurve,
  sharpnessCurve = sharpnessCurve,
)
