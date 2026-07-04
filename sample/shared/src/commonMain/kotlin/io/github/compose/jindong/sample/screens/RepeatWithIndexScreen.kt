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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.compose.jindong.Jindong
import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.model.HapticPattern
import io.github.compose.jindong.core.model.ScheduledHapticEvent
import io.github.compose.jindong.core.ms
import io.github.compose.jindong.dsl.Delay
import io.github.compose.jindong.dsl.Haptic
import io.github.compose.jindong.dsl.RepeatWithIndex
import io.github.compose.jindong.sample.components.HapticTimeline
import io.github.compose.jindong.sample.components.LabeledSlider
import io.github.compose.jindong.sample.components.MonoLabel
import io.github.compose.jindong.sample.components.PlayButton
import io.github.compose.jindong.sample.components.ScreenDescription
import io.github.compose.jindong.sample.components.SegmentedToggle
import io.github.compose.jindong.sample.components.TimelineMapper
import io.github.compose.jindong.sample.components.VGap
import io.github.compose.jindong.sample.components.formatFixed
import io.github.compose.jindong.sample.components.timelineAccentColor
import io.github.compose.jindong.sample.theme.Dimens
import io.github.compose.jindong.sample.theme.JindongTheme

/**
 * Repeat With Index (handoff 05): repeat with a per-cycle ramp so each pass gets a different
 * intensity. The intended (outline) preview heights must match the compiled (filled) timeline bars.
 * Window is fixed at 1120ms.
 */
@Composable
fun RepeatWithIndexScreen(modifier: Modifier = Modifier) {
  val colors = JindongTheme.colors
  var riCount by remember { mutableIntStateOf(5) }
  var riDir by remember { mutableStateOf(RampDirection.Out) }
  var playTrigger by remember { mutableIntStateOf(0) }

  val window = 1120L
  val accent = timelineAccentColor()

  // TIMELINE PATH (always live): emit count bars at i*140+10 with the ramp intensity per pass.
  val pattern = remember(riCount, riDir) { repeatIndexPattern(riCount, riDir) }
  val bars = TimelineMapper.toBars(pattern, window) { accent }
  val intensities = (0 until riCount).map { ramp(it, riCount, riDir) }

  Column(modifier = modifier.fillMaxWidth()) {
    ScreenDescription(
      buildAnnotatedString {
        append("Repeat with a ")
        withStyle(SpanStyle(color = colors.text, fontWeight = FontWeight.SemiBold)) { append("per-cycle ramp") }
        append(" — intensity changes each pass. Preview heights should match the compiled timeline.")
      },
    )

    VGap(14.dp)

    HapticTimeline(
      bars = bars,
      topLeft = "COMPILED ▲",
      topRight = "× $riCount",
      minLabel = "0",
      maxLabel = window.toString(),
      playheadProgress = 0f,
      playheadVisible = false,
    )

    VGap(16.dp)

    IntendedPreviewCard(intensities = intensities)

    VGap(18.dp)

    Text(
      text = "Ramp direction",
      style = JindongTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
      color = colors.text2,
      modifier = Modifier.padding(bottom = 10.dp),
    )
    SegmentedToggle(
      options = listOf("Fade in", "Fade out", "None"),
      selectedIndex =
      when (riDir) {
        RampDirection.In -> 0
        RampDirection.Out -> 1
        RampDirection.None -> 2
      },
      onSelect = {
        riDir =
          when (it) {
            0 -> RampDirection.In
            1 -> RampDirection.Out
            else -> RampDirection.None
          }
      },
    )

    VGap(18.dp)

    LabeledSlider(
      label = "Count",
      valueText = "× $riCount",
      value = riCount.toFloat(),
      onValueChange = { riCount = it.toInt() },
      valueRange = 2f..8f,
      steps = 5,
    )

    VGap(24.dp)

    PlayButton(onClick = { playTrigger++ })
  }

  // VIBRATION PATH (stale pre-#84; see SingleHapticScreen for the rationale). The timeline above is
  // always live; this executor call self-heals once #84 lands.
  Jindong(playTrigger, riCount, riDir) {
    RepeatWithIndex(riCount) { i ->
      Haptic(70.ms, HapticIntensity.Custom(ramp(i, riCount, riDir)))
      Delay(70.ms)
    }
  }
}

/** Outline ("intended") per-cycle intensity bars: accentBg fill, accent 1dp border, value caption. */
@Composable
private fun IntendedPreviewCard(intensities: List<Float>) {
  val colors = JindongTheme.colors
  Column(
    modifier =
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(Dimens.radiusCard))
      .border(Dimens.stroke, colors.border, RoundedCornerShape(Dimens.radiusCard))
      .padding(horizontal = 14.dp, vertical = 13.dp),
  ) {
    MonoLabel(text = "INTENDED PER-CYCLE INTENSITY", modifier = Modifier.padding(bottom = 10.dp))
    Row(
      modifier = Modifier.fillMaxWidth().height(40.dp),
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      intensities.forEach { intensity ->
        Column(
          modifier = Modifier.weight(1f),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Bottom,
        ) {
          Box(
            modifier =
            Modifier
              .widthIn(max = 18.dp)
              .fillMaxWidth()
              .height((intensity * 36f).dp)
              .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
              .background(colors.accentBg)
              .border(Dimens.stroke, colors.accent, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)),
          )
          Text(
            text = formatFixed(intensity, 1),
            style = JindongTheme.typography.monoAxis.copy(fontSize = 8.sp, letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified),
            color = colors.text3,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
          )
        }
      }
    }
  }
}

/**
 * Builds the RepeatWithIndex timeline pattern: count bars, dur 70, start i*140, ramp intensity.
 * Start times mirror the replayed `RepeatWithIndex { Haptic(70); Delay(70) }` (first pulse at 0ms)
 * so the compiled preview lines up with what the screen actually plays.
 */
internal fun repeatIndexPattern(
  count: Int,
  dir: RampDirection,
): HapticPattern {
  val events =
    (0 until count).map { i ->
      ScheduledHapticEvent(
        startTimeMs = i * 140L,
        durationMs = 70L,
        intensity = HapticIntensity.Custom(ramp(i, count, dir)),
      )
    }
  return HapticPattern(events)
}
