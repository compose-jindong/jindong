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
package io.github.jindong.android

import android.content.Context
import android.os.Build
import android.os.Vibrator
import androidx.test.core.app.ApplicationProvider
import io.github.compose.jindong.executor.HapticExecutor
import io.github.compose.jindong.executor.createHapticExecutor
import io.github.compose.jindong.model.HapticIntensity
import io.github.compose.jindong.model.HapticPattern
import io.github.compose.jindong.model.ScheduledHapticEvent
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowVibrator

/**
 * Tests for Jindong's AndroidHapticExecutor using Robolectric.
 *
 * Verify that Jindong's AndroidHapticExecutor correctly translates
 * haptic patterns into Android VibrationEffect API calls.
 *
 * - Single event patterns: single vibrations with various intensity levels
 * - Sequence patterns: complex sequences with delays and multiple intensities
 * - Edge cases: minimum duration, minimum amplitude, cancellation
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
class AndroidVibratorTest {

  private lateinit var shadowVibrator: ShadowVibrator
  private lateinit var executor: HapticExecutor

  @Before
  fun setup() {
    val context: Context = ApplicationProvider.getApplicationContext()
    val vibrator = context.getSystemService(Vibrator::class.java)
    shadowVibrator = shadowOf(vibrator).also { it.setHasAmplitudeControl(true) }
    executor = createHapticExecutor(context)
  }

  @Test
  fun `executor should be supported`() {
    executor.isSupported shouldBe true
  }

  // ============================================================================
  // Single Event Pattern Tests (HapticIntensity levels)
  // ============================================================================

  @Test
  fun `should vibrate with HIGH intensity`() = runTest {
    // HapticIntensity.HIGH (value = 1.0 -> 255)
    val pattern = HapticPattern(
      listOf(
        ScheduledHapticEvent(
          startTimeMs = 0,
          durationMs = 100,
          intensity = HapticIntensity.HIGH,
        ),
      ),
    )

    executor.execute(pattern)

    shadowVibrator.isVibrating shouldBe true
  }

  @Test
  fun `should vibrate with MEDIUM intensity`() = runTest {
    // HapticIntensity.MEDIUM (value = 0.5 -> 127)
    val pattern = HapticPattern(
      listOf(
        ScheduledHapticEvent(
          startTimeMs = 0,
          durationMs = 200,
          intensity = HapticIntensity.MEDIUM,
        ),
      ),
    )

    executor.execute(pattern)

    shadowVibrator.isVibrating shouldBe true
  }

  @Test
  fun `should vibrate with LIGHT intensity`() = runTest {
    // HapticIntensity.LIGHT (value = 0.25 -> 63)
    val pattern = HapticPattern(
      listOf(
        ScheduledHapticEvent(
          startTimeMs = 0,
          durationMs = 150,
          intensity = HapticIntensity.LIGHT,
        ),
      ),
    )

    executor.execute(pattern)

    shadowVibrator.isVibrating shouldBe true
  }

  @Test
  fun `should vibrate with STRONG intensity`() = runTest {
    // HapticIntensity.STRONG (value = 0.75 -> 191)
    val pattern = HapticPattern(
      listOf(
        ScheduledHapticEvent(
          startTimeMs = 0,
          durationMs = 100,
          intensity = HapticIntensity.STRONG,
        ),
      ),
    )

    executor.execute(pattern)

    shadowVibrator.isVibrating shouldBe true
  }

  @Test
  fun `should vibrate with Custom intensity`() = runTest {
    // HapticIntensity.Custom(0.65f) -> 165
    val pattern = HapticPattern(
      listOf(
        ScheduledHapticEvent(
          startTimeMs = 0,
          durationMs = 100,
          intensity = HapticIntensity.Custom(0.65f),
        ),
      ),
    )

    executor.execute(pattern)

    shadowVibrator.isVibrating shouldBe true
  }

  // ============================================================================
  // Sequence Pattern Tests (Multiple events with delays)
  // ============================================================================

  @Test
  fun `should handle sequence pattern with delays`() = runTest {
    // Pattern: vibrate(100ms@HIGH) -> pause(50ms) -> vibrate(100ms@MEDIUM)
    val pattern = HapticPattern(
      listOf(
        ScheduledHapticEvent(
          startTimeMs = 0,
          durationMs = 100,
          intensity = HapticIntensity.HIGH,
        ),
        ScheduledHapticEvent(
          startTimeMs = 150, // 100ms + 50ms delay
          durationMs = 100,
          intensity = HapticIntensity.MEDIUM,
        ),
      ),
    )

    executor.execute(pattern)

    shadowVibrator.isVibrating shouldBe true
  }

  @Test
  fun `should handle complex sequence with multiple segments`() = runTest {
    // Pattern: Haptic -> Delay -> Haptic -> Delay -> Haptic
    val pattern = HapticPattern(
      listOf(
        ScheduledHapticEvent(
          startTimeMs = 0,
          durationMs = 100,
          intensity = HapticIntensity.HIGH,
        ),
        ScheduledHapticEvent(
          startTimeMs = 150, // 100ms + 50ms delay
          durationMs = 100,
          intensity = HapticIntensity.MEDIUM,
        ),
        ScheduledHapticEvent(
          startTimeMs = 300, // 250ms + 50ms delay
          durationMs = 100,
          intensity = HapticIntensity.HIGH,
        ),
      ),
    )

    executor.execute(pattern)

    shadowVibrator.isVibrating shouldBe true
  }

  @Test
  fun `should handle back-to-back events without gaps`() = runTest {
    // Pattern: Three consecutive haptics (like Triple Tap)
    val pattern = HapticPattern(
      listOf(
        ScheduledHapticEvent(
          startTimeMs = 0,
          durationMs = 25,
          intensity = HapticIntensity.MEDIUM,
        ),
        ScheduledHapticEvent(
          startTimeMs = 25,
          durationMs = 25,
          intensity = HapticIntensity.MEDIUM,
        ),
        ScheduledHapticEvent(
          startTimeMs = 50,
          durationMs = 25,
          intensity = HapticIntensity.MEDIUM,
        ),
      ),
    )

    executor.execute(pattern)

    shadowVibrator.isVibrating shouldBe true
  }

  // ============================================================================
  // Edge Cases
  // ============================================================================

  @Test
  fun `should handle minimum duration (1ms)`() = runTest {
    val pattern = HapticPattern(
      listOf(
        ScheduledHapticEvent(
          startTimeMs = 0,
          durationMs = 1,
          intensity = HapticIntensity.HIGH,
        ),
      ),
    )

    executor.execute(pattern)

    shadowVibrator.isVibrating shouldBe true
  }

  @Test
  fun `should handle minimum intensity`() = runTest {
    // Custom(0.0) should map to amplitude 1 (not 0, as 0 means off)
    val pattern = HapticPattern(
      listOf(
        ScheduledHapticEvent(
          startTimeMs = 0,
          durationMs = 100,
          intensity = HapticIntensity.Custom(0.0f),
        ),
      ),
    )

    executor.execute(pattern)

    shadowVibrator.isVibrating shouldBe true
  }

  @Test
  fun `should handle empty pattern gracefully`() = runTest {
    val pattern = HapticPattern.Empty

    executor.execute(pattern)

    // Should not crash, but also should not vibrate
    shadowVibrator.isVibrating shouldBe false
  }

  // ============================================================================
  // Control Operations
  // ============================================================================

  @Test
  fun `should cancel vibration via HapticHandle`() = runTest {
    val pattern = HapticPattern(
      listOf(
        ScheduledHapticEvent(
          startTimeMs = 0,
          durationMs = 1000,
          intensity = HapticIntensity.HIGH,
        ),
      ),
    )

    // Start vibration and get handle
    val handle = executor.executeAsync(pattern)
    shadowVibrator.isVibrating shouldBe true
    handle.isActive shouldBe true

    // Cancel via handle
    handle.cancel()
    shadowVibrator.isVibrating shouldBe false
    handle.isActive shouldBe false
  }

  @Test
  fun `should release executor resources`() {
    shouldNotThrow<Exception> {
      executor.release()
    }
  }
}
