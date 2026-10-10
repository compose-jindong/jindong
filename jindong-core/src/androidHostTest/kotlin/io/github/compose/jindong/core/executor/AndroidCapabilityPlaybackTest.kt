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
import android.os.Parcel
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.vibrator.VibratorEnvelopeEffectInfo
import androidx.test.core.app.ApplicationProvider
import io.github.compose.jindong.core.model.HapticControlPoint
import io.github.compose.jindong.core.model.HapticCurve
import io.github.compose.jindong.core.model.HapticEventType
import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.model.HapticPattern
import io.github.compose.jindong.core.model.ScheduledHapticEvent
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowVibrator
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], shadows = [PlanningVibratorShadow::class, PlanningVibratorCapabilitiesShadow::class])
class AndroidCapabilityPlaybackTest {
  private lateinit var context: Context
  private lateinit var vibrator: PlanningVibratorShadow
  private val curved = HapticPattern(
    listOf(
      ScheduledHapticEvent(
        0L,
        300L,
        HapticIntensity.Custom(0f),
        eventType = HapticEventType.CONTINUOUS,
        intensityCurve = HapticCurve(listOf(HapticControlPoint(0L, 0f), HapticControlPoint(80L, 0.8f), HapticControlPoint(300L, 0f))),
        sharpness = 0.6f,
      ),
    ),
  )

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    vibrator = Shadow.extract(context.getSystemService(Vibrator::class.java))
    vibrator.setHasVibrator(true)
    vibrator.setHasAmplitudeControl(true)
  }

  @Test
  fun `API36 renders a positive curve with zero fixed intensity using the diagnosed envelope`() {
    vibrator.envelopeSupported = true
    val executor = DefaultAndroidHapticExecutor(context)
    val diagnostics = executor.diagnose(curved)
    diagnostics.backend shouldBe HapticPlaybackBackend.ANDROID_ENVELOPE
    vibrator.effects.size shouldBe 0
    val handle = executor.executeAsync(curved)
    ReflectionHelpers.callInstanceMethod<Long>(vibrator.effects.single(), "getDuration") shouldBe diagnostics.estimatedNativeDurationMs
    handle.isActive shouldBe true
    handle.cancel()
    vibrator.stopped shouldBe true
    vibrator.cancelCount shouldBe 1
  }

  @Test
  fun `unsupported envelope and hardware limits both use the diagnosed waveform`() {
    vibrator.envelopeSupported = true
    vibrator.minSegmentMs = 100L
    val executor = DefaultAndroidHapticExecutor(context)
    val diagnostics = executor.diagnose(curved)
    diagnostics.backend shouldBe HapticPlaybackBackend.ANDROID_WAVEFORM
    executor.executeAsync(curved).cancel()
    ReflectionHelpers.callInstanceMethod<Long>(vibrator.effects.single(), "getDuration") shouldBe diagnostics.estimatedNativeDurationMs
    vibrator.cancelCount shouldBe 1
  }

  @Test
  fun `largest supported waveform stays below the recommended Binder transaction size`() {
    val pattern = HapticPattern(
      List(MAX_ANDROID_WAVEFORM_SEGMENTS - 1) { index ->
        ScheduledHapticEvent(index.toLong(), 1L, if (index % 2 == 0) HapticIntensity.LIGHT else HapticIntensity.HIGH)
      },
    )
    val executor = DefaultAndroidHapticExecutor(context)
    executor.diagnose(pattern).backend shouldBe HapticPlaybackBackend.ANDROID_WAVEFORM
    executor.executeAsync(pattern).cancel()
    val parcel = Parcel.obtain()
    try {
      vibrator.effects.single().writeToParcel(parcel, 0)
      parcel.dataSize() shouldBe 12 + 92 * MAX_ANDROID_WAVEFORM_SEGMENTS
      (parcel.dataSize() < 64 * 1024) shouldBe true
    } finally {
      parcel.recycle()
    }
  }

  @Test
  fun `supported primitive composition uses queried native durations`() {
    vibrator.primitiveDurations[7] = 12
    val pattern = HapticPattern(
      listOf(
        ScheduledHapticEvent(0L, 0L, HapticIntensity.HIGH, eventType = HapticEventType.TRANSIENT, sharpness = 1f),
        ScheduledHapticEvent(100L, 0L, HapticIntensity.MEDIUM, eventType = HapticEventType.TRANSIENT, sharpness = 1f),
      ),
    )
    val executor = DefaultAndroidHapticExecutor(context)
    executor.diagnose(pattern).backend shouldBe HapticPlaybackBackend.ANDROID_PRIMITIVES
    executor.diagnose(pattern).estimatedNativeDurationMs shouldBe 112L
    executor.executeAsync(pattern).cancel()
    vibrator.effects.size shouldBe 1
    vibrator.cancelCount shouldBe 1
  }

  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun `zero logical duration transient waits through its native pulse`() = runTest {
    vibrator.primitiveDurations[7] = 12
    val executor = DefaultAndroidHapticExecutor(context, testTimeSource)
    val pattern = HapticPattern(listOf(ScheduledHapticEvent(0L, 0L, HapticIntensity.HIGH, eventType = HapticEventType.TRANSIENT, sharpness = 1f)))
    executor.execute(pattern)
    testScheduler.currentTime shouldBe 12L
    vibrator.effects.size shouldBe 1
    vibrator.cancelCount shouldBe 1
  }

  @Test
  fun `native start errors stop partial playback and propagate before returning a handle`() {
    val executor = DefaultAndroidHapticExecutor(context)
    vibrator.failStart = true
    shouldThrow<IllegalStateException> { executor.executeAsync(curved) }.message shouldBe "native start failed"
    vibrator.stopped shouldBe true
    vibrator.cancelCount shouldBe 1
    vibrator.failStart = false
    val replacement = executor.executeAsync(curved)
    replacement.isActive shouldBe true
    replacement.cancel()
    vibrator.cancelCount shouldBe 2
  }
}

/** Supplies hardware capabilities while leaving real SDK effect builders and validation in use. */
@Implements(Vibrator::class)
open class PlanningVibratorCapabilitiesShadow : ShadowVibrator() {
  var envelopeSupported = false
  var minSegmentMs = 20L
  val primitiveDurations = mutableMapOf<Int, Int>()
  val effects = mutableListOf<VibrationEffect>()
  var failStart = false
  var stopped = true
  var cancelCount = 0

  @Implementation
  protected fun vibrate(effect: VibrationEffect) {
    stopped = false
    if (failStart) error("native start failed")
    effects += effect
  }

  @Implementation(minSdk = 36)
  protected fun areEnvelopeEffectsSupported(): Boolean = envelopeSupported

  @Implementation(minSdk = 36)
  protected fun getEnvelopeEffectInfo(): VibratorEnvelopeEffectInfo {
    val parcel = Parcel.obtain()
    try {
      parcel.writeInt(16)
      parcel.writeLong(minSegmentMs)
      parcel.writeLong(1000L)
      parcel.setDataPosition(0)
      return VibratorEnvelopeEffectInfo.CREATOR.createFromParcel(parcel)
    } finally {
      parcel.recycle()
    }
  }

  @Implementation(minSdk = 31)
  protected fun arePrimitivesSupported(vararg ids: Int): BooleanArray = ids.map { primitiveDurations.containsKey(it) }.toBooleanArray()

  @Implementation(minSdk = 31)
  override fun getPrimitiveDurations(vararg ids: Int): IntArray = ids.map { primitiveDurations[it] ?: 0 }.toIntArray()
}

@Implements(className = "android.os.SystemVibrator", isInAndroidSdk = false)
class PlanningVibratorShadow : PlanningVibratorCapabilitiesShadow() {
  @Implementation
  protected fun hasVibrator(): Boolean = true

  @Implementation
  protected fun hasAmplitudeControl(): Boolean = true

  @Implementation
  protected fun cancel() {
    stopped = true
    cancelCount++
  }
}
