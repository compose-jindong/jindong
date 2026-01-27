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

package io.github.compose.jindong.compose

import androidx.compose.runtime.ExperimentalComposeApi
import io.github.compose.jindong.core.element.HapticElement
import io.github.compose.jindong.core.element.SequenceElement
import io.github.compose.jindong.core.element.VibrationElement
import io.github.compose.jindong.core.model.HapticIntensity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Tests for [JindongApplier] using Kotest FunSpec.
 *
 * Verifies correct tree manipulation operations:
 * - Insert (top-down and bottom-up)
 * - Remove (single and multiple elements)
 * - Move (forward, backward, multiple elements)
 * - Clear
 * - Navigation (down/up)
 */
class JindongApplierTest :
  FunSpec({

    fun createElement(durationMs: Long): VibrationElement = VibrationElement(durationMs = durationMs, intensity = HapticIntensity.HIGH)

    lateinit var root: HapticElement
    lateinit var applier: JindongApplier

    beforeEach {
      root = SequenceElement()
      applier = JindongApplier(root)
    }

    context("JindongApplier insertTopDown") {
      test("should add element at index 0") {
        val element = createElement(100)

        applier.insertTopDown(0, element)

        root.children shouldContainExactly listOf(element)
      }

      test("should insert at beginning and push existing elements") {
        val element1 = createElement(100)
        val element2 = createElement(200)

        applier.insertTopDown(0, element1)
        applier.insertTopDown(0, element2) // Push element1 to index 1

        root.children shouldContainExactly listOf(element2, element1)
      }

      test("should insert in the middle") {
        val element1 = createElement(100)
        val element2 = createElement(200)
        val element3 = createElement(300)

        applier.insertTopDown(0, element1)
        applier.insertTopDown(1, element2)
        applier.insertTopDown(1, element3) // Insert between element1 and element2

        root.children shouldContainExactly listOf(element1, element3, element2)
      }

      test("should append to end") {
        val element1 = createElement(100)
        val element2 = createElement(200)

        applier.insertTopDown(0, element1)
        applier.insertTopDown(1, element2)

        root.children shouldContainExactly listOf(element1, element2)
      }
    }

    context("JindongApplier remove") {
      test("should remove single element at index") {
        val element1 = createElement(100)
        val element2 = createElement(200)
        val element3 = createElement(300)

        applier.insertTopDown(0, element1)
        applier.insertTopDown(1, element2)
        applier.insertTopDown(2, element3)

        applier.remove(index = 1, count = 1)

        root.children shouldContainExactly listOf(element1, element3)
      }

      test("should remove multiple consecutive elements") {
        val elements = (1..5).map { createElement(it * 100L) }
        elements.forEach { applier.insertTopDown(root.children.size, it) }

        applier.remove(index = 1, count = 3)

        root.children shouldContainExactly listOf(elements[0], elements[4])
      }

      test("should remove from start") {
        val element1 = createElement(100)
        val element2 = createElement(200)

        applier.insertTopDown(0, element1)
        applier.insertTopDown(1, element2)

        applier.remove(index = 0, count = 1)

        root.children shouldContainExactly listOf(element2)
      }

      test("should remove from end") {
        val element1 = createElement(100)
        val element2 = createElement(200)

        applier.insertTopDown(0, element1)
        applier.insertTopDown(1, element2)

        applier.remove(index = 1, count = 1)

        root.children shouldContainExactly listOf(element1)
      }
    }

    context("JindongApplier move") {
      context("moving single element") {
        test("should move element forward") {
          val element1 = createElement(100)
          val element2 = createElement(200)
          val element3 = createElement(300)

          applier.insertTopDown(0, element1)
          applier.insertTopDown(1, element2)
          applier.insertTopDown(2, element3)

          applier.move(from = 0, to = 2, count = 1)

          root.children shouldContainExactly listOf(element2, element3, element1)
        }

        test("should move element backward") {
          val element1 = createElement(100)
          val element2 = createElement(200)
          val element3 = createElement(300)

          applier.insertTopDown(0, element1)
          applier.insertTopDown(1, element2)
          applier.insertTopDown(2, element3)

          applier.move(from = 2, to = 0, count = 1)

          root.children shouldContainExactly listOf(element3, element1, element2)
        }

        test("should move to adjacent position") {
          val element1 = createElement(100)
          val element2 = createElement(200)
          val element3 = createElement(300)

          applier.insertTopDown(0, element1)
          applier.insertTopDown(1, element2)
          applier.insertTopDown(2, element3)

          applier.move(from = 0, to = 1, count = 1)

          root.children shouldContainExactly listOf(element2, element1, element3)
        }

        test("should do nothing when from equals to") {
          val element1 = createElement(100)
          val element2 = createElement(200)

          applier.insertTopDown(0, element1)
          applier.insertTopDown(1, element2)

          applier.move(from = 1, to = 1, count = 1)

          root.children shouldContainExactly listOf(element1, element2)
        }
      }

      context("moving multiple elements") {
        test("should move multiple elements forward") {
          val elements = (1..5).map { createElement(it * 100L) }
          elements.forEach { applier.insertTopDown(root.children.size, it) }

          applier.move(from = 0, to = 3, count = 2)

          root.children shouldContainExactly listOf(
            elements[2],
            elements[3],
            elements[4],
            elements[0],
            elements[1],
          )
        }

        test("should move multiple elements backward") {
          val elements = (1..5).map { createElement(it * 100L) }
          elements.forEach { applier.insertTopDown(root.children.size, it) }

          applier.move(from = 3, to = 1, count = 2)

          root.children shouldContainExactly listOf(
            elements[0],
            elements[3],
            elements[4],
            elements[1],
            elements[2],
          )
        }

        test("should throw exception when target index is out of bounds") {
          val elements = (1..5).map { createElement(it * 100L) }
          elements.forEach { applier.insertTopDown(root.children.size, it) }

          shouldThrow<IndexOutOfBoundsException> {
            applier.move(from = 0, to = 10, count = 1)
          }
        }
      }

      context("complex move scenarios") {
        test("should handle sequential move operations correctly") {
          val elements = (1..7).map { createElement(it * 100L) }
          elements.forEach { applier.insertTopDown(root.children.size, it) }

          // Original: [0, 1, 2, 3, 4, 5, 6]
          applier.move(from = 0, to = 3, count = 2) // [2, 3, 4, 0, 1, 5, 6]
          applier.move(from = 5, to = 1, count = 2) // [2, 5, 6, 3, 4, 0, 1]
          applier.move(from = 6, to = 0, count = 1) // [1, 2, 5, 6, 3, 4, 0]

          root.children shouldContainExactly listOf(
            elements[1],
            elements[2],
            elements[5],
            elements[6],
            elements[3],
            elements[4],
            elements[0],
          )
        }
      }
    }

    context("JindongApplier clear") {
      test("should remove all children from root") {
        val element1 = createElement(100)
        val element2 = createElement(200)
        val element3 = createElement(300)

        applier.insertTopDown(0, element1)
        applier.insertTopDown(1, element2)
        applier.insertTopDown(2, element3)

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
        val containerElement = SequenceElement()
        val leafElement = createElement(100)

        applier.insertTopDown(0, containerElement)
        applier.down(containerElement)
        applier.insertTopDown(0, leafElement)

        root.children shouldContainExactly listOf(containerElement)
        containerElement.children shouldContainExactly listOf(leafElement)

        applier.up()

        applier.current shouldBe root
      }

      test("should build nested tree structure") {
        val sequence1 = SequenceElement()
        val sequence2 = SequenceElement()
        val element1 = createElement(100)
        val element2 = createElement(200)
        val element3 = createElement(300)

        // Build: root -> [sequence1 -> [element1, sequence2 -> [element2]], element3]
        applier.insertTopDown(0, sequence1)
        applier.insertTopDown(1, element3)

        applier.down(sequence1)
        applier.insertTopDown(0, element1)
        applier.insertTopDown(1, sequence2)

        applier.down(sequence2)
        applier.insertTopDown(0, element2)

        root.children shouldContainExactly listOf(sequence1, element3)
        sequence1.children shouldContainExactly listOf(element1, sequence2)
        sequence2.children shouldContainExactly listOf(element2)
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
        val elements = (1..10).map { createElement(it * 100L) }

        // Add all elements
        elements.forEach { applier.insertTopDown(root.children.size, it) }
        root.children.size shouldBe 10

        // Remove some
        applier.remove(2, 3)
        root.children.size shouldBe 7

        // Move some
        applier.move(0, 5, 2)
        root.children.size shouldBe 7

        // Add more
        val newElement = createElement(999)
        applier.insertTopDown(0, newElement)
        root.children.size shouldBe 8

        // Clear all
        applier.clear()
        root.children.shouldBeEmpty()
      }
    }
  })
