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
package io.github.compose.jindong.sample.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import io.github.compose.jindong.core.model.HapticPattern
import io.github.compose.jindong.core.model.ScheduledHapticEvent
import io.github.compose.jindong.sample.theme.Dimens
import io.github.compose.jindong.sample.theme.JindongTheme

/**
 * One timeline bar in normalized [0,1] coordinates. Anchored to the plot bottom; height grows
 * upward. [color] is usually the accent token, or a per-card tone on the preset screen.
 */
@Immutable
data class TimelineBar(
  val leftFraction: Float,
  val widthFraction: Float,
  val heightFraction: Float,
  val color: Color,
)

/**
 * Converts a [HapticPattern]'s events into normalized [TimelineBar]s.
 *
 * - `leftFraction = start / window`
 * - `widthFraction = max(0.006, dur / window)` (min 0.6% so an instantaneous tap stays visible)
 * - `heightFraction = max(0.06, intensity * 0.98)` (min 6%)
 */
object TimelineMapper {
  fun toBars(
    pattern: HapticPattern,
    windowMs: Long,
    color: (ScheduledHapticEvent) -> Color,
  ): List<TimelineBar> {
    if (windowMs <= 0L) return emptyList()
    val window = windowMs.toFloat()
    return pattern.events.map { event ->
      TimelineBar(
        leftFraction = (event.startTimeMs / window).coerceIn(0f, 1f),
        widthFraction = maxOf(0.006f, event.durationMs / window),
        heightFraction = maxOf(0.06f, event.intensity.value * 0.98f),
        color = color(event),
      )
    }
  }
}

/**
 * Signature haptic timeline: horizontal = time, vertical = intensity, gap = rest.
 *
 * The plot is drawn in a single [Canvas]; [bars] and [playheadProgress] are read inside the draw
 * lambda, so wiring an animated playhead later only redraws (no recomposition). Screens currently
 * render a static timeline ([playheadVisible] = false) and rebuild [bars] on each state change.
 *
 * @param bars normalized bars to render; this component renders exactly what it is given.
 * @param playheadProgress 0..1 horizontal position of the playhead.
 * @param playheadVisible gates the playhead alpha (visible only while playing).
 */
@Composable
fun HapticTimeline(
  bars: List<TimelineBar>,
  topLeft: String,
  topRight: String,
  minLabel: String,
  maxLabel: String,
  playheadProgress: Float,
  playheadVisible: Boolean,
  modifier: Modifier = Modifier,
) {
  val colors = JindongTheme.colors
  val axisColor = colors.border2
  val playheadColor = colors.text

  Column(
    modifier =
    modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(Dimens.radiusTimeline))
      .background(colors.surface2)
      .border(Dimens.stroke, colors.border, RoundedCornerShape(Dimens.radiusTimeline))
      .padding(PaddingValues(start = 16.dp, top = 15.dp, end = 16.dp, bottom = 14.dp)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(text = topLeft, style = JindongTheme.typography.monoAxis, color = colors.text3)
      Text(text = topRight, style = JindongTheme.typography.monoAxis, color = colors.text3)
    }

    Canvas(
      modifier =
      Modifier
        .fillMaxWidth()
        .height(Dimens.plotHeight)
        .padding(top = 8.dp),
    ) {
      drawLAxis(axisColor, Dimens.axisStroke.toPx())
      bars.forEach { bar -> drawBar(bar, Dimens.barMinWidth.toPx(), Dimens.barCornerRadius.toPx()) }
      if (playheadVisible) {
        drawPlayhead(
          progress = playheadProgress.coerceIn(0f, 1f),
          color = playheadColor,
          strokeWidth = Dimens.playheadStroke.toPx(),
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(text = minLabel, style = JindongTheme.typography.monoAxis, color = colors.text3)
      Text(text = "TIME (ms) →", style = JindongTheme.typography.monoAxis, color = colors.text3)
      Text(text = maxLabel, style = JindongTheme.typography.monoAxis, color = colors.text3)
    }
  }
}

private fun DrawScope.drawLAxis(
  color: Color,
  strokeWidth: Float,
) {
  // Left + bottom only (L-shaped axis); top/right have no border.
  drawLine(color, Offset(0f, 0f), Offset(0f, size.height), strokeWidth)
  drawLine(color, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth)
}

private fun DrawScope.drawBar(
  bar: TimelineBar,
  minWidthPx: Float,
  cornerRadiusPx: Float,
) {
  val left = bar.leftFraction * size.width
  val width = maxOf(minWidthPx, bar.widthFraction * size.width)
  val height = bar.heightFraction * size.height
  val top = size.height - height
  // Top corners rounded only; anchored to the plot bottom.
  val path =
    Path().apply {
      val r = minOf(cornerRadiusPx, width / 2f, height)
      moveTo(left, size.height)
      lineTo(left, top + r)
      quadraticTo(left, top, left + r, top)
      lineTo(left + width - r, top)
      quadraticTo(left + width, top, left + width, top + r)
      lineTo(left + width, size.height)
      close()
    }
  drawPath(path, bar.color)
}

private fun DrawScope.drawPlayhead(
  progress: Float,
  color: Color,
  strokeWidth: Float,
) {
  val x = progress * size.width
  drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth)
}

/** Default accent tone for callers that do not supply a per-event color lambda. */
@Composable
fun timelineAccentColor(): Color = JindongTheme.colors.accent
