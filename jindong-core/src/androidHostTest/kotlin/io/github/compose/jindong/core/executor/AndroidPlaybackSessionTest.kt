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
package io.github.compose.jindong.core.executor

import android.content.Context
import android.os.Build
import android.os.Vibrator
import androidx.test.core.app.ApplicationProvider
import io.github.compose.jindong.core.HapticManager
import io.github.compose.jindong.core.dsl.buildHapticPattern
import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.model.HapticPattern
import io.github.compose.jindong.core.model.ScheduledHapticEvent
import io.github.compose.jindong.core.ms
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowVibrator

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
class AndroidPlaybackSessionTest {
  private lateinit var context: Context
  private lateinit var vibrator: ShadowVibrator
  private val pattern = buildHapticPattern { haptic(1000.ms) }

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    vibrator = shadowOf(context.getSystemService(Vibrator::class.java))
  }

  @Test(timeout = 5_000)
  fun `manual manager initialization and reuse do not lock recursively`() {
    HapticManager.release()
    try {
      HapticManager.initialize(context)
      HapticManager.isSupported shouldBe true
      HapticManager.release()
      HapticManager.initialize(context)
      HapticManager.isSupported shouldBe true
    } finally {
      HapticManager.release()
    }
  }

  @Test
  fun `cancelling execute stops the actual vibrator`() = runTest {
    val executor = DefaultAndroidHapticExecutor(context, testTimeSource)
    val job = launch(start = CoroutineStart.UNDISPATCHED) { executor.execute(pattern) }
    vibrator.isVibrating shouldBe true
    job.cancel()
    job.join()
    vibrator.isVibrating shouldBe false
    job.isCancelled shouldBe true
  }

  @Test
  fun `old execute cleanup cannot cancel a replacement waveform`() = runTest {
    val executor = DefaultAndroidHapticExecutor(context, testTimeSource)
    val job = launch(start = CoroutineStart.UNDISPATCHED) { executor.execute(pattern) }
    val next = executor.executeAsync(pattern)
    job.join()
    vibrator.isVibrating shouldBe true
    next.isActive shouldBe true
    next.cancel()
    vibrator.isVibrating shouldBe false
  }

  @Test
  fun `release stops playback and disallows reuse`() {
    val executor = DefaultAndroidHapticExecutor(context)
    val handle = executor.executeAsync(pattern)
    executor.release()
    executor.release()
    handle.isActive shouldBe false
    vibrator.isVibrating shouldBe false
    shouldThrow<IllegalStateException> { executor.executeAsync(pattern) }
  }

  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun `delay-only playback waits for logical duration without native output`() = runTest {
    val executor = DefaultAndroidHapticExecutor(context, testTimeSource)
    val silence = buildHapticPattern { delay(100.ms) }
    executor.execute(silence)
    testScheduler.currentTime shouldBe 100L
    vibrator.pattern.shouldBeNull()
    vibrator.isVibrating shouldBe false
  }

  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun `playback waits through trailing silence beyond native waveform end`() = runTest {
    val executor = DefaultAndroidHapticExecutor(context, testTimeSource)
    val withSilence = buildHapticPattern {
      haptic(50.ms)
      delay(50.ms)
    }
    executor.execute(withSilence)
    testScheduler.currentTime shouldBe 100L
    vibrator.pattern.sum() shouldBe 53L
    vibrator.isVibrating shouldBe false
  }

  @Test
  fun `already-cancelled coroutine does not start a native effect`() = runTest {
    val executor = DefaultAndroidHapticExecutor(context, testTimeSource)
    val job = launch(start = CoroutineStart.UNDISPATCHED) {
      currentCoroutineContext().cancel()
      executor.execute(pattern)
    }
    job.join()
    vibrator.pattern.shouldBeNull()
  }

  @Test
  fun `native compatibility length overflow is rejected before vibration`() {
    val executor = DefaultAndroidHapticExecutor(context)
    val overflowing = HapticPattern(listOf(ScheduledHapticEvent(0L, Long.MAX_VALUE, HapticIntensity.HIGH)))
    shouldThrow<IllegalArgumentException> { executor.executeAsync(overflowing) }
    vibrator.pattern.shouldBeNull()
  }

  @Test
  fun `unsupported hardware returns an inactive handle`() {
    vibrator.setHasVibrator(false)
    val executor = DefaultAndroidHapticExecutor(context)
    executor.executeAsync(pattern).isActive shouldBe false
    vibrator.isVibrating shouldBe false
  }
}
