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

/**
 * Harness destinations. [Home] is the hub (index 0); [modules] lists the feature screens that
 * Home links to, in display order. [index] doubles as the app-bar counter value ("NN / <count>").
 */
sealed interface Screen {
  val routeId: String
  val title: String
  val index: Int
  val subtitle: String

  data object Home : Screen {
    override val routeId = "home"
    override val title = "Jindong"
    override val index = 0
    override val subtitle = "Device capability + module hub"
  }

  data object Single : Screen {
    override val routeId = "single"
    override val title = "Single Haptic"
    override val index = 1
    override val subtitle = "Duration × intensity, isolated"
  }

  data object Intensity : Screen {
    override val routeId = "intensity"
    override val title = "Intensity Lab"
    override val index = 2
    override val subtitle = "Compare 5 strength levels"
  }

  data object Timing : Screen {
    override val routeId = "timing"
    override val title = "Timing"
    override val index = 3
    override val subtitle = "Delay & sequence rhythm"
  }

  data object Repeat : Screen {
    override val routeId = "repeat"
    override val title = "Repeat"
    override val index = 4
    override val subtitle = "Static N-times repeat"
  }

  data object RepeatIdx : Screen {
    override val routeId = "repeat-index"
    override val title = "Repeat w/ Index"
    override val index = 5
    override val subtitle = "Per-cycle intensity ramp"
  }

  data object Preset : Screen {
    override val routeId = "preset"
    override val title = "Preset Gallery"
    override val index = 6
    override val subtitle = "10 real-world patterns"
  }

  data object Reactive : Screen {
    override val routeId = "reactive"
    override val title = "Reactive"
    override val index = 7
    override val subtitle = "State-driven · no buttons ★"
  }

  data object Algebra : Screen {
    override val routeId = "algebra"
    override val title = "Algebra"
    override val index = 8
    override val subtitle = "Transform a pattern, see the diff"
  }

  companion object {
    val modules: List<Screen> =
      listOf(Single, Intensity, Timing, Repeat, RepeatIdx, Preset, Reactive, Algebra)
  }
}
