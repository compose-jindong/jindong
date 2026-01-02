package io.github.jindong.compose

import androidx.compose.runtime.AbstractApplier
import io.github.jindong.node.HapticNode

/**
 * Applier that manages the haptic node tree for Compose Runtime.
 *
 * This applier is responsible for:
 * - Adding nodes to the tree ([insertTopDown], [insertBottomUp])
 * - Removing nodes from the tree ([remove])
 * - Moving nodes within the tree ([move])
 * - Clearing the tree ([onClear])
 *
 * The applier maintains a tree of [io.github.jindong.node.HapticNode]s which will be traversed
 * to collect [ScheduledHapticEvent]s for playback.
 *
 * @param root The root node of the composition tree
 */
internal class JindongApplier(
  root: HapticNode,
) : AbstractApplier<HapticNode>(root) {

  override fun insertTopDown(index: Int, instance: HapticNode) {
    current.children.add(index, instance)
  }

  override fun insertBottomUp(index: Int, instance: HapticNode) {
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
