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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
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

    test("execute should play pattern through the handle path and wait for completion") {
      runTest {
        // execute() drives playback via executeAsync so the in-flight vibration stays reachable via
        // currentHandle (issue #93); it still suspends until the handle's playback window elapses.
        val playbackMs = 10L
        val timedExecutor = FakeHapticExecutor(
          playbackDuration = playbackMs.milliseconds,
          timeSource = testTimeSource,
        )
        HapticManager.initializeExecutor(timedExecutor)
        val pattern = buildHapticPattern { haptic(100.ms) }

        val before = testScheduler.currentTime
        HapticManager.execute(pattern)
        val elapsed = testScheduler.currentTime - before

        assertSoftly {
          timedExecutor.asyncExecutedPatterns shouldHaveSize 1
          timedExecutor.asyncExecutedPatterns[0] shouldBe pattern
          // Suspended for the whole playback window (poll granularity may round up the final step).
          (elapsed >= playbackMs) shouldBe true
        }
      }
    }

    test("execute should handle empty pattern") {
      runTest {
        val timedExecutor = FakeHapticExecutor(
          playbackDuration = 0.milliseconds,
          timeSource = testTimeSource,
        )
        HapticManager.initializeExecutor(timedExecutor)
        val emptyPattern = buildHapticPattern { }

        shouldNotThrowAny {
          HapticManager.execute(emptyPattern)
        }

        assertSoftly {
          timedExecutor.asyncExecutedPatterns shouldHaveSize 1
          timedExecutor.asyncExecutedPatterns[0].events.shouldBeEmpty()
        }
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

    test("executeAsync should cancel an execute() playback that is still in flight") {
      // Regression for #93: while execute() is mid-playback, an executeAsync must find and cancel the
      // in-flight handle (the two share currentHandle) instead of firing a second overlapping
      // vibration. Reverting execute() to clear currentHandle before playback makes this RED.
      runTest {
        // Long window so the execute() playback is still active when executeAsync lands.
        val timedExecutor = FakeHapticExecutor(
          playbackDuration = 1_000.milliseconds,
          timeSource = testTimeSource,
        )
        HapticManager.initializeExecutor(timedExecutor)

        val longPattern = buildHapticPattern { haptic(500.ms) }
        val otherPattern = buildHapticPattern { haptic(100.ms) }

        // Start the suspending execute() in the background; let it install its handle and begin its
        // await, then interleave the async call while it is provably still in flight.
        val executeJob = launch { HapticManager.execute(longPattern) }
        advanceTimeBy(10)
        val executeHandle = timedExecutor.issuedHandles.single()
        executeHandle.isActive shouldBe true

        val asyncHandle = HapticManager.executeAsync(otherPattern)

        assertSoftly {
          // The in-flight execute() playback was cancelled, not left overlapping.
          executeHandle.cancelled shouldBe true
          executeHandle.isActive shouldBe false
          // Both playbacks reached the executor; only the async one is still live.
          timedExecutor.asyncExecutedPatterns shouldBe listOf(longPattern, otherPattern)
          asyncHandle.isActive shouldBe true
        }

        // execute()'s cleanup unwinds once its handle is cancelled; it must not cancel the newer
        // async handle that took over the currentHandle slot.
        executeJob.join()
        asyncHandle.isActive shouldBe true
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
