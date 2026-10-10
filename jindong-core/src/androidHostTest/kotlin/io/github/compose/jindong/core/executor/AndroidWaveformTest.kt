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
import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.model.HapticPattern
import io.github.compose.jindong.core.model.ScheduledHapticEvent
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
class AndroidWaveformTest {
  private lateinit var executor: DefaultAndroidHapticExecutor

  @Before
  fun setup() {
    val context: Context = ApplicationProvider.getApplicationContext()
    shadowOf(context.getSystemService(Vibrator::class.java)).setHasAmplitudeControl(true)
    executor = DefaultAndroidHapticExecutor(context)
  }

  @Test
  fun `all zero events and gaps produce no native waveform`(): Unit = with(executor) {
    HapticPattern(
      listOf(event(20, 30, 0f), event(100, 50, 0f)),
    ).toWaveform().shouldBeNull()
  }

  @Test
  fun `positive to zero boundaries and surrounding gaps stay silent`(): Unit = with(executor) {
    val waveform = HapticPattern(
      listOf(event(20, 30, 0f), event(50, 50, 1f), event(100, 50, 0f), event(200, 50, 0.5f)),
    ).toWaveform()!!

    waveform.timings shouldBe longArrayOf(50, 50, 100, 50, 1)
    waveform.amplitudes shouldBe intArrayOf(0, 255, 0, 127, 0)
  }

  @Test
  fun `zero event does not mask a positive overlap`(): Unit = with(executor) {
    val waveform = HapticPattern(
      listOf(event(0, 100, 0f), event(25, 50, 1f)),
    ).toWaveform()!!

    waveform.timings shouldBe longArrayOf(25, 50, 25, 1)
    waveform.amplitudes shouldBe intArrayOf(0, 255, 0, 0)
  }

  @Test
  fun `tiny positive intensity still reaches the minimum positive amplitude`(): Unit = with(executor) {
    val waveform = HapticPattern(listOf(event(0, 10, Float.MIN_VALUE))).toWaveform()!!

    waveform.amplitudes shouldBe intArrayOf(1, 0, 1, 0)
  }

  private fun event(start: Long, duration: Long, intensity: Float): ScheduledHapticEvent = ScheduledHapticEvent(
    startTimeMs = start,
    durationMs = duration,
    intensity = HapticIntensity.Custom(intensity),
  )
}
