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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.compose.jindong.Jindong
import io.github.compose.jindong.core.dsl.buildHapticPattern
import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.ms
import io.github.compose.jindong.dsl.Delay
import io.github.compose.jindong.dsl.Haptic
import io.github.compose.jindong.dsl.Sequence
import io.github.compose.jindong.sample.components.HapticTimeline
import io.github.compose.jindong.sample.components.JindongIcons
import io.github.compose.jindong.sample.components.PlayButton
import io.github.compose.jindong.sample.components.ScreenDescription
import io.github.compose.jindong.sample.components.TimelineMapper
import io.github.compose.jindong.sample.components.VGap
import io.github.compose.jindong.sample.components.formatFixed
import io.github.compose.jindong.sample.components.timelineAccentColor
import io.github.compose.jindong.sample.theme.Dimens
import io.github.compose.jindong.sample.theme.JindongTheme

/**
 * Intensity Lab (handoff 02): compare LIGHT/MEDIUM/STRONG/HIGH at a fixed 100ms, plus a custom
 * fader. The timeline draws an ascending staircase; "Play all" fires the four presets in sequence.
 */
@Composable
fun IntensityLabScreen(modifier: Modifier = Modifier) {
  val colors = JindongTheme.colors
  var ilCustom by remember { mutableFloatStateOf(0.6f) }
  // One trigger per actionable button so each tap re-fires its own pattern key.
  var playAll by remember { mutableIntStateOf(0) }
  var rowTrigger by remember { mutableIntStateOf(0) }
  var rowIntensity by remember { mutableStateOf<HapticIntensity>(HapticIntensity.LIGHT) }
  var customTrigger by remember { mutableIntStateOf(0) }

  val window = 900L
  // Ascending staircase: each preset is 100ms with an 80ms gap, so events land at 20/200/380/560ms.
  val staircase =
    buildHapticPattern {
      ascendingPresets.forEachIndexed { i, intensity ->
        if (i == 0) delay(20.ms) else delay(80.ms)
        haptic(100.ms, intensity)
      }
    }
  val accent = timelineAccentColor()
  val bars = TimelineMapper.toBars(staircase, window) { accent }

  Column(modifier = modifier.fillMaxWidth()) {
    ScreenDescription(
      buildAnnotatedString {
        append("Compare strength levels side by side. ")
        withStyle(SpanStyle(color = colors.text, fontWeight = FontWeight.SemiBold)) {
          append("Duration fixed at 100 ms")
        }
        append(" — only height changes.")
      },
    )

    VGap(14.dp)

    HapticTimeline(
      bars = bars,
      topLeft = "INTENSITY ▲",
      topRight = "0.25 → 1.00",
      minLabel = "0",
      maxLabel = "900",
      playheadProgress = 0f,
      playheadVisible = false,
    )

    VGap(16.dp)

    PlayButton(onClick = { playAll++ }, text = "Play all (ascending)")

    VGap(18.dp)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      ascendingPresets.forEach { intensity ->
        IntensityRow(
          intensity = intensity,
          onPlay = {
            rowIntensity = intensity
            rowTrigger++
          },
        )
      }
    }

    VGap(14.dp)

    CustomCard(
      value = ilCustom,
      onValueChange = { ilCustom = it },
      onPlay = { customTrigger++ },
    )
  }

  // VIBRATION PATH (stale pre-#84; see SingleHapticScreen for the rationale). The timeline above is
  // always live; these executor calls self-heal once #84 lands.
  Jindong(playAll) {
    Sequence {
      ascendingPresets.forEach { intensity ->
        Haptic(100.ms, intensity)
        Delay(120.ms)
      }
    }
  }
  Jindong(rowTrigger, rowIntensity) {
    Haptic(100.ms, rowIntensity)
  }
  Jindong(customTrigger, ilCustom) {
    Haptic(100.ms, HapticIntensity.Custom(ilCustom))
  }
}

/** One intensity row: mini bar preview, label + "100 ms · v", trailing play affordance. */
@Composable
private fun IntensityRow(
  intensity: HapticIntensity,
  onPlay: () -> Unit,
) {
  val colors = JindongTheme.colors
  Row(
    modifier =
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(Dimens.radiusCard))
      .border(Dimens.stroke, colors.border, RoundedCornerShape(Dimens.radiusCard))
      .padding(horizontal = 13.dp, vertical = 11.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    MiniBar(intensity.value, accent = colors.accent)
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = intensityLabel(intensity),
        style = JindongTheme.typography.rowTitle,
        color = colors.text,
      )
      Text(
        text = "100 ms · ${formatFixed(intensity.value, 2)}",
        style = JindongTheme.typography.monoMicro,
        color = colors.text3,
      )
    }
    PlayChip(onClick = onPlay)
  }
}

/** 40x34 mini bar area with a width-14 accent bar whose height is `intensity * 30`dp. */
@Composable
private fun MiniBar(
  intensity: Float,
  accent: Color,
) {
  Box(
    modifier = Modifier.width(40.dp).height(34.dp),
    contentAlignment = Alignment.BottomStart,
  ) {
    Box(
      modifier =
      Modifier
        .padding(start = 13.dp)
        .width(14.dp)
        .height((intensity * 30f).dp)
        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
        .background(accent),
    )
  }
}

/** Custom intensity card: label row + value, accent slider, trailing play. */
@Composable
private fun CustomCard(
  value: Float,
  onValueChange: (Float) -> Unit,
  onPlay: () -> Unit,
) {
  val colors = JindongTheme.colors
  Column(
    modifier =
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(Dimens.radiusCard))
      .border(Dimens.stroke, colors.border, RoundedCornerShape(Dimens.radiusCard))
      .padding(13.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(text = "Custom", style = JindongTheme.typography.rowTitle, color = colors.text)
      Text(
        text = formatFixed(value, 2),
        style = JindongTheme.typography.monoValue.copy(fontSize = androidx.compose.ui.unit.TextUnit.Unspecified),
        color = colors.text,
      )
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Slider(
        value = value,
        onValueChange = onValueChange,
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
      PlayChip(onClick = onPlay)
    }
  }
}

/** Small bordered play affordance used on the rows and the custom card (>= 48dp hit area). */
@Composable
private fun PlayChip(onClick: () -> Unit) {
  val colors = JindongTheme.colors
  Box(
    modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouch),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
      Modifier
        .clip(RoundedCornerShape(Dimens.radiusSmall))
        .border(Dimens.stroke, colors.border2, RoundedCornerShape(Dimens.radiusSmall))
        .clickable(onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 7.dp),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = JindongIcons.Play,
        contentDescription = "Play",
        tint = colors.text2,
        modifier = Modifier.size(13.dp),
      )
    }
  }
}

private fun intensityLabel(intensity: HapticIntensity): String = when (intensity) {
  HapticIntensity.LIGHT -> "LIGHT"
  HapticIntensity.MEDIUM -> "MEDIUM"
  HapticIntensity.STRONG -> "STRONG"
  HapticIntensity.HIGH -> "HIGH"
  else -> "CUSTOM"
}
