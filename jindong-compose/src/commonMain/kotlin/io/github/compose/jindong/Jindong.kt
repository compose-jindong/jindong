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
package io.github.compose.jindong

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Recomposer
import io.github.compose.jindong.compose.JindongApplier
import io.github.compose.jindong.core.element.SequenceElement
import io.github.compose.jindong.core.model.HapticPattern
import io.github.compose.jindong.executor.LocalHapticExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Composable that compiles the haptic pattern in [content] and plays it when [keys] change.
 *
 * Like [LaunchedEffect], a change to any key restarts the effect: the pattern is recompiled and
 * played. The reactive behavior follows three rules.
 *
 * 1. Values read inside [content] are frozen at compile time. The pattern is compiled once per key
 *    change through a single-shot composition that never recomposes (see [compilePattern]), so a
 *    state value the block captures is read once and does not update on its own.
 * 2. Keys are the playback trigger. Put a value in [keys] exactly when its change should fire
 *    playback. A slider value passed as a key fires on every drag step; state that only shapes the
 *    pattern belongs inside [content] as a parameter.
 * 3. Cancel-and-restart is best effort. A key change cancels the in-flight playback before starting
 *    the new one, ordered by the manager's state lock. The platform stop calls do not report
 *    completion, so a few milliseconds of physical overlap are possible.
 *
 * To make a value both shape the pattern and re-fire when it changes, pass it as a key and read it
 * inside the block:
 *
 * ```
 * var count by remember { mutableStateOf(0) }
 *
 * Jindong(count) {
 *     Haptic(100.ms)
 *     Haptic(50.ms, intensity = HapticIntensity.STRONG)
 * }
 *
 * Button(onClick = { count++ }) {
 *     Text("Trigger Haptic")
 * }
 * ```
 *
 * To embed a prebuilt pattern and re-fire when it changes, thread it through the keys and place it
 * with [io.github.compose.jindong.dsl.Clip]:
 *
 * ```
 * Jindong(pattern) { Clip(pattern) }
 * ```
 *
 * @param keys Keys that trigger re-execution when changed (like [LaunchedEffect])
 * @param content DSL block defining the haptic pattern
 */
@Composable
fun Jindong(
  vararg keys: Any?,
  content: @Composable JindongScope.() -> Unit,
) {
  val pattern = rememberHapticPattern(*keys) { content() }
  val executor = LocalHapticExecutor.current

  LaunchedEffect(*keys) {
    executor.execute(pattern)
  }
}

/**
 * Internal class that manages the Composition for haptic pattern compilation.
 */
private class JindongCompositionHost : AutoCloseable {
  private val clock = BroadcastFrameClock()
  private val coroutineScope = CoroutineScope(clock)
  private val rootElement = SequenceElement()
  private val applier = JindongApplier(rootElement)
  private val recomposer = Recomposer(clock)
  private val composition = Composition(applier, recomposer)

  init {
    coroutineScope.launch {
      recomposer.runRecomposeAndApplyChanges()
    }
  }

  override fun close() {
    dispose()
  }

  fun setContent(content: @Composable JindongScope.() -> Unit) {
    composition.setContent {
      val scope = JindongScopeImpl()
      scope.content()
    }
  }

  fun sendFrame() {
    clock.sendFrame(0L)
  }

  fun collectEvents(): HapticPattern {
    val events = rootElement.collectEvents(0L)
    return HapticPattern(events)
  }

  fun dispose() {
    composition.dispose()
    recomposer.close()
    coroutineScope.cancel()
  }
}

/**
 * Compiles a haptic DSL block into a [HapticPattern].
 *
 * This function creates an internal Composition to build the node tree,
 * collects events from the tree, and disposes the Composition.
 *
 * @param content The DSL block defining the haptic pattern
 * @return The compiled [HapticPattern] containing scheduled events
 */
internal fun compilePattern(
  content: @Composable JindongScope.() -> Unit,
): HapticPattern = JindongCompositionHost()
  .use { host ->
    host.setContent(content)
    host.sendFrame()
    host.collectEvents()
  }
