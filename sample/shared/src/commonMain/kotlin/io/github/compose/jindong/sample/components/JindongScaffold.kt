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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.compose.jindong.sample.nav.Screen
import io.github.compose.jindong.sample.theme.Dimens
import io.github.compose.jindong.sample.theme.JindongTheme

/**
 * Sticky 56dp app bar over a vertically scrollable [content] column.
 *
 * App bar layout:
 * - left: back chevron `‹` (only when [screen] is not the hub) tapping [onBack].
 * - center: [Screen.title] (the Home title doubles as the wordmark).
 * - right: a mono counter (`진동` on Home, else `NN / 07`) plus a 30x30 theme toggle (`☾` light /
 *   `☀` dark) calling [onToggleTheme]. Both controls keep a >= 48dp hit area.
 *
 * The body uses [Dimens.screenPadH] horizontal padding, [Dimens.bodyTop] top and [Dimens.bodyBottom]
 * bottom padding.
 */
@Composable
fun JindongScaffold(
  screen: Screen,
  isDark: Boolean,
  onBack: () -> Unit,
  onToggleTheme: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  val colors = JindongTheme.colors
  Column(modifier = modifier.fillMaxSize().background(colors.bg)) {
    AppBar(
      screen = screen,
      isDark = isDark,
      onBack = onBack,
      onToggleTheme = onToggleTheme,
    )
    Column(
      modifier =
      Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(
          start = Dimens.screenPadH,
          end = Dimens.screenPadH,
          top = Dimens.bodyTop,
          bottom = Dimens.bodyBottom,
        ),
      content = content,
    )
  }
}

@Composable
private fun AppBar(
  screen: Screen,
  isDark: Boolean,
  onBack: () -> Unit,
  onToggleTheme: () -> Unit,
) {
  val colors = JindongTheme.colors
  val strokePx = with(LocalDensity.current) { Dimens.stroke.toPx() }
  Box(
    modifier =
    Modifier
      .fillMaxWidth()
      .height(Dimens.appBarHeight)
      .background(colors.bg)
      .drawBehind {
        drawLine(
          color = colors.border,
          start = Offset(0f, size.height - strokePx / 2f),
          end = Offset(size.width, size.height - strokePx / 2f),
          strokeWidth = strokePx,
        )
      }
      .padding(horizontal = Dimens.screenPadH),
  ) {
    if (screen.index > 0) {
      Box(
        modifier =
        Modifier
          .align(Alignment.CenterStart)
          .size(Dimens.minTouch)
          .clip(RoundedCornerShape(Dimens.radiusSmall))
          .clickable(onClick = onBack)
          .semantics { contentDescription = "Back" },
        contentAlignment = Alignment.Center,
      ) {
        Text(text = "‹", fontSize = 20.sp, color = colors.text2)
      }
    }

    Text(
      text = screen.title,
      style =
      if (screen.index == 0) {
        JindongTheme.typography.wordmark
      } else {
        JindongTheme.typography.appBarTitle
      },
      color = colors.text,
      modifier = Modifier.align(Alignment.Center),
    )

    Row(
      modifier = Modifier.align(Alignment.CenterEnd),
      horizontalArrangement = Arrangement.spacedBy(Dimens.rowGapMedium),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = if (screen.index == 0) "진동" else "${pad2(screen.index)} / 07",
        style = JindongTheme.typography.monoMicro.copy(fontWeight = FontWeight.Normal),
        color = colors.text3,
      )
      ThemeToggle(isDark = isDark, onToggleTheme = onToggleTheme)
    }
  }
}

@Composable
private fun ThemeToggle(
  isDark: Boolean,
  onToggleTheme: () -> Unit,
) {
  val colors = JindongTheme.colors
  Box(
    modifier =
    Modifier
      .size(Dimens.minTouch)
      .clip(RoundedCornerShape(Dimens.radiusSmall))
      .clickable(onClick = onToggleTheme)
      .semantics { contentDescription = if (isDark) "Switch to light theme" else "Switch to dark theme" },
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
      Modifier
        .size(Dimens.toggleSize)
        .clip(RoundedCornerShape(Dimens.radiusIcon))
        .border(Dimens.stroke, colors.border, RoundedCornerShape(Dimens.radiusIcon)),
      contentAlignment = Alignment.Center,
    ) {
      Text(text = if (isDark) "☀" else "☾", fontSize = 13.sp, color = colors.text2)
    }
  }
}

/** Zero-pads a non-negative index to two digits without relying on JVM-only String.format. */
private fun pad2(value: Int): String = if (value < 10) "0$value" else value.toString()
