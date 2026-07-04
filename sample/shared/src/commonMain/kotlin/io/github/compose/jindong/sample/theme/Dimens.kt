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
package io.github.compose.jindong.sample.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing, radius and stroke tokens for the harness. Prototype px map to dp 1:1; touch-target and
 * accessibility minimums override the visual sizes where the handoff calls for it.
 */
object Dimens {
  // Layout
  val screenPadH = 16.dp
  val bodyTop = 18.dp
  val bodyBottom = 48.dp
  val appBarHeight = 56.dp
  val plotHeight = 120.dp

  // Stroke / touch
  val stroke = 1.dp
  val minTouch = 48.dp

  // Radii
  val radiusSmall = 8.dp
  val radiusIcon = 8.dp
  val radiusChip = 9.dp
  val radiusCard = 11.dp
  val radiusButton = 11.dp
  val radiusBigCard = 13.dp
  val radiusTimeline = 12.dp

  // Padding / gaps
  val cardPadding = 14.dp
  val rowGapSmall = 7.dp
  val rowGapMedium = 11.dp
  val rowGapLarge = 14.dp

  // Component metrics
  val toggleSize = 30.dp
  val dotSize = 6.dp
  val nodeDotSize = 7.dp
  val barCornerRadius = 3.dp
  val barMinWidth = 3.dp
  val axisStroke = 1.dp
  val playheadStroke = 1.5.dp
}
