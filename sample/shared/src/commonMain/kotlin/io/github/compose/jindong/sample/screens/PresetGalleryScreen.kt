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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.compose.jindong.Jindong
import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.ms
import io.github.compose.jindong.dsl.Delay
import io.github.compose.jindong.dsl.Haptic
import io.github.compose.jindong.dsl.Sequence
import io.github.compose.jindong.sample.components.HapticTimeline
import io.github.compose.jindong.sample.components.ScreenDescription
import io.github.compose.jindong.sample.components.TimelineMapper
import io.github.compose.jindong.sample.components.VGap
import io.github.compose.jindong.sample.theme.Dimens
import io.github.compose.jindong.sample.theme.JindongTheme

/**
 * Preset Gallery (handoff 06): ten real-world patterns built from the primitives, each an accordion
 * card with a tone-colored timeline. One card open at a time (Heartbeat initially).
 */
@Composable
fun PresetGalleryScreen(modifier: Modifier = Modifier) {
  val colors = JindongTheme.colors
  var open by remember { mutableStateOf("Heartbeat") }
  var playName by remember { mutableStateOf("") }
  var playTrigger by remember { mutableIntStateOf(0) }

  Column(modifier = modifier.fillMaxWidth()) {
    ScreenDescription(
      buildAnnotatedString {
        append("Ten real-world patterns built from the primitives above. Tap a card to expand its timeline.")
      },
    )

    VGap(16.dp)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      presets.forEach { preset ->
        PresetCard(
          preset = preset,
          expanded = open == preset.name,
          onToggle = { open = if (open == preset.name) "" else preset.name },
          onPlay = {
            open = preset.name
            playName = preset.name
            playTrigger++
          },
        )
      }
    }
  }

  // VIBRATION PATH (stale pre-#84; see SingleHapticScreen for the rationale). Each preset's literal
  // events are replayed serially via the Compose DSL; the timeline cards above are always live.
  val current = remember(playName) { presets.firstOrNull { it.name == playName } }
  if (current != null) {
    Jindong(playTrigger, playName) {
      Sequence {
        var cursor = 0L
        current.events.forEach { event ->
          val gap = event.start - cursor
          if (gap > 0) Delay(gap.ms)
          Haptic(event.dur.ms, HapticIntensity.Custom(event.intensity))
          cursor = event.start + event.dur
        }
      }
    }
  }
}

/** One accordion preset card: tone dot, name/desc, play affordance, caret, and an expanded timeline. */
@Composable
private fun PresetCard(
  preset: Preset,
  expanded: Boolean,
  onToggle: () -> Unit,
  onPlay: () -> Unit,
) {
  val colors = JindongTheme.colors
  val tone = preset.toneColor() ?: colors.text3
  val caretRotation by animateFloatAsState(if (expanded) 180f else 0f)

  Column(
    modifier =
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(Dimens.radiusBigCard))
      .border(Dimens.stroke, colors.border, RoundedCornerShape(Dimens.radiusBigCard)),
  ) {
    Row(
      modifier =
      Modifier
        .fillMaxWidth()
        .clickable(onClick = onToggle)
        .padding(Dimens.cardPadding),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(tone))
      Column(modifier = Modifier.weight(1f)) {
        Text(text = preset.name, style = JindongTheme.typography.cardTitle, color = colors.text)
        Text(
          text = preset.desc,
          style = JindongTheme.typography.bodySmall,
          color = colors.text3,
          modifier = Modifier.padding(top = 1.dp),
        )
      }
      PresetPlayChip(onClick = onPlay)
      Text(
        text = "⌄",
        style = JindongTheme.typography.bodySmall,
        color = colors.text3,
        modifier = Modifier.rotate(caretRotation),
      )
    }

    if (expanded) {
      Box(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
        PresetTimeline(preset = preset, tone = tone)
      }
    }
  }
}

/** Tone-colored timeline for the expanded preset, window = max(span * 1.1, 200). */
@Composable
private fun PresetTimeline(
  preset: Preset,
  tone: Color,
) {
  val window = presetWindow(preset.span)
  val pattern = remember(preset.name) { preset.toPattern() }
  val bars = TimelineMapper.toBars(pattern, window) { tone }
  HapticTimeline(
    bars = bars,
    topLeft = "INTENSITY ▲",
    topRight = "${preset.span} ms",
    minLabel = "0",
    maxLabel = window.toString(),
    playheadProgress = 0f,
    playheadVisible = false,
  )
}

/** Small bordered "▶" affordance on each preset header (>= 48dp hit area). */
@Composable
private fun PresetPlayChip(onClick: () -> Unit) {
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
        .padding(horizontal = 13.dp, vertical = 7.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "▶",
        style = JindongTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
        color = colors.text2,
      )
    }
  }
}
