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
import io.github.compose.jindong.core.model.HapticEventType
import io.github.compose.jindong.core.model.HapticPattern
import io.github.compose.jindong.core.model.ScheduledHapticEvent
import io.github.compose.jindong.core.model.checkedEventCount
import io.github.compose.jindong.core.model.checkedTimeAdd

internal const val IOS_SCHEDULING_LEAD_MS: Long = 10L
private const val MAX_CONTINUOUS_DURATION_MS = 30_000L

internal data class IosPlannedEvent(
  val startTimeMs: Long,
  val durationMs: Long,
  val eventType: HapticEventType,
  val intensity: Float,
  val sharpness: Float,
)

internal enum class IosCurveParameter { INTENSITY, SHARPNESS }

internal data class IosPlannedCurve(
  val parameter: IosCurveParameter,
  val startTimeMs: Long,
  val points: List<HapticControlPoint>,
)

internal data class IosPlayerPlan(
  val events: List<IosPlannedEvent>,
  val curves: List<IosPlannedCurve> = emptyList(),
)

internal data class IosPlaybackPlan(
  val players: List<IosPlayerPlan>,
  val diagnostics: HapticPlaybackDiagnostics,
)

internal fun HapticPattern.iosPlaybackPlan(capabilities: HapticDeviceCapabilities): IosPlaybackPlan {
  val audible = events.filter { event ->
    (event.eventType == HapticEventType.TRANSIENT || event.durationMs > 0L) &&
      (event.intensityCurve?.points?.any { it.value > 0f } ?: (event.intensity.value > 0f))
  }
  if (audible.isEmpty() || !capabilities.supportsHaptics) {
    return IosPlaybackPlan(
      emptyList(),
      HapticPlaybackDiagnostics(
        backend = if (audible.isEmpty()) HapticPlaybackBackend.SILENT else HapticPlaybackBackend.UNSUPPORTED,
        capabilities = capabilities,
        logicalDurationMs = durationMs,
        estimatedNativeDurationMs = 0L,
        unsupportedReason = if (audible.isEmpty()) null else "Core Haptics is unavailable on this device",
      ),
    )
  }

  val players = mutableListOf<IosPlayerPlan>()
  val constants = mutableListOf<IosPlannedEvent>()
  val approximations = mutableListOf("Core Haptics schedules playback 10 ms ahead on the engine clock")
  var nativeEventCount = 0L
  for (event in audible) {
    val parts = if (event.durationMs == 0L) 1L else (event.durationMs - 1L) / MAX_CONTINUOUS_DURATION_MS + 1L
    nativeEventCount += parts
    checkedEventCount(nativeEventCount, "Core Haptics native events")
    if (parts > 1L && approximations.none { it.startsWith("Continuous events") }) {
      approximations += "Continuous events over 30 seconds are split; the actuator may retrigger at boundaries"
    }
    val nativeEvents = buildList {
      var offset = 0L
      repeat(parts.toInt()) {
        val duration = minOf(event.durationMs - offset, MAX_CONTINUOUS_DURATION_MS)
        add(
          IosPlannedEvent(
            startTimeMs = event.startTimeMs + offset,
            durationMs = duration,
            eventType = event.eventType,
            intensity = if (event.intensityCurve == null) event.intensity.value else 1f,
            sharpness = if (event.sharpnessCurve == null) event.sharpness else 0f,
          ),
        )
        offset += duration
      }
    }
    if (event.intensityCurve == null && event.sharpnessCurve == null) {
      if (constants.isEmpty()) players += IosPlayerPlan(constants)
      constants += nativeEvents
    } else {
      players += IosPlayerPlan(nativeEvents, event.plannedCurves())
    }
  }
  if (audible.any { it.eventType == HapticEventType.TRANSIENT }) {
    approximations += "Transient pulse duration is hardware-defined; native duration estimates use the onset"
  }
  val nativeEnd = audible.maxOf { it.startTimeMs + it.durationMs }
  return IosPlaybackPlan(
    players.map { it.copy(events = it.events.toList()) },
    HapticPlaybackDiagnostics(
      backend = HapticPlaybackBackend.IOS_CORE_HAPTICS,
      capabilities = capabilities,
      logicalDurationMs = durationMs,
      estimatedNativeDurationMs = checkedTimeAdd(nativeEnd, IOS_SCHEDULING_LEAD_MS, "Core Haptics scheduling"),
      approximations = approximations,
    ),
  )
}

private fun ScheduledHapticEvent.plannedCurves(): List<IosPlannedCurve> = buildList {
  intensityCurve?.let { add(IosPlannedCurve(IosCurveParameter.INTENSITY, startTimeMs, it.points)) }
  sharpnessCurve?.let { add(IosPlannedCurve(IosCurveParameter.SHARPNESS, startTimeMs, it.points)) }
}
