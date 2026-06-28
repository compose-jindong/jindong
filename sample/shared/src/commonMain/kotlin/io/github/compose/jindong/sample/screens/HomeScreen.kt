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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.compose.jindong.HapticPlatform
import io.github.compose.jindong.rememberHapticCapabilities
import io.github.compose.jindong.sample.components.CapabilityBadge
import io.github.compose.jindong.sample.components.MonoLabel
import io.github.compose.jindong.sample.components.ScreenDescription
import io.github.compose.jindong.sample.components.SectionCard
import io.github.compose.jindong.sample.components.VGap
import io.github.compose.jindong.sample.nav.Screen
import io.github.compose.jindong.sample.theme.Dimens
import io.github.compose.jindong.sample.theme.JindongTheme

/**
 * Home / Device Capability hub (handoff 00). Reads live [rememberHapticCapabilities] into three
 * badges, then lists the seven feature modules. When the device lacks amplitude control on Android,
 * modules 02 and 05 carry a warn caption because their intensity steps will feel identical.
 */
@Composable
fun HomeScreen(
  onModuleClick: (Screen) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = JindongTheme.colors
  val caps = rememberHapticCapabilities()
  val isAndroid = caps.platform == HapticPlatform.Android
  val amplitudeFlat = isAndroid && !caps.hasAmplitudeControl

  Column(modifier = modifier.fillMaxWidth()) {
    ScreenDescription(
      buildAnnotatedString {
        append("A verification harness for the Jindong haptic library. Each module isolates one ")
        append("variable so you can ")
        withStyle(SpanStyle(color = colors.text, fontWeight = FontWeight.SemiBold)) {
          append("see vibration")
        }
        append(" on the timeline.")
      },
    )

    VGap(18.dp)

    SectionCard(radius = Dimens.radiusBigCard, padding = 0.dp) {
      MonoLabel(
        text = "DEVICE CAPABILITY",
        modifier = Modifier.padding(start = 15.dp, top = 12.dp, end = 15.dp, bottom = 9.dp),
      )
      CapabilityRow(
        key = "Haptics supported",
        value = if (caps.isSupported) "Yes" else "No",
        dotColor = if (caps.isSupported) colors.ok else colors.warn,
      )
      CapabilityRow(
        key = "Amplitude control",
        value =
        when {
          caps.platform == HapticPlatform.Ios -> "Continuous"
          caps.hasAmplitudeControl -> "Yes"
          else -> "No"
        },
        dotColor =
        if (caps.platform == HapticPlatform.Ios || caps.hasAmplitudeControl) {
          colors.ok
        } else {
          colors.warn
        },
      )
      CapabilityRow(
        key = "Platform",
        value = if (isAndroid) "Android" else "iOS",
        dotColor = colors.text3,
      )
    }

    VGap(20.dp)

    MonoLabel(text = "MODULES", modifier = Modifier.padding(bottom = 4.dp))

    Screen.modules.forEach { module ->
      val warnCaption =
        if (amplitudeFlat && (module == Screen.Intensity || module == Screen.RepeatIdx)) {
          "On this device, intensity levels feel identical"
        } else {
          null
        }
      ModuleRow(
        screen = module,
        warnCaption = warnCaption,
        onClick = { onModuleClick(module) },
      )
    }
  }
}

/** Single capability badge row with a top [divider] hairline and 11/15 padding. */
@Composable
private fun CapabilityRow(
  key: String,
  value: String,
  dotColor: androidx.compose.ui.graphics.Color,
) {
  val colors = JindongTheme.colors
  val strokePx = with(LocalDensity.current) { Dimens.stroke.toPx() }
  Box(
    modifier =
    Modifier
      .fillMaxWidth()
      .drawBehind {
        drawLine(
          color = colors.divider,
          start = Offset(0f, strokePx / 2f),
          end = Offset(size.width, strokePx / 2f),
          strokeWidth = strokePx,
        )
      }
      .padding(horizontal = 15.dp, vertical = 11.dp),
  ) {
    CapabilityBadge(key = key, value = value, dotColor = dotColor)
  }
}

/** Tappable module row: mono number, title + subtitle stack, trailing chevron, divider underline. */
@Composable
private fun ModuleRow(
  screen: Screen,
  warnCaption: String?,
  onClick: () -> Unit,
) {
  val colors = JindongTheme.colors
  val strokePx = with(LocalDensity.current) { Dimens.stroke.toPx() }
  Row(
    modifier =
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(Dimens.radiusSmall))
      .clickable(onClick = onClick)
      .drawBehind {
        drawLine(
          color = colors.divider,
          start = Offset(0f, size.height - strokePx / 2f),
          end = Offset(size.width, size.height - strokePx / 2f),
          strokeWidth = strokePx,
        )
      }
      .padding(vertical = 14.dp, horizontal = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(Dimens.rowGapLarge),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = pad2(screen.index),
      style = JindongTheme.typography.chipLabel.copy(fontWeight = FontWeight.Normal),
      color = colors.text3,
      modifier = Modifier.widthIn(min = 18.dp),
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(text = screen.title, style = JindongTheme.typography.cardTitle, color = colors.text)
      Text(
        text = screen.subtitle,
        style = JindongTheme.typography.bodySmall,
        color = colors.text3,
        modifier = Modifier.padding(top = 1.dp),
      )
      if (warnCaption != null) {
        Text(
          text = warnCaption,
          style = JindongTheme.typography.chipLabel.copy(fontWeight = FontWeight.Normal),
          color = colors.warn,
          modifier = Modifier.padding(top = 3.dp),
        )
      }
    }
    Text(text = "›", fontSize = 16.sp, color = colors.border2)
  }
}

private fun pad2(value: Int): String = if (value < 10) "0$value" else value.toString()
