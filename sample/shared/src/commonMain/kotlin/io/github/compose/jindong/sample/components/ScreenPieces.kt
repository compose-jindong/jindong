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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import io.github.compose.jindong.sample.theme.Dimens
import io.github.compose.jindong.sample.theme.JindongTheme

/**
 * Intro / description paragraph at the top of every module screen. 13sp, [text2], line-height 1.55.
 * Pass an [AnnotatedString] when a fragment needs the emphasized [text]+600 treatment (e.g.
 * "see vibration", "Width = duration").
 */
@Composable
fun ScreenDescription(
  text: AnnotatedString,
  modifier: Modifier = Modifier,
) {
  Text(
    text = text,
    style = JindongTheme.typography.intro,
    color = JindongTheme.colors.text2,
    modifier = modifier.fillMaxWidth(),
  )
}

/**
 * Plain bordered card container ([surface] over a [border] 1dp outline). [radius] defaults to the
 * card radius; preset/large cards pass [Dimens.radiusBigCard]. Content lays out in a [ColumnScope].
 */
@Composable
fun SectionCard(
  modifier: Modifier = Modifier,
  radius: Dp = Dimens.radiusCard,
  padding: Dp = Dimens.cardPadding,
  content: @Composable ColumnScope.() -> Unit,
) {
  val colors = JindongTheme.colors
  Column(
    modifier =
    modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(radius))
      .background(colors.surface)
      .border(Dimens.stroke, colors.border, RoundedCornerShape(radius))
      .padding(padding),
    content = content,
  )
}

/** Vertical spacer of the given [height]; a terse alias used between stacked screen sections. */
@Composable
fun VGap(height: Dp) {
  Spacer(Modifier.height(height))
}

/** Formats a Float to a fixed-fraction string without JVM-only String.format (e.g. 0.5 -> "0.50"). */
fun formatFixed(
  value: Float,
  decimals: Int,
): String {
  if (decimals <= 0) return value.toLong().toString()
  var scale = 1
  repeat(decimals) { scale *= 10 }
  val scaled = (value * scale + if (value >= 0f) 0.5f else -0.5f).toLong()
  val whole = scaled / scale
  val frac = (if (scaled < 0) -scaled else scaled) % scale
  val fracStr = frac.toString().padStart(decimals, '0')
  return "$whole.$fracStr"
}
