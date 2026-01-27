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
package io.github.compose.jindong.compose

import androidx.compose.runtime.AbstractApplier
import io.github.compose.jindong.core.element.HapticElement

/**
 * Applier that manages the haptic element tree for Compose Runtime.
 *
 * This applier is responsible for:
 * - Adding elements to the tree ([insertTopDown], [insertBottomUp])
 * - Removing elements from the tree ([remove])
 * - Moving elements within the tree ([move])
 * - Clearing the tree ([onClear])
 *
 * The applier maintains a tree of [HapticElement]s which will be traversed
 * to collect [io.github.compose.jindong.core.model.ScheduledHapticEvent]s for playback.
 *
 * @param root The root element of the composition tree
 */
internal class JindongApplier(
  root: HapticElement,
) : AbstractApplier<HapticElement>(root) {

  override fun insertTopDown(index: Int, instance: HapticElement) {
    current.children.add(index, instance)
  }

  override fun insertBottomUp(index: Int, instance: HapticElement) {
    // Insertion is already handled in insertTopDown
  }

  override fun remove(index: Int, count: Int) {
    current.children.subList(index, index + count).clear()
  }

  override fun move(from: Int, to: Int, count: Int) {
    if (from == to) return

    val items = current.children.subList(from, from + count).toList()
    current.children.subList(from, from + count).clear()
    current.children.addAll(to, items)
  }

  override fun onClear() {
    root.children.clear()
  }
}
