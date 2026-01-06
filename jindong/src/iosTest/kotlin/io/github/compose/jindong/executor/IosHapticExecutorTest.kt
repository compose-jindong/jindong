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
package io.github.compose.jindong.executor

import io.github.compose.jindong.model.HapticIntensity
import io.github.compose.jindong.model.HapticPattern
import io.github.compose.jindong.model.IosHapticParameters
import io.github.compose.jindong.model.ScheduledHapticEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * iOS-specific HapticExecutor tests.
 *
 * These tests run on iOS Simulator where haptics are NOT supported.
 * Tests verify:
 * - Executor creation works without crash
 * - isSupported returns false on simulator
 * - Graceful degradation when haptics unavailable
 * - No crashes on execute/release calls
 *
 * Actual haptic output must be tested manually on real devices.
 */
class IosHapticExecutorTest :
  FunSpec({

    test("createHapticExecutor should return DefaultIosHapticExecutor") {
      val executor = createHapticExecutor(null)

      (executor is DefaultIosHapticExecutor) shouldBe true
    }

    test("isSupported should return false on simulator") {
      val executor = createHapticExecutor(null)

      // Simulator does not support haptics
      executor.isSupported shouldBe false
    }

    test("execute should not crash when haptics not supported") {
      val executor = createHapticExecutor(null)
      val pattern = createTestPattern()

      executor.execute(pattern)
    }

    test("execute with empty pattern should not crash") {
      val executor = createHapticExecutor(null)
      val emptyPattern = HapticPattern.Empty

      executor.execute(emptyPattern)
    }

    test("executeAsync should return inactive handle when not supported") {
      val executor = createHapticExecutor(null)
      val pattern = createTestPattern()

      val handle = executor.executeAsync(pattern)

      handle.isActive shouldBe false
    }

    test("executeAsync with empty pattern should return inactive handle") {
      val executor = createHapticExecutor(null)

      val handle = executor.executeAsync(HapticPattern.Empty)

      handle.isActive shouldBe false
    }

    test("release should not crash") {
      val executor = createHapticExecutor(null)

      // Should not throw
      executor.release()
    }

    test("release can be called multiple times without crash") {
      val executor = createHapticExecutor(null)

      executor.release()
      executor.release()
      executor.release()
    }

    test("execute after release should not crash") {
      val executor = createHapticExecutor(null)
      executor.release()

      // Should handle gracefully
      executor.execute(createTestPattern())
    }

    test("handle cancel should not crash when already inactive") {
      val executor = createHapticExecutor(null)
      val handle = executor.executeAsync(createTestPattern())

      handle.cancel()
      handle.cancel()

      handle.isActive shouldBe false
    }

    test("pattern with iOS parameters should not crash") {
      val executor = createHapticExecutor(null)
      val patternWithIosParams = HapticPattern(
        events = listOf(
          ScheduledHapticEvent(
            startTimeMs = 0,
            durationMs = 100,
            intensity = HapticIntensity.HIGH,
            iosParameters = IosHapticParameters(
              sharpness = 0.8f,
            ),
          ),
        ),
      )

      executor.execute(patternWithIosParams)
    }

    test("complex pattern with multiple events should not crash") {
      val executor = createHapticExecutor(null)
      val complexPattern = HapticPattern(
        events = listOf(
          ScheduledHapticEvent(
            startTimeMs = 0,
            durationMs = 50,
            intensity = HapticIntensity.LIGHT,
          ),
          ScheduledHapticEvent(
            startTimeMs = 100,
            durationMs = 100,
            intensity = HapticIntensity.MEDIUM,
          ),
          ScheduledHapticEvent(
            startTimeMs = 250,
            durationMs = 150,
            intensity = HapticIntensity.HIGH,
          ),
        ),
      )

      executor.execute(complexPattern)
    }
  })

private fun createTestPattern(): HapticPattern = HapticPattern(
  events = listOf(
    ScheduledHapticEvent(
      startTimeMs = 0,
      durationMs = 100,
      intensity = HapticIntensity.MEDIUM,
    ),
  ),
)
