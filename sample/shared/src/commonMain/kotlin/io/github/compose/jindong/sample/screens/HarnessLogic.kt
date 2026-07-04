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
package io.github.compose.jindong.sample.screens

import androidx.compose.ui.graphics.Color
import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.model.HapticPattern
import io.github.compose.jindong.core.model.ScheduledHapticEvent
import kotlin.math.ceil

// Pure, side-effect-free logic shared across the harness screens. Kept out of the composables so the
// timeline math (which must always reflect live control values) is trivially correct and verifiable.

/** Intensity-chip identifiers for the Single Haptic screen, in display order. */
internal val intensityChips: List<String> = listOf("LIGHT", "MEDIUM", "STRONG", "HIGH", "CUSTOM")

/** The four amplitude presets shown on the Intensity Lab, ascending. */
internal val ascendingPresets: List<HapticIntensity> =
  listOf(HapticIntensity.LIGHT, HapticIntensity.MEDIUM, HapticIntensity.STRONG, HapticIntensity.HIGH)

/** Maps a preset chip label to its fixed intensity value (CUSTOM resolves at the call site). */
internal fun presetIntensity(chip: String): HapticIntensity = when (chip) {
  "LIGHT" -> HapticIntensity.LIGHT
  "MEDIUM" -> HapticIntensity.MEDIUM
  "STRONG" -> HapticIntensity.STRONG
  "HIGH" -> HapticIntensity.HIGH
  else -> HapticIntensity.MEDIUM
}

/** Ramp direction for Repeat-with-Index. */
internal enum class RampDirection { In, Out, None }

/**
 * Per-cycle intensity ramp for iteration [i] of [count] passes (handoff 05).
 *
 * `f = (count <= 1) ? 0 : i / (count - 1)`
 * - [RampDirection.In]:  `0.2 + 0.8f` (weak to strong)
 * - [RampDirection.Out]: `1.0 - 0.8f` (strong to weak)
 * - [RampDirection.None]: flat `0.7`
 */
internal fun ramp(
  i: Int,
  count: Int,
  dir: RampDirection,
): Float {
  val f = if (count <= 1) 0f else i.toFloat() / (count - 1)
  return when (dir) {
    RampDirection.In -> 0.2f + 0.8f * f
    RampDirection.Out -> 1.0f - 0.8f * f
    RampDirection.None -> 0.7f
  }
}

/**
 * Timeline window for the Timing screen (handoff 03): `max(600, ceil(total / 200) * 200)` where
 * [totalMs] is the sum of all node durations, floored at 200.
 */
internal fun timingWindow(totalMs: Int): Long {
  val total = maxOf(200, totalMs)
  val rounded = ceil(total / 200.0).toLong() * 200L
  return maxOf(600L, rounded)
}

/** A node in the Timing screen rhythm builder: either a tap of [ms] or a pause of [ms] (handoff 03). */
internal enum class NodeType { Tap, Pause }

internal data class Node(
  val type: NodeType,
  val ms: Int,
)

/** A single literal event tuple `(start, dur, intensity)` for a preset (handoff 06). */
internal data class PresetEvent(
  val start: Long,
  val dur: Long,
  val intensity: Float,
)

/** A preset gallery entry: name, description, tone color, total span and its literal event list. */
internal data class Preset(
  val name: String,
  val desc: String,
  val tone: Long,
  val span: Long,
  val events: List<PresetEvent>,
)

private fun ev(
  start: Long,
  dur: Long,
  intensity: Float,
) = PresetEvent(start, dur, intensity)

// Tone colors copied verbatim from handoff 06. Neutral entries resolve to text3 at the call site.
private const val TONE_NEUTRAL = 0L // sentinel: render with text3
private const val TONE_GREEN = 0xFF22C55E
private const val TONE_BLUE = 0xFF2F5FE0
private const val TONE_RED = 0xFFEF4444
private const val TONE_ORANGE = 0xFFF59E0B

/** The ten real-world presets (handoff 06), event tuples and tone hex copied verbatim. */
internal val presets: List<Preset> =
  listOf(
    Preset("Single Tap", "50ms single", TONE_NEUTRAL, 200, listOf(ev(0, 50, 0.6f))),
    Preset("Double Tap", "tap-pause-tap", TONE_NEUTRAL, 300, listOf(ev(0, 40, 0.6f), ev(130, 40, 0.6f))),
    Preset(
      "Triple Tap",
      "(tap+pause) × 3",
      TONE_NEUTRAL,
      420,
      listOf(ev(0, 40, 0.6f), ev(130, 40, 0.6f), ev(260, 40, 0.6f)),
    ),
    Preset("Light Feedback", "40ms LIGHT", TONE_GREEN, 160, listOf(ev(0, 40, 0.25f))),
    Preset("Strong Feedback", "80ms STRONG", TONE_BLUE, 200, listOf(ev(0, 80, 0.75f))),
    Preset(
      "Heartbeat",
      "strong-soft × 2, long rest",
      TONE_RED,
      900,
      listOf(ev(0, 60, 0.9f), ev(110, 50, 0.4f), ev(480, 60, 0.9f), ev(590, 50, 0.4f)),
    ),
    Preset("Notification", "soft → strong", TONE_ORANGE, 340, listOf(ev(0, 50, 0.4f), ev(160, 60, 0.8f))),
    Preset(
      "Success",
      "LIGHT→MEDIUM→STRONG",
      TONE_GREEN,
      460,
      listOf(ev(0, 50, 0.3f), ev(150, 50, 0.55f), ev(300, 60, 0.8f)),
    ),
    Preset(
      "Error",
      "HIGH × 3, urgent",
      TONE_RED,
      360,
      listOf(ev(0, 60, 1.0f), ev(120, 60, 1.0f), ev(240, 60, 1.0f)),
    ),
    Preset(
      "Ramp Up",
      "LIGHT→MEDIUM→STRONG→HIGH",
      TONE_BLUE,
      560,
      listOf(ev(0, 50, 0.25f), ev(140, 50, 0.5f), ev(280, 50, 0.75f), ev(420, 60, 1.0f)),
    ),
  )

/** Resolves a preset tone constant to a Color, or null for the neutral (text3) sentinel. */
internal fun Preset.toneColor(): Color? = if (tone == TONE_NEUTRAL) null else Color(tone)

/** Timeline window for a preset card (handoff 06): `max(span * 1.1, 200)`. */
internal fun presetWindow(span: Long): Long = maxOf((span * 1.1).toLong(), 200L)

/** Builds a [HapticPattern] from a preset's literal event tuples. */
internal fun Preset.toPattern(): HapticPattern = HapticPattern(
  events.map { event ->
    ScheduledHapticEvent(
      startTimeMs = event.start,
      durationMs = event.dur,
      intensity = HapticIntensity.Custom(event.intensity),
    )
  },
)
