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
@file:OptIn(ExperimentalComposeApi::class)

package io.github.jindong.compose

import androidx.compose.runtime.ExperimentalComposeApi
import io.github.jindong.node.HapticEventNode
import io.github.jindong.node.HapticNode
import io.github.jindong.node.SequenceNode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Tests for [JindongApplier] using Kotest FunSpec.
 *
 * Verifies correct tree manipulation operations:
 * - Insert (top-down and bottom-up)
 * - Remove (single and multiple nodes)
 * - Move (forward, backward, multiple nodes)
 * - Clear
 * - Navigation (down/up)
 */
class JindongApplierTest :
  FunSpec({

    fun createNode(durationMs: Long): HapticEventNode =
      HapticEventNode(durationMs = durationMs, intensity = 1.0f)

    lateinit var root: HapticNode
    lateinit var applier: JindongApplier

    beforeEach {
      root = SequenceNode()
      applier = JindongApplier(root)
    }

    context("JindongApplier insertTopDown") {
      test("should add node at index 0") {
        val node = createNode(100)

        applier.insertTopDown(0, node)

        root.children shouldContainExactly listOf(node)
      }

      test("should insert at beginning and push existing nodes") {
        val node1 = createNode(100)
        val node2 = createNode(200)

        applier.insertTopDown(0, node1)
        applier.insertTopDown(0, node2) // Push node1 to index 1

        root.children shouldContainExactly listOf(node2, node1)
      }

      test("should insert in the middle") {
        val node1 = createNode(100)
        val node2 = createNode(200)
        val node3 = createNode(300)

        applier.insertTopDown(0, node1)
        applier.insertTopDown(1, node2)
        applier.insertTopDown(1, node3) // Insert between node1 and node2

        root.children shouldContainExactly listOf(node1, node3, node2)
      }

      test("should append to end") {
        val node1 = createNode(100)
        val node2 = createNode(200)

        applier.insertTopDown(0, node1)
        applier.insertTopDown(1, node2)

        root.children shouldContainExactly listOf(node1, node2)
      }
    }

    context("JindongApplier remove") {
      test("should remove single node at index") {
        val node1 = createNode(100)
        val node2 = createNode(200)
        val node3 = createNode(300)

        applier.insertTopDown(0, node1)
        applier.insertTopDown(1, node2)
        applier.insertTopDown(2, node3)

        applier.remove(index = 1, count = 1)

        root.children shouldContainExactly listOf(node1, node3)
      }

      test("should remove multiple consecutive nodes") {
        val nodes = (1..5).map { createNode(it * 100L) }
        nodes.forEach { applier.insertTopDown(root.children.size, it) }

        applier.remove(index = 1, count = 3)

        root.children shouldContainExactly listOf(nodes[0], nodes[4])
      }

      test("should remove from start") {
        val node1 = createNode(100)
        val node2 = createNode(200)

        applier.insertTopDown(0, node1)
        applier.insertTopDown(1, node2)

        applier.remove(index = 0, count = 1)

        root.children shouldContainExactly listOf(node2)
      }

      test("should remove from end") {
        val node1 = createNode(100)
        val node2 = createNode(200)

        applier.insertTopDown(0, node1)
        applier.insertTopDown(1, node2)

        applier.remove(index = 1, count = 1)

        root.children shouldContainExactly listOf(node1)
      }
    }

    context("JindongApplier move") {
      context("moving single node") {
        test("should move node forward") {
          val node1 = createNode(100)
          val node2 = createNode(200)
          val node3 = createNode(300)

          applier.insertTopDown(0, node1)
          applier.insertTopDown(1, node2)
          applier.insertTopDown(2, node3)

          applier.move(from = 0, to = 2, count = 1)

          root.children shouldContainExactly listOf(node2, node3, node1)
        }

        test("should move node backward") {
          val node1 = createNode(100)
          val node2 = createNode(200)
          val node3 = createNode(300)

          applier.insertTopDown(0, node1)
          applier.insertTopDown(1, node2)
          applier.insertTopDown(2, node3)

          applier.move(from = 2, to = 0, count = 1)

          root.children shouldContainExactly listOf(node3, node1, node2)
        }

        test("should move to adjacent position") {
          val node1 = createNode(100)
          val node2 = createNode(200)
          val node3 = createNode(300)

          applier.insertTopDown(0, node1)
          applier.insertTopDown(1, node2)
          applier.insertTopDown(2, node3)

          applier.move(from = 0, to = 1, count = 1)

          root.children shouldContainExactly listOf(node2, node1, node3)
        }

        test("should do nothing when from equals to") {
          val node1 = createNode(100)
          val node2 = createNode(200)

          applier.insertTopDown(0, node1)
          applier.insertTopDown(1, node2)

          applier.move(from = 1, to = 1, count = 1)

          root.children shouldContainExactly listOf(node1, node2)
        }
      }

      context("moving multiple nodes") {
        test("should move multiple nodes forward") {
          val nodes = (1..5).map { createNode(it * 100L) }
          nodes.forEach { applier.insertTopDown(root.children.size, it) }

          applier.move(from = 0, to = 3, count = 2)

          root.children shouldContainExactly listOf(
            nodes[2],
            nodes[3],
            nodes[4],
            nodes[0],
            nodes[1],
          )
        }

        test("should move multiple nodes backward") {
          val nodes = (1..5).map { createNode(it * 100L) }
          nodes.forEach { applier.insertTopDown(root.children.size, it) }

          applier.move(from = 3, to = 1, count = 2)

          root.children shouldContainExactly listOf(
            nodes[0],
            nodes[3],
            nodes[4],
            nodes[1],
            nodes[2],
          )
        }
      }

      context("complex move scenarios") {
        test("should handle sequential move operations correctly") {
          val nodes = (1..7).map { createNode(it * 100L) }
          nodes.forEach { applier.insertTopDown(root.children.size, it) }

          // Original: [0, 1, 2, 3, 4, 5, 6]
          applier.move(from = 0, to = 3, count = 2) // [2, 3, 4, 0, 1, 5, 6]
          applier.move(from = 5, to = 1, count = 2) // [2, 5, 6, 3, 4, 0, 1]
          applier.move(from = 6, to = 0, count = 1) // [1, 2, 5, 6, 3, 4, 0]

          root.children shouldContainExactly listOf(
            nodes[1],
            nodes[2],
            nodes[5],
            nodes[6],
            nodes[3],
            nodes[4],
            nodes[0],
          )
        }
      }
    }

    context("JindongApplier clear") {
      test("should remove all children from root") {
        val node1 = createNode(100)
        val node2 = createNode(200)
        val node3 = createNode(300)

        applier.insertTopDown(0, node1)
        applier.insertTopDown(1, node2)
        applier.insertTopDown(2, node3)

        root.children.size shouldBe 3

        applier.clear()

        root.children.shouldBeEmpty()
      }

      test("should handle already empty root") {
        root.children.shouldBeEmpty()

        applier.clear()

        root.children.shouldBeEmpty()
      }
    }

    context("JindongApplier navigation") {
      test("should navigate down and up correctly") {
        val containerNode = SequenceNode()
        val leafNode = createNode(100)

        applier.insertTopDown(0, containerNode)
        applier.down(containerNode)
        applier.insertTopDown(0, leafNode)

        root.children shouldContainExactly listOf(containerNode)
        containerNode.children shouldContainExactly listOf(leafNode)

        applier.up()

        applier.current shouldBe root
      }

      test("should build nested tree structure") {
        val sequence1 = SequenceNode()
        val sequence2 = SequenceNode()
        val node1 = createNode(100)
        val node2 = createNode(200)
        val node3 = createNode(300)

        // Build: root -> [sequence1 -> [node1, sequence2 -> [node2]], node3]
        applier.insertTopDown(0, sequence1)
        applier.insertTopDown(1, node3)

        applier.down(sequence1)
        applier.insertTopDown(0, node1)
        applier.insertTopDown(1, sequence2)

        applier.down(sequence2)
        applier.insertTopDown(0, node2)
        applier.up()

        applier.up()

        root.children shouldContainExactly listOf(sequence1, node3)
        sequence1.children shouldContainExactly listOf(node1, sequence2)
        sequence2.children shouldContainExactly listOf(node2)
      }
    }

    context("JindongApplier edge cases") {
      test("should handle empty operations on empty root") {
        root.children.shouldBeEmpty()

        // These should not throw
        applier.clear()
        root.children.shouldBeEmpty()
      }

      test("should maintain tree integrity after multiple operations") {
        val nodes = (1..10).map { createNode(it * 100L) }

        // Add all nodes
        nodes.forEach { applier.insertTopDown(root.children.size, it) }
        root.children.size shouldBe 10

        // Remove some
        applier.remove(2, 3)
        root.children.size shouldBe 7

        // Move some
        applier.move(0, 5, 2)
        root.children.size shouldBe 7

        // Add more
        val newNode = createNode(999)
        applier.insertTopDown(0, newNode)
        root.children.size shouldBe 8

        // Clear all
        applier.clear()
        root.children.shouldBeEmpty()
      }
    }
  })
