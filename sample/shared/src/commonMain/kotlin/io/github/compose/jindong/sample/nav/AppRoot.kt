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
package io.github.compose.jindong.sample.nav

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.compose.jindong.sample.components.JindongScaffold
import io.github.compose.jindong.sample.screens.HomeScreen
import io.github.compose.jindong.sample.screens.IntensityLabScreen
import io.github.compose.jindong.sample.screens.PresetGalleryScreen
import io.github.compose.jindong.sample.screens.ReactiveScreen
import io.github.compose.jindong.sample.screens.RepeatScreen
import io.github.compose.jindong.sample.screens.RepeatWithIndexScreen
import io.github.compose.jindong.sample.screens.SingleHapticScreen
import io.github.compose.jindong.sample.screens.TimingScreen
import io.github.compose.jindong.sample.theme.JindongTheme

/**
 * Single-level navigation host. Home lists the seven modules; tapping one swaps [current], the app
 * bar back chevron returns to Home. The theme follows the system preference unless [darkOverride] is
 * set by the app-bar toggle.
 *
 * Back handling: on a module screen the Android system back gesture/button returns to Home via
 * [PlatformBackHandler] (a no-op on iOS, where the app-bar chevron is the affordance). This avoids
 * the deprecated `androidx.compose.ui.backhandler.BackHandler`, which on iOS requires a
 * navigation-event dispatcher owner that `ComposeUIViewController` does not provide.
 */
@Composable
fun AppRoot() {
  var current by rememberSaveable(stateSaver = ScreenSaver) {
    mutableStateOf<Screen>(Screen.Home)
  }
  var darkOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }
  val dark = darkOverride ?: isSystemInDarkTheme()

  PlatformBackHandler(enabled = current != Screen.Home) { current = Screen.Home }

  JindongTheme(darkTheme = dark) {
    JindongScaffold(
      screen = current,
      isDark = dark,
      onBack = { current = Screen.Home },
      onToggleTheme = { darkOverride = !dark },
    ) {
      when (current) {
        Screen.Home -> HomeScreen(onModuleClick = { current = it })
        Screen.Single -> SingleHapticScreen()
        Screen.Intensity -> IntensityLabScreen()
        Screen.Timing -> TimingScreen()
        Screen.Repeat -> RepeatScreen()
        Screen.RepeatIdx -> RepeatWithIndexScreen()
        Screen.Preset -> PresetGalleryScreen()
        Screen.Reactive -> ReactiveScreen()
      }
    }
  }
}

/** Saves a [Screen] by its [Screen.routeId] and restores it by looking up Home + the modules. */
private val ScreenSaver: Saver<Screen, String> =
  Saver(
    save = { it.routeId },
    restore = { id -> (listOf(Screen.Home) + Screen.modules).firstOrNull { it.routeId == id } ?: Screen.Home },
  )
