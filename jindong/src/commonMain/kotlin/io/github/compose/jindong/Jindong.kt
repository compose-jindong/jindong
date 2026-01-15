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
import io.github.compose.jindong.executor.LocalHapticExecutor
import io.github.compose.jindong.model.HapticPattern
import io.github.compose.jindong.node.SequenceNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Composable that triggers haptic pattern execution when keys change.
 *
 * This works similarly to [LaunchedEffect] - when any of the keys change,
 * the haptic pattern defined in [content] is compiled and executed.
 *
 * Example:
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
 * @param keys Keys that trigger re-execution when changed (like [LaunchedEffect])
 * @param content DSL block defining the haptic pattern
 */
@Composable
fun Jindong(
  vararg keys: Any?,
  content: @Composable JindongScope.() -> Unit,
) {
  val pattern = rememberHapticPattern(content)
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
  private val rootNode = SequenceNode()
  private val applier = JindongApplier(rootNode)
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
    val events = rootNode.collectEvents(0L)
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
