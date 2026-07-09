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
import io.github.compose.jindong.core.executor.HapticExecutor
import io.github.compose.jindong.core.executor.createHapticExecutor
import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.model.HapticPattern
import io.github.compose.jindong.core.model.ScheduledHapticEvent
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
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
    // Single event split: [100ms event] + [1ms gap] + [1ms post] + [1ms end]
    shadowVibrator.pattern shouldBe longArrayOf(100, 1, 1, 1)
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
    shadowVibrator.pattern shouldBe longArrayOf(200, 1, 1, 1)
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
    shadowVibrator.pattern shouldBe longArrayOf(150, 1, 1, 1)
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
    shadowVibrator.pattern shouldBe longArrayOf(100, 1, 1, 1)
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
    shadowVibrator.pattern shouldBe longArrayOf(100, 1, 1, 1)
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
    // On an amplitude-capable (LRA) actuator, the active->gap boundary gets a fall ramp borrowed
    // from the gap front: the 50ms gap becomes [8ms ramp @ HIGH/2][42ms gap]. Total span unchanged.
    // [100ms event1] + [8ms ramp] + [42ms gap] + [100ms event2] + [1ms end]
    shadowVibrator.pattern shouldBe longArrayOf(100, 8, 42, 100, 1)
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
    // LRA fall ramps soften both internal active->gap boundaries: each 50ms gap becomes
    // [8ms ramp][42ms gap]. Total span unchanged (ramp borrowed from the gap front).
    // [100 e1][8 ramp][42 gap1][100 e2][8 ramp][42 gap2][100 e3][1 end]
    shadowVibrator.pattern shouldBe longArrayOf(100, 8, 42, 100, 8, 42, 100, 1)
  }

  @Test
  fun `should insert a fall ramp at an active-to-gap boundary on an LRA actuator`() = runTest {
    // setup() already enabled amplitude control (LRA). A single active->gap boundary.
    val pattern = HapticPattern(
      listOf(
        ScheduledHapticEvent(
          startTimeMs = 0,
          durationMs = 100,
          intensity = HapticIntensity.STRONG,
        ),
        ScheduledHapticEvent(
          startTimeMs = 150, // 100ms + 50ms gap
          durationMs = 50,
          intensity = HapticIntensity.STRONG,
        ),
      ),
    )

    executor.execute(pattern)

    shadowVibrator.isVibrating shouldBe true
    // The 50ms gap is split into an 8ms ramp + 42ms gap; the active segments are untouched.
    // ShadowVibrator only exposes timings (getPattern), so amplitude precision is asserted in
    // InsertFallRampsTest; here we verify the timeline was reshaped by the ramp.
    // [100 active][8 ramp][42 gap][50 active][1 end]
    shadowVibrator.pattern shouldBe longArrayOf(100, 8, 42, 50, 1)
  }

  @Test
  fun `should not insert a fall ramp on an ERM actuator without amplitude control`() = runTest {
    val context: Context = ApplicationProvider.getApplicationContext()
    // Disable amplitude control BEFORE the executor evaluates its lazy hasAmplitudeControl.
    shadowVibrator.setHasAmplitudeControl(false)
    val ermExecutor = createHapticExecutor(context)

    val pattern = HapticPattern(
      listOf(
        ScheduledHapticEvent(
          startTimeMs = 0,
          durationMs = 100,
          intensity = HapticIntensity.HIGH,
        ),
        ScheduledHapticEvent(
          startTimeMs = 150, // 100ms + 50ms gap
          durationMs = 100,
          intensity = HapticIntensity.MEDIUM,
        ),
      ),
    )

    ermExecutor.execute(pattern)

    shadowVibrator.isVibrating shouldBe true
    // No ramp on ERM (amplitude would round up anyway): original gap shape preserved.
    shadowVibrator.pattern shouldBe longArrayOf(100, 50, 100, 1)
  }

  @Test
  fun `should leave a sub-threshold gap unramped on an LRA actuator`() = runTest {
    // setup() enabled amplitude control (LRA). The gap (4ms) is not greater than MIN_RAMP_MS,
    // so insertFallRamps must leave it intact rather than splitting it into a ramp.
    val pattern = HapticPattern(
      listOf(
        ScheduledHapticEvent(
          startTimeMs = 0,
          durationMs = 100,
          intensity = HapticIntensity.HIGH,
        ),
        ScheduledHapticEvent(
          startTimeMs = 104, // 100ms + 4ms gap (== MIN_RAMP_MS, not greater)
          durationMs = 50,
          intensity = HapticIntensity.HIGH,
        ),
      ),
    )

    executor.execute(pattern)

    shadowVibrator.isVibrating shouldBe true
    // Gap stays whole: [100 active][4 gap][50 active][1 end]; no ramp inserted.
    shadowVibrator.pattern shouldBe longArrayOf(100, 4, 50, 1)
  }

  @Test
  fun `should not vibrate a zero-duration event`() = runTest {
    // A zero-duration event produces no active segment, so it must be a no-op rather than
    // emitting the compat-only primer/trailing buzz.
    val pattern = HapticPattern(
      listOf(
        ScheduledHapticEvent(
          startTimeMs = 0,
          durationMs = 0,
          intensity = HapticIntensity.HIGH,
        ),
      ),
    )

    executor.execute(pattern)

    // isVibrating alone could pass even if a short compat-only waveform briefly played and ended;
    // assert no waveform was ever handed to the vibrator, proving execute() was a true no-op.
    shadowVibrator.pattern.shouldBeNull()
    shadowVibrator.isVibrating shouldBe false
  }

  @Test
  fun `should await the merged duration for overlapping events`() = runTest {
    // Overlap [0,100)@HIGH + [50,150)@MEDIUM merges to a 150ms span; execute() must suspend for
    // the played waveform (150ms span + 1ms trailing = 151ms), not the raw maxOf of the events.
    val pattern = HapticPattern(
      listOf(
        ScheduledHapticEvent(
          startTimeMs = 0,
          durationMs = 100,
          intensity = HapticIntensity.HIGH,
        ),
        ScheduledHapticEvent(
          startTimeMs = 50,
          durationMs = 100,
          intensity = HapticIntensity.MEDIUM,
        ),
      ),
    )

    val before = testScheduler.currentTime
    executor.execute(pattern)
    val elapsed = testScheduler.currentTime - before

    elapsed shouldBe shadowVibrator.pattern.sum()
    elapsed shouldBe 151L
  }

  @Test
  fun `should await the full played waveform length including compat segments`() = runTest {
    // A single 100ms event plays as [100 active][1 gap][1 primer][1 end] = 103ms (the primer is
    // single-event only). execute() must delay for the whole 103ms, not the bare 100ms merged span,
    // so the caller resumes when the vibration truly ends.
    val pattern = HapticPattern(
      listOf(
        ScheduledHapticEvent(
          startTimeMs = 0,
          durationMs = 100,
          intensity = HapticIntensity.HIGH,
        ),
      ),
    )

    val before = testScheduler.currentTime
    executor.execute(pattern)
    val elapsed = testScheduler.currentTime - before

    elapsed shouldBe shadowVibrator.pattern.sum()
    elapsed shouldBe 103L
  }

  @Test
  fun `should serialize overlapping events keeping higher intensity`() = runTest {
    // Overlap: [0,100)@HIGH overlaps [50,150)@MEDIUM.
    // Merged serial timeline: [0,50)@HIGH, [50,100)@HIGH (winner), [100,150)@MEDIUM.
    val pattern = HapticPattern(
      listOf(
        ScheduledHapticEvent(
          startTimeMs = 0,
          durationMs = 100,
          intensity = HapticIntensity.HIGH,
        ),
        ScheduledHapticEvent(
          startTimeMs = 50,
          durationMs = 100,
          intensity = HapticIntensity.MEDIUM,
        ),
      ),
    )

    executor.execute(pattern)

    shadowVibrator.isVibrating shouldBe true
    // [50ms HIGH] + [50ms HIGH] + [50ms MEDIUM] + [1ms end], total span 150ms.
    shadowVibrator.pattern shouldBe longArrayOf(50, 50, 50, 1)
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
    // Back-to-back events without gaps in input: [25ms event1] + [25ms event2] + [25ms event3] + [1ms end]
    shadowVibrator.pattern shouldBe longArrayOf(25, 25, 25, 1)
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
    // Single event split: [1ms event] + [1ms gap] + [1ms post] + [1ms end]
    shadowVibrator.pattern shouldBe longArrayOf(1, 1, 1, 1)
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
    shadowVibrator.pattern shouldBe longArrayOf(100, 1, 1, 1)
  }

  @Test
  fun `should handle empty pattern gracefully`() = runTest {
    val pattern = HapticPattern.Empty

    executor.execute(pattern)

    // Should not crash, and no waveform should ever reach the vibrator.
    shadowVibrator.pattern.shouldBeNull()
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
