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

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import io.github.compose.jindong.core.model.HapticPattern
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.time.TimeSource

/** Uses one capability-selected native effect per playback. Requires the VIBRATE permission. */
@RequiresApi(Build.VERSION_CODES.O)
internal class DefaultAndroidHapticExecutor(
  context: Context,
  timeSource: TimeSource = TimeSource.Monotonic,
) : HapticExecutor {
  private val sessions = PlaybackSessions(timeSource)
  private val vibrator: Vibrator = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
      manager.defaultVibrator
    }

    else -> context.getSystemService(Vibrator::class.java)
  }

  override val isSupported: Boolean by lazy { vibrator.hasVibrator() }
  override val hasAmplitudeControl: Boolean by lazy { vibrator.hasAmplitudeControl() }

  private val capabilities: AndroidPlaybackCapabilities by lazy {
    val envelopeSupported = isSupported && Build.VERSION.SDK_INT >= 36 && vibrator.areEnvelopeEffectsSupported()
    AndroidPlaybackCapabilities(
      apiLevel = Build.VERSION.SDK_INT,
      device = HapticDeviceCapabilities(isSupported, hasAmplitudeControl, envelopeSupported),
      envelopeLimits = if (envelopeSupported && Build.VERSION.SDK_INT >= 36) readEnvelopeLimits() else null,
      primitiveDurationsMs = if (isSupported && Build.VERSION.SDK_INT >= 31) readPrimitiveDurations() else emptyMap(),
    )
  }

  override fun diagnose(pattern: HapticPattern): HapticPlaybackDiagnostics = planAndroidPlayback(pattern, capabilities).diagnostics

  @RequiresPermission(Manifest.permission.VIBRATE)
  override suspend fun execute(pattern: HapticPattern) {
    currentCoroutineContext().ensureActive()
    executeAsync(pattern).awaitCompletion()
  }

  @RequiresPermission(Manifest.permission.VIBRATE)
  override fun executeAsync(pattern: HapticPattern): PlaybackSessions.Session = sessions.start {
    val plan = planAndroidPlayback(pattern, capabilities)
    val effect = when (plan.diagnostics.backend) {
      HapticPlaybackBackend.SILENT -> return@start NativePlayback(pattern.durationMs)

      HapticPlaybackBackend.UNSUPPORTED -> return@start null

      HapticPlaybackBackend.ANDROID_ENVELOPE -> {
        check(Build.VERSION.SDK_INT >= 36)
        envelopeEffect(plan.envelope)
      }

      HapticPlaybackBackend.ANDROID_PRIMITIVES -> {
        check(Build.VERSION.SDK_INT >= 31)
        primitiveEffect(plan.primitives)
      }

      HapticPlaybackBackend.ANDROID_WAVEFORM -> {
        val waveform = checkNotNull(plan.waveform)
        VibrationEffect.createWaveform(waveform.timings, waveform.amplitudes, -1)
      }

      else -> error("Unexpected Android playback backend: ${plan.diagnostics.backend}")
    }
    try {
      vibrator.vibrate(effect)
    } catch (failure: Throwable) {
      try {
        vibrator.cancel()
      } catch (stopError: Throwable) {
        failure.addSuppressed(stopError)
      }
      throw failure
    }
    NativePlayback(maxOf(pattern.durationMs, plan.diagnostics.estimatedNativeDurationMs)) { vibrator.cancel() }
  }

  @RequiresPermission(Manifest.permission.VIBRATE)
  override fun release() = sessions.release { }

  /** Retains the waveform inspection path used by the existing host regression tests. */
  internal fun HapticPattern.toWaveform(): AndroidWaveform? = planAndroidPlayback(
    this,
    AndroidPlaybackCapabilities(26, HapticDeviceCapabilities(true, hasAmplitudeControl)),
  ).waveform

  @RequiresApi(36)
  private fun readEnvelopeLimits(): AndroidEnvelopeLimits = vibrator.envelopeEffectInfo.let {
    AndroidEnvelopeLimits(it.maxSize, it.minControlPointDurationMillis, it.maxControlPointDurationMillis, it.maxDurationMillis)
  }

  @RequiresApi(31)
  private fun readPrimitiveDurations(): Map<AndroidPrimitive, Int> {
    val primitives = AndroidPrimitive.entries
    val ids = primitives.map { it.id }.toIntArray()
    val supported = vibrator.arePrimitivesSupported(*ids)
    val durations = vibrator.getPrimitiveDurations(*ids)
    return primitives.mapIndexedNotNull { index, primitive ->
      if (supported.getOrNull(index) == true && (durations.getOrNull(index) ?: 0) > 0) primitive to durations[index] else null
    }.toMap()
  }

  @RequiresApi(36)
  private fun envelopeEffect(points: List<AndroidEnvelopePoint>): VibrationEffect {
    val builder = VibrationEffect.BasicEnvelopeBuilder().setInitialSharpness(points.first().sharpness)
    for ((start, end) in points.zipWithNext()) builder.addControlPoint(end.intensity, end.sharpness, end.timeMs - start.timeMs)
    return builder.build()
  }

  @RequiresApi(31)
  private fun primitiveEffect(steps: List<AndroidPrimitiveStep>): VibrationEffect {
    val builder = VibrationEffect.startComposition()
    for (step in steps) builder.addPrimitive(step.primitive.id, step.scale, step.delayMs)
    return builder.compose()
  }
}

/** Creates an Android executor. [context] must be an Android [Context]. */
@RequiresApi(Build.VERSION_CODES.O)
actual fun createHapticExecutor(context: Any?): HapticExecutor {
  requireNotNull(context) { "Context is required for Android HapticExecutor" }
  require(context is Context) { "Context must be android.content.Context" }
  return DefaultAndroidHapticExecutor(context)
}
