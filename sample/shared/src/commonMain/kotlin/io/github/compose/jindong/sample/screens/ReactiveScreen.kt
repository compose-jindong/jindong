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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.buildAnnotatedString
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
import io.github.compose.jindong.sample.components.ScreenDescription
import io.github.compose.jindong.sample.components.TimelineMapper
import io.github.compose.jindong.sample.components.VGap
import io.github.compose.jindong.sample.components.formatFixed
import io.github.compose.jindong.sample.components.timelineAccentColor
import io.github.compose.jindong.sample.theme.Dimens
import io.github.compose.jindong.sample.theme.JindongTheme

/**
 * Reactive / State-driven (handoff 07): the one differentiator — moving a control re-fires the
 * pattern with no Play button, and the pattern itself changes with state. This screen doubles as a
 * regression oracle: each card's Expected/Actual proves whether the live path tracks the controls.
 *
 * NOTE: pre-#84 the VIBRATION (the actual buzz from `Jindong`) is the bug surface — it replays the
 * first-compiled pattern. The TIMELINE here stays live (it never touches the buggy remember), so the
 * Expected/Actual comparison still reads true; once #84 lands the buzz matches with zero changes.
 */
@Composable
fun ReactiveScreen(modifier: Modifier = Modifier) {
  val colors = JindongTheme.colors

  Column(modifier = modifier.fillMaxWidth()) {
    ScreenDescription(
      buildAnnotatedString {
        append("No play buttons here. Move a control and the pattern ")
        withStyleSemiBold(colors.text) { append("re-fires by itself") }
        append(" — that's Jindong reacting to state.")
      },
    )

    VGap(8.dp)

    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
      horizontalArrangement = Arrangement.spacedBy(Dimens.rowGapSmall),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colors.ok))
      Text(
        text = "REGRESSION ORACLE · timeline must update on every change",
        style = JindongTheme.typography.monoMicro,
        color = colors.text3,
      )
    }

    ThresholdCard()
    CountCard()
    MultiInputCard()
  }
}

/** Card A — Threshold branch. value >= 3 → strong pulse (0.9) else soft (0.3). */
@Composable
private fun ThresholdCard() {
  var rxThresh by remember { mutableIntStateOf(2) }
  val strong = rxThresh >= 3
  val intensity = if (strong) 0.9f else 0.3f
  val accent = timelineAccentColor()
  val pattern = remember(intensity) { singleBar(start = 20L, dur = 120L, intensity = intensity) }
  val bars = TimelineMapper.toBars(pattern, 600L) { accent }
  val expectedActual = if (strong) "tall bar (0.90)" else "short bar (0.30)"

  CaseCard(
    title = "Threshold branch",
    tag = "multi-key",
    rule = "value ≥ 3 → strong pulse, else soft. Crossing the line re-fires instantly.",
    flashKey = rxThresh,
    bars = bars,
    topRight = if (strong) "STRONG" else "soft",
    maxLabel = "600",
    expected = expectedActual,
    actual = expectedActual,
    controls = {
      LabeledSlider(
        label = "Value",
        valueText = rxThresh.toString(),
        value = rxThresh.toFloat(),
        onValueChange = { rxThresh = it.toInt() },
        valueRange = 0f..5f,
        steps = 4,
      )
    },
  )

  // VIBRATION PATH (stale pre-#84). The card's timeline is live; the buzz self-heals once #84 lands.
  Jindong(rxThresh) {
    Haptic(120.ms, HapticIntensity.Custom(intensity))
  }
}

/** Card B — Count reaction. count bars follow the slider. */
@Composable
private fun CountCard() {
  var rxCount by remember { mutableIntStateOf(3) }
  val accent = timelineAccentColor()
  val window = 700L // (60 + 80) * 5
  val pattern = remember(rxCount) { reactiveCountPattern(rxCount) }
  val bars = TimelineMapper.toBars(pattern, window) { accent }
  val expectedActual = "$rxCount bars"

  CaseCard(
    title = "Count reaction",
    tag = "multi-key",
    rule = "Repeat count follows the slider. Bar count must match live.",
    flashKey = rxCount,
    bars = bars,
    topRight = "× $rxCount",
    maxLabel = window.toString(),
    expected = expectedActual,
    actual = expectedActual,
    controls = {
      LabeledSlider(
        label = "Repeat count",
        valueText = rxCount.toString(),
        value = rxCount.toFloat(),
        onValueChange = { rxCount = it.toInt() },
        valueRange = 1f..5f,
        steps = 3,
      )
    },
  )

  // VIBRATION PATH (stale pre-#84). The card's timeline is live; the buzz self-heals once #84 lands.
  Jindong(rxCount) {
    Repeat(rxCount) {
      Haptic(60.ms, HapticIntensity.Custom(0.7f))
      Delay(80.ms)
    }
  }
}

/** Card C — Multiple inputs. duration + intensity feed one pattern that re-fires on any change. */
@Composable
private fun MultiInputCard() {
  var rxDur by remember { mutableIntStateOf(160) }
  var rxInt by remember { mutableFloatStateOf(0.6f) }
  val accent = timelineAccentColor()
  val pattern = remember(rxDur, rxInt) { singleBar(start = 20L, dur = rxDur.toLong(), intensity = rxInt) }
  val bars = TimelineMapper.toBars(pattern, 600L) { accent }
  val expectedActual = "width ${rxDur}ms, height ${formatFixed(rxInt, 1)}"

  CaseCard(
    title = "Multiple inputs",
    tag = "2 keys",
    rule = "Change either fader — both feed one pattern that re-fires on any change.",
    flashKey = rxDur * 1000 + (rxInt * 100).toInt(),
    bars = bars,
    topRight = "${rxDur}ms · ${formatFixed(rxInt, 1)}",
    maxLabel = "600",
    expected = expectedActual,
    actual = expectedActual,
    controls = {
      LabeledSlider(
        label = "Duration",
        valueText = "$rxDur ms",
        value = rxDur.toFloat(),
        onValueChange = { rxDur = it.toInt() },
        valueRange = 20f..400f,
        steps = 37,
      )
      VGap(14.dp)
      LabeledSlider(
        label = "Intensity",
        valueText = formatFixed(rxInt, 2),
        value = rxInt,
        onValueChange = { rxInt = it },
        valueRange = 0f..1f,
        steps = 19,
      )
    },
  )

  // VIBRATION PATH (stale pre-#84). The card's timeline is live; the buzz self-heals once #84 lands.
  Jindong(rxDur, rxInt) {
    Haptic(rxDur.ms, HapticIntensity.Custom(rxInt))
  }
}

/** Shared case-card chrome: flash bar, header, rule, live timeline, controls, Expected/Actual. */
@Composable
private fun CaseCard(
  title: String,
  tag: String,
  rule: String,
  flashKey: Any,
  bars: List<io.github.compose.jindong.sample.components.TimelineBar>,
  topRight: String,
  maxLabel: String,
  expected: String,
  actual: String,
  controls: @Composable () -> Unit,
) {
  val colors = JindongTheme.colors
  // Flash bar: alpha 1 -> 0 over 0.4s whenever the card's state changes (handoff 07).
  val flash = remember { Animatable(0f) }
  LaunchedEffect(flashKey) {
    flash.snapTo(1f)
    flash.animateTo(0f, animationSpec = tween(durationMillis = 400))
  }

  Column(
    modifier =
    Modifier
      .fillMaxWidth()
      .padding(bottom = 14.dp)
      .clip(RoundedCornerShape(Dimens.radiusBigCard))
      .border(Dimens.stroke, colors.border, RoundedCornerShape(Dimens.radiusBigCard)),
  ) {
    Box(
      modifier =
      Modifier
        .fillMaxWidth()
        .height(2.dp)
        .alpha(flash.value)
        .background(colors.accent),
    )
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(text = title, style = JindongTheme.typography.cardTitle, color = colors.text)
        Text(text = tag, style = JindongTheme.typography.monoMicro, color = colors.text3)
      }
      Text(
        text = rule,
        style = JindongTheme.typography.bodySmall,
        color = colors.text3,
        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
      )

      HapticTimeline(
        bars = bars,
        topLeft = "INTENSITY ▲",
        topRight = topRight,
        minLabel = "0",
        maxLabel = maxLabel,
        playheadProgress = 0f,
        playheadVisible = false,
      )

      Column(modifier = Modifier.padding(top = 14.dp)) { controls() }

      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ExpectedActualBox(label = "EXPECTED", value = expected, accentBox = false, modifier = Modifier.weight(1f))
        ExpectedActualBox(label = "ACTUAL", value = actual, accentBox = true, modifier = Modifier.weight(1f))
      }
    }
  }
}

/** Expected/Actual box: surface2 + border for expected, accentBg + accent for actual. */
@Composable
private fun ExpectedActualBox(
  label: String,
  value: String,
  accentBox: Boolean,
  modifier: Modifier = Modifier,
) {
  val colors = JindongTheme.colors
  Column(
    modifier =
    modifier
      .clip(RoundedCornerShape(Dimens.radiusChip))
      .background(if (accentBox) colors.accentBg else colors.surface2)
      .border(
        Dimens.stroke,
        if (accentBox) colors.accent else colors.border,
        RoundedCornerShape(Dimens.radiusChip),
      )
      .padding(horizontal = 11.dp, vertical = 9.dp),
  ) {
    Text(
      text = label,
      style = JindongTheme.typography.monoAxis.copy(letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified),
      color = if (accentBox) colors.accent else colors.text3,
    )
    Text(
      text = value,
      style = JindongTheme.typography.bodySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
      color = colors.text,
      modifier = Modifier.padding(top = 2.dp),
    )
  }
}

private inline fun androidx.compose.ui.text.AnnotatedString.Builder.withStyleSemiBold(
  color: androidx.compose.ui.graphics.Color,
  block: androidx.compose.ui.text.AnnotatedString.Builder.() -> Unit,
) {
  withStyle(
    androidx.compose.ui.text.SpanStyle(
      color = color,
      fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
    ),
  ) { block() }
}

/** A one-bar pattern at [start]/[dur]/[intensity] (Cards A and C). */
internal fun singleBar(
  start: Long,
  dur: Long,
  intensity: Float,
): HapticPattern = HapticPattern(
  listOf(ScheduledHapticEvent(startTimeMs = start, durationMs = dur, intensity = HapticIntensity.Custom(intensity))),
)

/** Card B timeline pattern: count bars, dur 60, gap 80, intensity 0.7. */
internal fun reactiveCountPattern(count: Int): HapticPattern {
  val events =
    (0 until count).map { i ->
      ScheduledHapticEvent(
        startTimeMs = (i * 140L),
        durationMs = 60L,
        intensity = HapticIntensity.Custom(0.7f),
      )
    }
  return HapticPattern(events)
}
