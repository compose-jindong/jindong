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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.compose.jindong.Jindong
import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.model.HapticPattern
import io.github.compose.jindong.core.model.ScheduledHapticEvent
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
import io.github.compose.jindong.sample.components.timelineAccentColor
import io.github.compose.jindong.sample.theme.Dimens
import io.github.compose.jindong.sample.theme.JindongTheme

private const val TAP_INTENSITY = 0.7f

/**
 * Timing (handoff 03): stack tap and pause nodes into a rhythm. The timeline draws a bar per tap node
 * (intensity 0.7); pause nodes advance the cursor only, so widening a pause widens the visible gap.
 */
@Composable
fun TimingScreen(modifier: Modifier = Modifier) {
  val colors = JindongTheme.colors
  val nodes = remember {
    mutableStateListOf(Node(NodeType.Tap, 30), Node(NodeType.Pause, 120), Node(NodeType.Tap, 30))
  }
  var selected by remember { mutableIntStateOf(0) }
  var playTrigger by remember { mutableIntStateOf(0) }

  val total = nodes.sumOf { it.ms }
  val window = timingWindow(total)
  val accent = timelineAccentColor()

  // TIMELINE PATH (always live): walk the node list with an explicit cursor and emit one event per
  // tap. Building events directly keeps absolute start times deterministic as nodes/ms change.
  val pattern = remember(nodes.toList()) { timingPattern(nodes.toList()) }
  val bars = TimelineMapper.toBars(pattern, window) { accent }

  Column(modifier = modifier.fillMaxWidth()) {
    ScreenDescription(
      buildAnnotatedString {
        append("Stack ")
        withStyle(SpanStyle(color = colors.text, fontWeight = FontWeight.SemiBold)) { append("taps") }
        append(" and ")
        withStyle(SpanStyle(color = colors.text, fontWeight = FontWeight.SemiBold)) { append("pauses") }
        append(" into a rhythm. Widen a pause → the gap on the timeline grows.")
      },
    )

    VGap(14.dp)

    HapticTimeline(
      bars = bars,
      topLeft = "INTENSITY ▲",
      topRight = "$total ms",
      minLabel = "0",
      maxLabel = window.toString(),
      playheadProgress = 0f,
      playheadVisible = false,
    )

    VGap(16.dp)

    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(Dimens.rowGapSmall),
      verticalArrangement = Arrangement.spacedBy(Dimens.rowGapSmall),
    ) {
      nodes.forEachIndexed { index, node ->
        NodeBuilderChip(
          node = node,
          selected = index == selected,
          onClick = { selected = index },
        )
      }
      AddNodeButton(label = "+ Tap") {
        nodes.add(Node(NodeType.Tap, 30))
        selected = nodes.lastIndex
      }
      AddNodeButton(label = "+ Pause") {
        nodes.add(Node(NodeType.Pause, 100))
        selected = nodes.lastIndex
      }
    }

    if (selected in nodes.indices) {
      VGap(16.dp)
      NodeEditorCard(
        node = nodes[selected],
        canDelete = nodes.size > 1,
        onMsChange = { nodes[selected] = nodes[selected].copy(ms = it) },
        onDelete = {
          nodes.removeAt(selected)
          selected = (selected - 1).coerceIn(0, nodes.lastIndex)
        },
      )
    }

    VGap(18.dp)

    PlayButton(onClick = { playTrigger++ }, text = "Play sequence")
  }

  // VIBRATION PATH (stale pre-#84; see SingleHapticScreen for the rationale). The timeline above is
  // always live; this executor call self-heals once #84 lands.
  Jindong(playTrigger, nodes.toList()) {
    Sequence {
      nodes.toList().forEach { node ->
        when (node.type) {
          NodeType.Tap -> Haptic(node.ms.ms, HapticIntensity.Custom(TAP_INTENSITY))
          NodeType.Pause -> Delay(node.ms.ms)
        }
      }
    }
  }
}

/** A tap or pause node chip; taps render a round accent marker, pauses a small square. */
@Composable
private fun NodeBuilderChip(
  node: Node,
  selected: Boolean,
  onClick: () -> Unit,
) {
  io.github.compose.jindong.sample.components.NodeChip(
    label = if (node.type == NodeType.Tap) "Tap" else "Pause",
    msText = "${node.ms}ms",
    isTap = node.type == NodeType.Tap,
    selected = selected,
    onClick = onClick,
  )
}

/** Dashed-style add button for appending a tap or pause node. */
@Composable
private fun AddNodeButton(
  label: String,
  onClick: () -> Unit,
) {
  val colors = JindongTheme.colors
  Box(
    modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouch),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
      Modifier
        .clip(RoundedCornerShape(Dimens.radiusChip))
        .border(Dimens.stroke, colors.border2, RoundedCornerShape(Dimens.radiusChip))
        .clickable(onClick = onClick)
        .padding(horizontal = 11.dp, vertical = 8.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = label,
        style = JindongTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
        color = colors.text2,
      )
    }
  }
}

/** Editor for the selected node: label, value, delete affordance and a step-10 slider. */
@Composable
private fun NodeEditorCard(
  node: Node,
  canDelete: Boolean,
  onMsChange: (Int) -> Unit,
  onDelete: () -> Unit,
) {
  val colors = JindongTheme.colors
  val isTap = node.type == NodeType.Tap
  val range = if (isTap) 10f..120f else 20f..600f
  val steps = ((range.endInclusive - range.start) / 10f).toInt() - 1

  Column(
    modifier =
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(Dimens.radiusCard))
      .border(Dimens.stroke, colors.border, RoundedCornerShape(Dimens.radiusCard))
      .padding(Dimens.cardPadding),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = if (isTap) "Tap duration" else "Pause length",
        style = JindongTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
        color = colors.text,
      )
      Row(
        horizontalArrangement = Arrangement.spacedBy(Dimens.rowGapMedium),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${node.ms} ms",
          style = JindongTheme.typography.monoValue.copy(fontSize = androidx.compose.ui.unit.TextUnit.Unspecified),
          color = colors.text,
        )
        if (canDelete) {
          Box(
            modifier = Modifier.defaultMinSize(minWidth = Dimens.minTouch, minHeight = Dimens.minTouch).clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = JindongIcons.Close,
              contentDescription = "Delete node",
              tint = colors.text3,
              modifier = Modifier.size(14.dp),
            )
          }
        }
      }
    }
    androidx.compose.material3.Slider(
      value = node.ms.toFloat(),
      onValueChange = { onMsChange(it.toInt()) },
      valueRange = range,
      steps = steps,
      colors =
      androidx.compose.material3.SliderDefaults.colors(
        thumbColor = colors.accent,
        activeTrackColor = colors.accent,
        inactiveTrackColor = colors.border2,
      ),
    )
  }
}

/** Builds the pattern for the Timing timeline: one event per tap, pauses advance the cursor. */
internal fun timingPattern(nodes: List<Node>): HapticPattern {
  var cursor = 0L
  val events = mutableListOf<ScheduledHapticEvent>()
  nodes.forEach { node ->
    when (node.type) {
      NodeType.Tap -> {
        events.add(
          ScheduledHapticEvent(
            startTimeMs = cursor,
            durationMs = node.ms.toLong(),
            intensity = HapticIntensity.Custom(TAP_INTENSITY),
          ),
        )
        cursor += node.ms
      }

      NodeType.Pause -> cursor += node.ms
    }
  }
  return HapticPattern(events)
}
