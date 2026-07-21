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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.compose.jindong.Jindong
import io.github.compose.jindong.core.dsl.buildHapticPattern
import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.ms
import io.github.compose.jindong.dsl.Haptic
import io.github.compose.jindong.sample.components.HapticTimeline
import io.github.compose.jindong.sample.components.LabeledSlider
import io.github.compose.jindong.sample.components.PlayButton
import io.github.compose.jindong.sample.components.PresetChip
import io.github.compose.jindong.sample.components.ScreenDescription
import io.github.compose.jindong.sample.components.TimelineMapper
import io.github.compose.jindong.sample.components.VGap
import io.github.compose.jindong.sample.components.formatFixed
import io.github.compose.jindong.sample.components.timelineAccentColor
import io.github.compose.jindong.sample.theme.JindongTheme

/**
 * Single Haptic (handoff 01): isolate one `Haptic(duration, intensity)` and watch the bar's width
 * track duration and its height track intensity. Window is fixed at 600ms; the single bar starts at
 * 60ms.
 */
@Composable
fun SingleHapticScreen(modifier: Modifier = Modifier) {
  val colors = JindongTheme.colors
  var sgDur by remember { mutableIntStateOf(120) }
  var sgInt by remember { mutableFloatStateOf(0.5f) }
  var sgChip by remember { mutableStateOf("MEDIUM") }
  var playTrigger by remember { mutableIntStateOf(0) }

  val custom = sgChip == "CUSTOM"
  val resolvedIntensity = if (custom) HapticIntensity.Custom(sgInt) else presetIntensity(sgChip)

  // TIMELINE PATH (always live): recompute the pattern from scratch every recomposition via the
  // non-Compose buildHapticPattern, so the bar always reflects the current slider/chip values.
  val window = 600L
  val pattern =
    buildHapticPattern {
      delay(60.ms)
      haptic(sgDur.ms, resolvedIntensity)
    }
  val accent = timelineAccentColor()
  val bars = TimelineMapper.toBars(pattern, window) { accent }

  Column(modifier = modifier.fillMaxWidth()) {
    ScreenDescription(
      buildAnnotatedString {
        append("Change one variable, replay, watch the bar scale. ")
        withStyle(SpanStyle(color = colors.text, fontWeight = FontWeight.SemiBold)) {
          append("Width = duration")
        }
        append(", ")
        withStyle(SpanStyle(color = colors.text, fontWeight = FontWeight.SemiBold)) {
          append("height = intensity")
        }
        append(".")
      },
    )

    VGap(16.dp)

    HapticTimeline(
      bars = bars,
      topLeft = "INTENSITY ▲",
      topRight = formatFixed(resolvedIntensity.value, 2),
      minLabel = "0",
      maxLabel = "600",
      playheadProgress = 0f,
      playheadVisible = false,
    )

    VGap(18.dp)

    LabeledSlider(
      label = "Duration",
      valueText = "$sgDur ms",
      value = sgDur.toFloat(),
      onValueChange = { sgDur = it.toInt() },
      valueRange = 10f..500f,
      steps = (500 - 10) / 5 - 1,
    )

    VGap(18.dp)

    Text(
      text = "Intensity",
      style = JindongTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
      color = colors.text2,
      modifier = Modifier.padding(bottom = 11.dp),
    )

    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
      intensityChips.forEach { chip ->
        PresetChip(
          label = chip,
          selected = sgChip == chip,
          onClick = { sgChip = chip },
          subValue = chipSubValue(chip),
        )
      }
    }

    if (custom) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Slider(
          value = sgInt,
          onValueChange = { sgInt = it },
          valueRange = 0f..1f,
          steps = 99,
          colors =
          SliderDefaults.colors(
            thumbColor = colors.accent,
            activeTrackColor = colors.accent,
            inactiveTrackColor = colors.border2,
          ),
          modifier = Modifier.weight(1f),
        )
        Text(
          text = formatFixed(sgInt, 2),
          style = JindongTheme.typography.monoValue.copy(fontSize = 13.sp),
          color = colors.text,
          textAlign = TextAlign.End,
          modifier = Modifier.widthIn(min = 38.dp),
        )
      }
    }

    VGap(26.dp)

    PlayButton(onClick = { playTrigger++ })
  }

  // Explicit-play: key only on playTrigger, so this stays an explicit-play screen (Reactive is the
  // auto-firing one). The block reads live sgDur/resolvedIntensity, so Play always uses the current
  // values. The timeline above is driven separately from state and stays live.
  Jindong(playTrigger) {
    Haptic(sgDur.ms, resolvedIntensity)
  }
}

/** Mono sub-value rendered under each intensity chip (handoff 01). */
private fun chipSubValue(chip: String): String = when (chip) {
  "LIGHT" -> "0.25"
  "MEDIUM" -> "0.50"
  "STRONG" -> "0.75"
  "HIGH" -> "1.00"
  else -> "custom"
}
