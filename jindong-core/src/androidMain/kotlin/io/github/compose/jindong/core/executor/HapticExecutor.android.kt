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
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Android implementation of [HapticExecutor] using [VibrationEffect.createWaveform].
 *
 * Uses waveform-based vibration for all patterns (instead of `createOneShot`) because
 * certain Android devices require at least one off/gap segment in the middle of the waveform
 * to recognize and execute the vibration pattern.
 * Single-event patterns are split with a 1ms primer vibration at the end of the main vibration.
 *
 * Requires `<uses-permission android:name="android.permission.VIBRATE"/>` in AndroidManifest.xml
 */
@RequiresApi(Build.VERSION_CODES.O)
internal class DefaultAndroidHapticExecutor(context: Context) : HapticExecutor {

  private val vibrator: Vibrator = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val vibratorManager =
        context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
      vibratorManager.defaultVibrator
    }

    else -> context.getSystemService(Vibrator::class.java)
  }

  override val isSupported: Boolean by lazy {
    vibrator.hasVibrator()
  }

  // ERM actuators round any non-zero amplitude up to 100%, so per-level intensity is indistinguishable:
  // callers read this to warn that LIGHT/MEDIUM/STRONG/HIGH feel identical, and toWaveform() gates the
  // fall ramp on it (a ramp would be lost when every non-zero amplitude rounds up to full).
  override val hasAmplitudeControl: Boolean by lazy {
    vibrator.hasAmplitudeControl()
  }

  @RequiresPermission(Manifest.permission.VIBRATE)
  override suspend fun execute(pattern: HapticPattern) {
    if (!isSupported || pattern.events.isEmpty()) return

    // Await the exact waveform that was played, including the primer/trailing compat segments,
    // so the caller resumes when the vibration truly ends rather than a few ms early.
    val waveform = vibratePattern(pattern)
    delay(waveform.timings.sum())
  }

  @RequiresPermission(Manifest.permission.VIBRATE)
  override fun executeAsync(pattern: HapticPattern): HapticHandle = when {
    !isSupported || pattern.events.isEmpty() -> AndroidHapticHandle(null)

    else -> {
      vibratePattern(pattern)
      AndroidHapticHandle(vibrator)
    }
  }

  @RequiresPermission(Manifest.permission.VIBRATE)
  override fun release() = vibrator.cancel()

  @RequiresPermission(Manifest.permission.VIBRATE)
  private fun vibratePattern(pattern: HapticPattern): Waveform {
    val waveform = pattern.toWaveform()
    val vibrationEffect = VibrationEffect.createWaveform(waveform.timings, waveform.amplitudes, -1)
    vibrator.vibrate(vibrationEffect)
    return waveform
  }

  /**
   * Converts [HapticPattern] to an Android waveform in stages:
   * 1. [mergeToSerial] flattens overlapping events into a single serial timeline.
   * 2. [insertFallRamps] softens active->gap drops on amplitude-capable (LRA) actuators only.
   * 3. quantize each [HapticSegment] into (timing, amplitude) pairs.
   * 4. [applyDeviceCompat] adds the Samsung primer and trailing gap that some devices require.
   */
  private fun HapticPattern.toWaveform(): Waveform {
    val serial = mergeToSerial(events)
    val segments = if (hasAmplitudeControl) insertFallRamps(serial) else serial
    val timings = mutableListOf<Long>()
    val amplitudes = mutableListOf<Int>()

    for (segment in segments) {
      timings += segment.durationMs
      // Gaps stay at amplitude 0; coerceIn would otherwise floor them to 1 and turn a pause into a buzz.
      // An active event with 0f intensity (Custom(0.0)) is not a gap and still floors to amplitude 1.
      amplitudes += if (segment.isGap) 0 else segment.toAmplitude()
    }

    return applyDeviceCompat(timings, amplitudes, isSingleEvent = events.size == 1)
  }

  /**
   * Post-processes the quantized waveform with device-compat segments:
   * single-event patterns get a 1ms off + 1ms on primer (Samsung), and every pattern gets a
   * trailing 1ms gap so the waveform terminates cleanly.
   */
  private fun applyDeviceCompat(
    timings: MutableList<Long>,
    amplitudes: MutableList<Int>,
    isSingleEvent: Boolean,
  ): Waveform {
    if (isSingleEvent) {
      timings += 1L
      amplitudes += 0
      timings += 1L
      amplitudes += 1
    }

    timings += 1L
    amplitudes += 0

    return Waveform(timings.toLongArray(), amplitudes.toIntArray())
  }

  private fun HapticSegment.toAmplitude(): Int = (intensity * MAX_AMPLITUDE).toInt().coerceIn(1, MAX_AMPLITUDE)

  private data class Waveform(
    val timings: LongArray,
    val amplitudes: IntArray,
  ) {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is Waveform) return false
      return timings.contentEquals(other.timings) && amplitudes.contentEquals(other.amplitudes)
    }

    override fun hashCode(): Int {
      var result = timings.contentHashCode()
      result = 31 * result + amplitudes.contentHashCode()
      return result
    }
  }

  companion object {
    private const val MAX_AMPLITUDE = 255
  }
}

internal class AndroidHapticHandle(private var vibrator: Vibrator?) : HapticHandle {

  private val _isActive = AtomicBoolean(vibrator != null)
  override val isActive: Boolean
    get() = _isActive.get()

  @RequiresPermission(Manifest.permission.VIBRATE)
  override fun cancel() {
    if (!_isActive.compareAndSet(true, false)) return
    vibrator?.cancel()
    vibrator = null
  }
}

/**
 * Creates an Android-specific [HapticExecutor].
 *
 * @param context Must be an instance of `android.content.Context`
 * @return AndroidHapticExecutor implementation
 * @throws IllegalArgumentException if context is null or not a Context
 */
@RequiresApi(Build.VERSION_CODES.O)
actual fun createHapticExecutor(context: Any?): HapticExecutor {
  requireNotNull(context) { "Context is required for Android HapticExecutor" }
  require(context is Context) { "Context must be android.content.Context" }
  return DefaultAndroidHapticExecutor(context)
}
