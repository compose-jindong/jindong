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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.compose.jindong.sample.theme.Dimens
import io.github.compose.jindong.sample.theme.JindongTheme

/**
 * Selectable preset chip. Active = accent border/text over accentBg; inactive = border2 border over
 * transparent with text2. Optional [subValue] renders below the label in dim mono. Wrapped to a
 * >= 48dp hit area.
 */
@Composable
fun PresetChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  subValue: String? = null,
) {
  val colors = JindongTheme.colors
  val borderColor = if (selected) colors.accent else colors.border2
  val labelColor = if (selected) colors.accent else colors.text2
  val bgColor = if (selected) colors.accentBg else Color.Transparent

  // Click + 48dp min-touch live on the same (outer) node so the whole hit area is tappable; the
  // inner content keeps the compact chip visuals (8/11 padding).
  Box(
    modifier =
    modifier
      .defaultMinSize(minHeight = Dimens.minTouch)
      .clip(RoundedCornerShape(Dimens.radiusChip))
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier =
      Modifier
        .clip(RoundedCornerShape(Dimens.radiusChip))
        .background(bgColor)
        .border(Dimens.stroke, borderColor, RoundedCornerShape(Dimens.radiusChip))
        .padding(horizontal = 11.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(text = label, style = JindongTheme.typography.chipLabel, color = labelColor)
      if (subValue != null) {
        Text(
          text = subValue,
          style = JindongTheme.typography.monoAxis.copy(fontSize = 9.sp),
          color = labelColor.copy(alpha = 0.65f),
        )
      }
    }
  }
}

/**
 * Selectable node chip for sequence/rhythm editing. Adds a 7dp marker: a filled accent circle for a
 * tap node ([isTap] true) or a square border2 marker for a pause. Active state mirrors [PresetChip].
 */
@Composable
fun NodeChip(
  label: String,
  msText: String,
  isTap: Boolean,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = JindongTheme.colors
  val borderColor = if (selected) colors.accent else colors.border2
  val contentColor = if (selected) colors.accent else colors.text2
  val bgColor = if (selected) colors.accentBg else Color.Transparent
  val markerShape = if (isTap) CircleShape else RoundedCornerShape(1.dp)
  val markerColor = if (isTap) colors.accent else colors.border2

  Box(
    modifier =
    modifier
      .defaultMinSize(minHeight = Dimens.minTouch)
      .clip(RoundedCornerShape(Dimens.radiusChip))
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Row(
      modifier =
      Modifier
        .clip(RoundedCornerShape(Dimens.radiusChip))
        .background(bgColor)
        .border(Dimens.stroke, borderColor, RoundedCornerShape(Dimens.radiusChip))
        .padding(horizontal = 11.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(Dimens.rowGapSmall),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(modifier = Modifier.size(Dimens.nodeDotSize).clip(markerShape).background(markerColor))
      Text(text = label, style = JindongTheme.typography.chipLabel, color = contentColor)
      Text(
        text = msText,
        style = JindongTheme.typography.monoAxis.copy(fontSize = 9.sp),
        color = contentColor.copy(alpha = 0.65f),
      )
    }
  }
}
