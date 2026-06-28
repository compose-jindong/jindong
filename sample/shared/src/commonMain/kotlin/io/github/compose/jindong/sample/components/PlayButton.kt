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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.compose.jindong.sample.theme.Dimens
import io.github.compose.jindong.sample.theme.JindongTheme

/**
 * Full-width primary action button: a [JindongIcons.Play] glyph plus [text] label. Inverted ink/white
 * surface ([JindongColors.btnBg] / [JindongColors.btnText]) — not accent — per the handoff. Wrapped
 * to a >= 48dp hit area.
 */
@Composable
fun PlayButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  text: String = "Play",
) {
  Box(
    modifier =
    modifier
      .fillMaxWidth()
      .defaultMinSize(minHeight = Dimens.minTouch)
      .clip(RoundedCornerShape(Dimens.radiusButton))
      .background(JindongTheme.colors.btnBg)
      .clickable(onClick = onClick)
      .padding(14.dp),
    contentAlignment = Alignment.Center,
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = JindongIcons.Play,
        contentDescription = null,
        tint = JindongTheme.colors.btnText,
        modifier = Modifier.size(15.dp),
      )
      Text(
        text = text,
        style = JindongTheme.typography.cardTitle,
        color = JindongTheme.colors.btnText,
      )
    }
  }
}
