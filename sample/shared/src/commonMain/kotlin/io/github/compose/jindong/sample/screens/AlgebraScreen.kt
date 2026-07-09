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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.compose.jindong.Jindong
import io.github.compose.jindong.dsl.Clip
import io.github.compose.jindong.sample.components.HapticTimeline
import io.github.compose.jindong.sample.components.MonoLabel
import io.github.compose.jindong.sample.components.PlayButton
import io.github.compose.jindong.sample.components.PresetChip
import io.github.compose.jindong.sample.components.ScreenDescription
import io.github.compose.jindong.sample.components.TimelineMapper
import io.github.compose.jindong.sample.components.VGap
import io.github.compose.jindong.sample.components.timelineAccentColor
import io.github.compose.jindong.sample.theme.Dimens
import io.github.compose.jindong.sample.theme.JindongTheme

/**
 * Algebra (handoff 08): a fixed base pattern and the result of one algebra transform, stacked on two
 * timelines that share a time axis so `reversed()` or `timeStretch(2.0)` — otherwise invisible — read
 * at a glance. Toggling a transform chip re-derives the lower timeline and re-arms the two Clip
 * playbacks; this screen doubles as a manual regression harness for the pattern algebra.
 */
@Composable
fun AlgebraScreen(modifier: Modifier = Modifier) {
  val colors = JindongTheme.colors
  val accent = timelineAccentColor()

  val base = remember { algebraBasePattern() }
  var transform by remember { mutableStateOf(AlgebraTransform.None) }
  var basePlay by remember { mutableIntStateOf(0) }
  var resultPlay by remember { mutableIntStateOf(0) }

  // Transformed pattern is derived from the fixed base and the selected transform; keyed by the
  // transform so the lower timeline and the Clip playback stay live as the selection changes.
  val transformed = remember(transform) { transform.apply(base) }

  // Both timelines share one window (the larger span), so the transform reads as a change against a
  // fixed axis rather than each plot rescaling itself.
  val window = algebraWindow(maxOf(base.spanEndMs(), transformed.spanEndMs()))
  val baseBars = TimelineMapper.toBars(base, window) { accent }
  val resultBars = TimelineMapper.toBars(transformed, window) { accent }

  Column(modifier = modifier.fillMaxWidth()) {
    ScreenDescription(
      buildAnnotatedString {
        append("A fixed ")
        withStyle(SpanStyle(color = colors.text, fontWeight = FontWeight.SemiBold)) { append("base") }
        append(" pattern and its ")
        withStyle(SpanStyle(color = colors.text, fontWeight = FontWeight.SemiBold)) { append("transform") }
        append(". Toggle a chip → the lower timeline shows what the algebra did.")
      },
    )

    VGap(16.dp)

    MonoLabel(text = "BASE", modifier = Modifier.padding(bottom = 8.dp))
    HapticTimeline(
      bars = baseBars,
      topLeft = "INTENSITY ▲",
      topRight = "${base.spanEndMs()} ms",
      minLabel = "0",
      maxLabel = window.toString(),
      playheadProgress = 0f,
      playheadVisible = false,
    )
    VGap(8.dp)
    PlayButton(onClick = { basePlay++ }, text = "Play base")

    VGap(20.dp)

    MonoLabel(text = "TRANSFORM", modifier = Modifier.padding(bottom = 8.dp))
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(Dimens.rowGapSmall),
      verticalArrangement = Arrangement.spacedBy(Dimens.rowGapSmall),
    ) {
      AlgebraTransform.entries.forEach { entry ->
        PresetChip(
          label = entry.label,
          selected = entry == transform,
          onClick = { transform = entry },
        )
      }
    }

    VGap(12.dp)

    ExprLine(expr = transform.expr)

    VGap(18.dp)

    MonoLabel(text = "RESULT", modifier = Modifier.padding(bottom = 8.dp))
    HapticTimeline(
      bars = resultBars,
      topLeft = "INTENSITY ▲",
      topRight = "${transformed.spanEndMs()} ms",
      minLabel = "0",
      maxLabel = window.toString(),
      playheadProgress = 0f,
      playheadVisible = false,
    )
    VGap(8.dp)
    PlayButton(onClick = { resultPlay++ }, text = "Play result")
  }

  // VIBRATION PATH (stale pre-#84; see SingleHapticScreen for the rationale). Playback goes through
  // Clip with the pattern threaded into the trigger keys, so each press replays the live value; the
  // buzz self-heals once #84 lands. The timelines above are always live.
  Jindong(basePlay, base) { Clip(base) }
  Jindong(resultPlay, transformed) { Clip(transformed) }
}

/** Mono one-liner showing the algebra expression for the active transform (e.g. `base.reversed()`). */
@Composable
private fun ExprLine(expr: String) {
  val colors = JindongTheme.colors
  Text(
    text = expr,
    style = JindongTheme.typography.monoValue,
    color = colors.text2,
    modifier =
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(Dimens.radiusChip))
      .background(colors.surface2)
      .border(Dimens.stroke, colors.border, RoundedCornerShape(Dimens.radiusChip))
      .padding(horizontal = 12.dp, vertical = 10.dp),
  )
}
