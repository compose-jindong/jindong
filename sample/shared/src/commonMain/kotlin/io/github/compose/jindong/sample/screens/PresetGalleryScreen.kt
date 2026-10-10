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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import io.github.compose.jindong.dsl.Clip
import io.github.compose.jindong.rememberHapticPlaybackDiagnostics
import io.github.compose.jindong.sample.components.HapticTimeline
import io.github.compose.jindong.sample.components.JindongIcons
import io.github.compose.jindong.sample.components.MonoLabel
import io.github.compose.jindong.sample.components.PlayButton
import io.github.compose.jindong.sample.components.PresetChip
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
        append("Explore common rich feedback, or tap a preset card to expand its timeline.")
      },
    )

    VGap(16.dp)

    RichFeedbackSection()

    VGap(20.dp)

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

  // Preview and playback share the preset value, including its trailing silence.
  val current = remember(playName) { presets.firstOrNull { it.name == playName } }
  if (current != null) {
    Jindong(playTrigger, playName) {
      Clip(current.toPattern())
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
      Icon(
        imageVector = JindongIcons.CaretDown,
        contentDescription = if (expanded) "Collapse" else "Expand",
        tint = colors.text3,
        modifier = Modifier.size(16.dp).rotate(caretRotation),
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

/** Small bordered play affordance on each preset header (>= 48dp hit area). */
@Composable
private fun PresetPlayChip(onClick: () -> Unit) {
  val colors = JindongTheme.colors
  // min-touch + clickable on the same node as the bordered pill so the full hit area is tappable.
  Box(
    modifier =
    Modifier
      .defaultMinSize(minWidth = Dimens.minTouch, minHeight = Dimens.minTouch)
      .clip(RoundedCornerShape(Dimens.radiusSmall))
      .border(Dimens.stroke, colors.border2, RoundedCornerShape(Dimens.radiusSmall))
      .clickable(onClick = onClick)
      .padding(horizontal = 13.dp, vertical = 7.dp),
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

/** Shared rich patterns; changing selection previews the value, and only Play changes the key. */
@Composable
private fun RichFeedbackSection() {
  val colors = JindongTheme.colors
  var selected by remember { mutableIntStateOf(0) }
  var playTrigger by remember { mutableIntStateOf(0) }
  val preset = richFeedbackPresets[selected]
  val pattern = preset.pattern
  val diagnostic = rememberHapticPlaybackDiagnostics(pattern)
  val window = presetWindow(pattern.durationMs)
  val bars = remember(pattern, window, colors.accent) { TimelineMapper.toBars(pattern, window) { colors.accent } }

  Column(
    modifier = Modifier.fillMaxWidth()
      .clip(RoundedCornerShape(Dimens.radiusBigCard))
      .border(Dimens.stroke, colors.border, RoundedCornerShape(Dimens.radiusBigCard))
      .padding(Dimens.cardPadding),
  ) {
    MonoLabel("RICH FEEDBACK")
    VGap(10.dp)
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(Dimens.rowGapSmall),
      verticalArrangement = Arrangement.spacedBy(Dimens.rowGapSmall),
    ) {
      richFeedbackPresets.forEachIndexed { index, item ->
        PresetChip(
          label = item.name,
          selected = selected == index,
          onClick = { selected = index },
          subValue = "${item.pattern.durationMs} ms",
        )
      }
    }
    VGap(12.dp)
    HapticTimeline(
      bars = bars,
      topLeft = "INTENSITY ▲",
      topRight = "${pattern.durationMs} ms",
      minLabel = "0",
      maxLabel = window.toString(),
      playheadProgress = 0f,
      playheadVisible = false,
    )
    VGap(12.dp)
    Text(
      text = "Backend: ${diagnostic.backend.name.replace('_', ' ')}",
      style = JindongTheme.typography.bodySmall,
      color = colors.text2,
    )
    Text(
      text = "Timeline ${diagnostic.logicalDurationMs} ms · Native estimate ${diagnostic.estimatedNativeDurationMs} ms",
      style = JindongTheme.typography.bodySmall,
      color = colors.text3,
    )
    diagnostic.approximations.forEach { approximation ->
      Text(text = "• $approximation", style = JindongTheme.typography.bodySmall, color = colors.text3)
    }
    diagnostic.unsupportedReason?.let { reason ->
      Text(text = reason, style = JindongTheme.typography.bodySmall, color = colors.text3)
    }
    VGap(14.dp)
    PlayButton(onClick = { playTrigger++ }, text = "Play ${preset.name}")
  }

  Jindong(playTrigger, playOnInitialComposition = false) { Clip(pattern) }
}
