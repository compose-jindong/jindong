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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import io.github.compose.jindong.dsl.Repeat
import io.github.compose.jindong.sample.components.HapticTimeline
import io.github.compose.jindong.sample.components.LabeledSlider
import io.github.compose.jindong.sample.components.MonoLabel
import io.github.compose.jindong.sample.components.PlayButton
import io.github.compose.jindong.sample.components.ScreenDescription
import io.github.compose.jindong.sample.components.TimelineMapper
import io.github.compose.jindong.sample.components.VGap
import io.github.compose.jindong.sample.components.timelineAccentColor
import io.github.compose.jindong.sample.theme.Dimens
import io.github.compose.jindong.sample.theme.JindongTheme

private const val UNIT_INTENSITY = 0.7f

/**
 * Repeat (handoff 04): repeat one unit pattern N times (static). The timeline window is fixed at
 * 1500ms (max count 10), so bar count maps 1:1 to the slider value.
 */
@Composable
fun RepeatScreen(modifier: Modifier = Modifier) {
  val colors = JindongTheme.colors
  var rpCount by remember { mutableIntStateOf(4) }
  var playTrigger by remember { mutableIntStateOf(0) }

  val window = 1500L
  val accent = timelineAccentColor()

  // TIMELINE PATH (always live): emit count bars at explicit start times (i*150+10).
  val pattern = remember(rpCount) { repeatPattern(rpCount) }
  val bars = TimelineMapper.toBars(pattern, window) { accent }

  androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxWidth()) {
    ScreenDescription(
      buildAnnotatedString {
        append("Repeat one unit pattern ")
        withStyle(SpanStyle(color = colors.text, fontWeight = FontWeight.SemiBold)) { append("N times") }
        append(". Bar count on the timeline equals the count exactly.")
      },
    )

    VGap(14.dp)

    HapticTimeline(
      bars = bars,
      topLeft = "INTENSITY ▲",
      topRight = "× $rpCount",
      minLabel = "0",
      maxLabel = window.toString(),
      playheadProgress = 0f,
      playheadVisible = false,
    )

    VGap(16.dp)

    UnitPreviewCard()

    VGap(18.dp)

    LabeledSlider(
      label = "Count",
      valueText = "× $rpCount",
      value = rpCount.toFloat(),
      onValueChange = { rpCount = it.toInt() },
      valueRange = 1f..10f,
      steps = 8,
    )

    VGap(24.dp)

    PlayButton(onClick = { playTrigger++ })
  }

  // Explicit-play: playTrigger and the pattern parameters are keys, so Play replays the current
  // values. The timeline above is driven separately and is always live.
  Jindong(playTrigger, rpCount) {
    Repeat(rpCount) {
      Haptic(60.ms, HapticIntensity.Custom(UNIT_INTENSITY))
      Delay(90.ms)
    }
  }
}

/** "UNIT" preview: a tap bar, a gap, and a small rest marker — the repeated atom. */
@Composable
private fun UnitPreviewCard() {
  val colors = JindongTheme.colors
  Row(
    modifier =
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(Dimens.radiusCard))
      .border(Dimens.stroke, colors.border, RoundedCornerShape(Dimens.radiusCard))
      .padding(horizontal = 14.dp, vertical = 13.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    MonoLabel(text = "UNIT")
    Row(
      modifier = Modifier.height(24.dp),
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Box(
        modifier =
        Modifier
          .width(11.dp)
          .height(17.dp)
          .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
          .background(colors.accent),
      )
      Spacer(Modifier.width(3.dp))
      Box(modifier = Modifier.width(6.dp).height(2.dp).background(colors.border2))
    }
    Text(text = "tap + pause", style = JindongTheme.typography.bodySmall, color = colors.text3)
  }
}

/**
 * Builds the Repeat timeline pattern: count bars, dur 60, intensity 0.7, start i*150. Start times
 * mirror the replayed `Repeat { Haptic(60); Delay(90) }` (first pulse at 0ms) so the preview matches.
 */
internal fun repeatPattern(count: Int): HapticPattern {
  val events =
    (0 until count).map { i ->
      ScheduledHapticEvent(
        startTimeMs = i * 150L,
        durationMs = 60L,
        intensity = HapticIntensity.Custom(UNIT_INTENSITY),
      )
    }
  return HapticPattern(events)
}
