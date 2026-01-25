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
package io.github.compose.jindong.core

import io.github.compose.jindong.core.dsl.buildHapticPattern
import io.github.compose.jindong.core.fake.FakeHapticExecutor
import io.github.compose.jindong.core.model.HapticIntensity
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class HapticManagerTest :
  FunSpec({
    lateinit var fakeExecutor: FakeHapticExecutor

    beforeEach {
      fakeExecutor = FakeHapticExecutor()
      HapticManager.initializeExecutor(fakeExecutor)
    }

    afterEach {
      HapticManager.release()
    }

    test("isSupported should return true when executor supports haptics") {
      HapticManager.isSupported shouldBe true
    }

    test("isSupported should return false when executor does not support haptics") {
      HapticManager.initializeExecutor(FakeHapticExecutor(isSupported = false))

      HapticManager.isSupported shouldBe false
    }

    test("execute should execute pattern synchronously") {
      val pattern = buildHapticPattern {
        haptic(100.ms)
      }

      HapticManager.execute(pattern)

      assertSoftly {
        fakeExecutor.executedPatterns shouldHaveSize 1
        fakeExecutor.executedPatterns[0] shouldBe pattern
      }
    }

    test("execute should handle empty pattern") {
      val emptyPattern = buildHapticPattern { }

      HapticManager.execute(emptyPattern)

      assertSoftly {
        fakeExecutor.executedPatterns shouldHaveSize 1
        fakeExecutor.executedPatterns[0].events.shouldBeEmpty()
      }
    }

    test("executeAsync should execute pattern and return handle") {
      val pattern = buildHapticPattern {
        haptic(100.ms, HapticIntensity.STRONG)
      }

      val handle = HapticManager.executeAsync(pattern)

      assertSoftly {
        fakeExecutor.asyncExecutedPatterns shouldHaveSize 1
        fakeExecutor.asyncExecutedPatterns[0] shouldBe pattern
        handle.isActive shouldBe true
      }
    }

    test("executeAsync should cancel previous handle when executing new pattern") {
      val pattern1 = buildHapticPattern { haptic(100.ms) }
      val pattern2 = buildHapticPattern { haptic(200.ms) }

      val handle1 = HapticManager.executeAsync(pattern1)
      HapticManager.executeAsync(pattern2)

      assertSoftly {
        fakeExecutor.asyncExecutedPatterns shouldHaveSize 2
        handle1.isActive shouldBe false
      }
    }

    test("cancel should cancel current execution") {
      val pattern = buildHapticPattern { haptic(100.ms) }
      val handle = HapticManager.executeAsync(pattern)

      HapticManager.cancel()

      handle.isActive shouldBe false
    }

    test("cancel should not throw when no active execution exists") {
      shouldNotThrowAny {
        HapticManager.cancel()
      }
    }

    test("release should release executor resources") {
      HapticManager.release()

      fakeExecutor.releaseCalled shouldBe true
    }

    test("release should cancel active execution") {
      val pattern = buildHapticPattern { haptic(100.ms) }
      val handle = HapticManager.executeAsync(pattern)

      HapticManager.release()

      assertSoftly {
        handle.isActive shouldBe false
        fakeExecutor.releaseCalled shouldBe true
      }
    }

    test("should allow reuse after release") {
      val firstExecutor = FakeHapticExecutor()
      HapticManager.initializeExecutor(firstExecutor)
      HapticManager.release()

      val secondExecutor = FakeHapticExecutor()
      HapticManager.initializeExecutor(secondExecutor)
      val pattern = buildHapticPattern { haptic(100.ms) }
      HapticManager.executeAsync(pattern)

      assertSoftly {
        firstExecutor.releaseCalled shouldBe true
        firstExecutor.asyncExecutedPatterns.shouldBeEmpty()
        secondExecutor.asyncExecutedPatterns shouldHaveSize 1
      }
    }
  })
