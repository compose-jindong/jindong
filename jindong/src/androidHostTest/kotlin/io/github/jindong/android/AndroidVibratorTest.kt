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
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowVibrator

/**
 * Tests for Android Vibrator API using Robolectric.
 *
 * Verify that Jindong's AndroidHapticExecutor correctly translates
 * haptic patterns into Android VibrationEffect API calls.
 *
 * - OneShot patterns: single vibrations with various intensity levels
 * - Waveform patterns: complex sequences with delays and multiple intensities
 * - Edge cases: minimum duration, minimum amplitude, cancellation
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
class AndroidVibratorTest {

  private lateinit var vibrator: Vibrator
  private lateinit var shadowVibrator: ShadowVibrator

  @Before
  fun setup() {
    val context: Context = ApplicationProvider.getApplicationContext()
    vibrator = context.getSystemService(Vibrator::class.java)
    shadowVibrator = shadowOf(vibrator)
  }

  @Test
  fun `vibrator should be available`() {
    vibrator shouldNotBe null
    vibrator.hasVibrator() shouldBe true
  }

  // ============================================================================
  // OneShot Pattern Tests
  // ============================================================================
  // OneShot = Single vibration event (Jindong's HapticEventNode with single event)
  //
  // TODO: When AndroidHapticExecutor is implemented, replace these with:
  //   val pattern = HapticPattern(listOf(
  //       ScheduledHapticEvent(startTimeMs = 0, durationMs = X, intensity = Y)
  //   ))
  //   executor.execute(pattern)

  @Test
  fun `should vibrate with high intensity (255)`() {
    // TODO: Replace with Jindong's HapticIntensity.HIGH (value = 1.0 -> 255)
    val pattern = VibrationCapture.OneShot(duration = 100, amplitude = 255)

    val effect = pattern.createEffect()
    vibrator.vibrate(effect)

    shadowVibrator.isVibrating shouldBe true
    pattern.duration shouldBe 100L
    pattern.amplitude shouldBe 255
  }

  @Test
  fun `should vibrate with medium intensity (128)`() {
    // TODO: Replace with Jindong's HapticIntensity.MEDIUM (value = 0.5 -> 128)
    val pattern = VibrationCapture.OneShot(duration = 200, amplitude = 128)

    val effect = pattern.createEffect()
    vibrator.vibrate(effect)

    shadowVibrator.isVibrating shouldBe true
    pattern.duration shouldBe 200L
    pattern.amplitude shouldBe 128
  }

  @Test
  fun `should vibrate with low intensity (64)`() {
    // TODO: Replace with Jindong's HapticIntensity.LIGHT (value = 0.25 -> 64)
    val pattern = VibrationCapture.OneShot(duration = 150, amplitude = 64)

    val effect = pattern.createEffect()
    vibrator.vibrate(effect)

    shadowVibrator.isVibrating shouldBe true
    pattern.duration shouldBe 150L
    pattern.amplitude shouldBe 64
  }

  // ============================================================================
  // Waveform Pattern Tests
  // ============================================================================
  // Waveform = Complex sequence with delays (Jindong's Sequence/Delay pattern)
  //
  // TODO: When AndroidHapticExecutor is implemented, replace these with:
  //   Sequence {
  //       Haptic(100.ms, intensity = HIGH)
  //       Delay(50.ms)
  //       Haptic(100.ms, intensity = MEDIUM)
  //   }

  @Test
  fun `should handle waveform pattern with delays`() {
    // Pattern: vibrate(100ms@255) -> pause(50ms) -> vibrate(100ms@128)
    // TODO: Replace with Jindong's Sequence pattern
    val pattern = VibrationCapture.Waveform(
      timings = longArrayOf(0, 100, 50, 100),
      amplitudes = intArrayOf(0, 255, 0, 128),
      repeat = -1,
    )

    val effect = pattern.createEffect()
    vibrator.vibrate(effect)

    shadowVibrator.isVibrating shouldBe true
    pattern.timings shouldBe longArrayOf(0, 100, 50, 100)
    pattern.amplitudes shouldBe intArrayOf(0, 255, 0, 128)
    pattern.repeat shouldBe -1
  }

  @Test
  fun `should handle complex waveform with multiple segments`() {
    // Pattern: Repeat { Haptic -> Delay -> Haptic -> Delay -> Haptic }
    // TODO: Replace with Jindong's Repeat + Sequence pattern
    val pattern = VibrationCapture.Waveform(
      timings = longArrayOf(0, 100, 50, 100, 50, 100),
      amplitudes = intArrayOf(0, 255, 0, 128, 0, 255),
      repeat = -1,
    )

    val effect = pattern.createEffect()
    vibrator.vibrate(effect)

    shadowVibrator.isVibrating shouldBe true

    // Verify pattern structure
    pattern.timings.size shouldBe 6
    pattern.amplitudes.size shouldBe 6

    // First vibration
    pattern.timings[1] shouldBe 100
    pattern.amplitudes[1] shouldBe 255

    // Second vibration
    pattern.timings[3] shouldBe 100
    pattern.amplitudes[3] shouldBe 128

    // Third vibration
    pattern.timings[5] shouldBe 100
    pattern.amplitudes[5] shouldBe 255
  }

  // ============================================================================
  // Edge Cases
  // ============================================================================

  @Test
  fun `should handle minimum duration (1ms)`() {
    // TODO: Test with Jindong's minimum duration
    val pattern = VibrationCapture.OneShot(duration = 1, amplitude = 255)

    val effect = pattern.createEffect()
    vibrator.vibrate(effect)

    shadowVibrator.isVibrating shouldBe true
    pattern.duration shouldBe 1L
  }

  @Test
  fun `should handle minimum amplitude (1)`() {
    val shadowVibrator = shadowOf(vibrator)

    // TODO: Test with Jindong's Custom(0.0) -> should map to 1 (not 0)
    val pattern = VibrationCapture.OneShot(duration = 100, amplitude = 1)

    val effect = pattern.createEffect()
    vibrator.vibrate(effect)

    shadowVibrator.isVibrating shouldBe true
    pattern.amplitude shouldBe 1
  }

  // ============================================================================
  // Control Operations
  // ============================================================================

  @Test
  fun `should cancel vibration`() {
    // Start vibration
    // TODO: Use Jindong executor to start vibration
    val effect = VibrationEffect.createOneShot(1000, 255)
    vibrator.vibrate(effect)
    shadowVibrator.isVibrating shouldBe true

    // Cancel
    // TODO: Call executor.cancel() instead
    vibrator.cancel()
    shadowVibrator.isVibrating shouldBe false
  }
}
