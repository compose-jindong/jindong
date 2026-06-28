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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

private val LocalJindongColors =
  staticCompositionLocalOf<JindongColors> {
    error("JindongColors not provided. Wrap content in JindongTheme { }.")
  }

private val LocalJindongTypography =
  staticCompositionLocalOf<JindongTypography> {
    error("JindongTypography not provided. Wrap content in JindongTheme { }.")
  }

/**
 * Root theme for the Jindong sample harness. Provides [JindongColors] and [JindongTypography] and
 * paints [JindongColors.bg] as the screen background.
 *
 * The [darkTheme] flag is driven by the caller (AppRoot) so the manual toggle can override the
 * system preference per the handoff.
 */
@Composable
fun JindongTheme(
  darkTheme: Boolean,
  content: @Composable () -> Unit,
) {
  val colors = if (darkTheme) darkColors() else lightColors()
  val typography = jindongTypography(sans = instrumentSansFamily(), mono = ibmPlexMonoFamily())
  CompositionLocalProvider(
    LocalJindongColors provides colors,
    LocalJindongTypography provides typography,
  ) {
    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
      content()
    }
  }
}

object JindongTheme {
  val colors: JindongColors
    @Composable
    @ReadOnlyComposable
    get() = LocalJindongColors.current

  val typography: JindongTypography
    @Composable
    @ReadOnlyComposable
    get() = LocalJindongTypography.current
}
