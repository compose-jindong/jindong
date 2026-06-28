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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.compose.jindong.sample.Res
import io.github.compose.jindong.sample.ibm_plex_mono_medium
import io.github.compose.jindong.sample.ibm_plex_mono_regular
import io.github.compose.jindong.sample.ibm_plex_mono_semibold
import io.github.compose.jindong.sample.instrument_sans_variable
import org.jetbrains.compose.resources.Font

/**
 * Role-based text styles for the harness. Numbers and uppercase section labels that the user must be
 * able to verify use the [JindongTypography.monoValue]/etc. mono roles; everything else uses sans.
 *
 * Build with [jindongTypography] passing the families from [instrumentSansFamily] /
 * [ibmPlexMonoFamily].
 */
@Immutable
data class JindongTypography(
  val intro: TextStyle,
  val cardTitle: TextStyle,
  val sectionLabel: TextStyle,
  val monoValue: TextStyle,
  val monoMicro: TextStyle,
  val monoAxis: TextStyle,
  val appBarTitle: TextStyle,
  val wordmark: TextStyle,
  val chipLabel: TextStyle,
  val bodySmall: TextStyle,
  val rowTitle: TextStyle,
)

/**
 * Instrument Sans is a single variable font (wght 400-700). Each weight is a [Font] entry over the
 * same [Res.font.instrument_sans_variable] resource, selecting the axis via
 * [FontVariation.weight]. This family is the single swap point if a weight ever fails to resolve on
 * a target.
 */
@Composable
fun instrumentSansFamily(): FontFamily = FontFamily(
  Font(
    Res.font.instrument_sans_variable,
    weight = FontWeight.Normal,
    style = FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(400)),
  ),
  Font(
    Res.font.instrument_sans_variable,
    weight = FontWeight.Medium,
    style = FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(500)),
  ),
  Font(
    Res.font.instrument_sans_variable,
    weight = FontWeight.SemiBold,
    style = FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(600)),
  ),
  Font(
    Res.font.instrument_sans_variable,
    weight = FontWeight.Bold,
    style = FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(700)),
  ),
)

/**
 * IBM Plex Mono ships as three static files mapped to W400 / W500 / W600.
 */
@Composable
fun ibmPlexMonoFamily(): FontFamily = FontFamily(
  Font(Res.font.ibm_plex_mono_regular, weight = FontWeight.Normal),
  Font(Res.font.ibm_plex_mono_medium, weight = FontWeight.Medium),
  Font(Res.font.ibm_plex_mono_semibold, weight = FontWeight.SemiBold),
)

@Composable
fun jindongTypography(
  sans: FontFamily,
  mono: FontFamily,
): JindongTypography = JindongTypography(
  intro =
  TextStyle(
    fontFamily = sans,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = (13 * 1.55).sp,
  ),
  cardTitle =
  TextStyle(
    fontFamily = sans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
  ),
  sectionLabel =
  TextStyle(
    fontFamily = mono,
    fontWeight = FontWeight.Medium,
    fontSize = 9.sp,
    letterSpacing = 0.16.em,
  ),
  monoValue =
  TextStyle(
    fontFamily = mono,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
  ),
  monoMicro =
  TextStyle(
    fontFamily = mono,
    fontWeight = FontWeight.Normal,
    fontSize = 10.sp,
  ),
  monoAxis =
  TextStyle(
    fontFamily = mono,
    fontWeight = FontWeight.Normal,
    fontSize = 9.sp,
    letterSpacing = 0.12.em,
  ),
  appBarTitle =
  TextStyle(
    fontFamily = sans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    letterSpacing = (-0.01).em,
  ),
  wordmark =
  TextStyle(
    fontFamily = sans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 19.sp,
  ),
  chipLabel =
  TextStyle(
    fontFamily = sans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
  ),
  bodySmall =
  TextStyle(
    fontFamily = sans,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
  ),
  rowTitle =
  TextStyle(
    fontFamily = sans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
  ),
)
