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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.compose.jindong.sample.theme.Dimens
import io.github.compose.jindong.sample.theme.JindongTheme

/**
 * N-up segment control. Equal-weight segments separated by a 7dp gap. Active segment uses accent
 * border/text over accentBg; inactive uses border2 over transparent with text2.
 */
@Composable
fun SegmentedToggle(
  options: List<String>,
  selectedIndex: Int,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = JindongTheme.colors
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(Dimens.rowGapSmall),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    options.forEachIndexed { index, option ->
      val selected = index == selectedIndex
      val borderColor = if (selected) colors.accent else colors.border2
      val contentColor = if (selected) colors.accent else colors.text2
      val bgColor = if (selected) colors.accentBg else Color.Transparent
      // 48dp min-touch on the same node as clickable so each segment is independently tappable.
      Box(
        modifier =
        Modifier
          .weight(1f)
          .defaultMinSize(minHeight = Dimens.minTouch)
          .clip(RoundedCornerShape(Dimens.radiusChip))
          .background(bgColor)
          .border(Dimens.stroke, borderColor, RoundedCornerShape(Dimens.radiusChip))
          .clickable { onSelect(index) }
          .padding(10.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = option,
          style = JindongTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
          color = contentColor,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}
