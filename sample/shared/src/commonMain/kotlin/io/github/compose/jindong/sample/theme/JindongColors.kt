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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Semantic color tokens for the Jindong sample harness. Token names mirror the design handoff CSS
 * variables 1:1 so the prototype and the Compose port stay in sync.
 *
 * [accent] is reserved for data visualization (timeline bars) and active states; the primary button
 * uses the inverted [btnBg] / [btnText] pair, not [accent].
 */
@Immutable
data class JindongColors(
  val bg: Color,
  val surface: Color,
  val surface2: Color,
  val border: Color,
  val border2: Color,
  val divider: Color,
  val text: Color,
  val text2: Color,
  val text3: Color,
  val accent: Color,
  val accentBg: Color,
  val ok: Color,
  val warn: Color,
  val btnBg: Color,
  val btnText: Color,
)

fun lightColors(): JindongColors = JindongColors(
  bg = Color(0xFFFFFFFF),
  surface = Color(0xFFFFFFFF),
  surface2 = Color(0xFFFAFAFA),
  border = Color(0xFFECECEC),
  border2 = Color(0xFFE4E4E7),
  divider = Color(0xFFF4F4F5),
  text = Color(0xFF18181B),
  text2 = Color(0xFF52525B),
  text3 = Color(0xFFA1A1AA),
  accent = Color(0xFF2F5FE0),
  // rgba(47,95,224,0.07) baked: 0.07*255 ~= 18 = 0x12
  accentBg = Color(0x122F5FE0),
  ok = Color(0xFF16A34A),
  warn = Color(0xFFD97706),
  btnBg = Color(0xFF18181B),
  btnText = Color(0xFFFFFFFF),
)

fun darkColors(): JindongColors = JindongColors(
  bg = Color(0xFF101012),
  surface = Color(0xFF161618),
  surface2 = Color(0xFF16161A),
  border = Color(0xFF232327),
  border2 = Color(0xFF2E2E32),
  divider = Color(0xFF1C1C1F),
  text = Color(0xFFF4F4F5),
  text2 = Color(0xFFA1A1AA),
  text3 = Color(0xFF71717A),
  accent = Color(0xFF6B93FF),
  // rgba(107,147,255,0.14) baked: 0.14*255 ~= 36 = 0x24
  accentBg = Color(0x246B93FF),
  ok = Color(0xFF34D399),
  warn = Color(0xFFFBBF24),
  btnBg = Color(0xFFF4F4F5),
  btnText = Color(0xFF101012),
)
