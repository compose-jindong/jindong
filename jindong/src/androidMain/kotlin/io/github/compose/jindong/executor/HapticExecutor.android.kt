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

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import io.github.compose.jindong.model.HapticPattern
import io.github.compose.jindong.model.ScheduledHapticEvent
import kotlinx.coroutines.delay

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
    vibrator.hasVibrator() && vibrator.hasAmplitudeControl()
  }

  @RequiresPermission(Manifest.permission.VIBRATE)
  override suspend fun execute(pattern: HapticPattern) {
    if (!isSupported || pattern.events.isEmpty()) return

    vibratePattern(pattern)
    val totalDurationMs = pattern.events.maxOfOrNull { it.startTimeMs + it.durationMs } ?: 0L
    delay(totalDurationMs)
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
  private fun vibratePattern(pattern: HapticPattern) {
    val waveform = pattern.toWaveform()
    val vibrationEffect = VibrationEffect.createWaveform(waveform.timings, waveform.amplitudes, -1)
    vibrator.vibrate(vibrationEffect)
  }

  /**
   * Converts [HapticPattern] to Android waveform format with alternating off/on segments.
   * Note: Some devices require at least one gap in the middle of the waveform to function properly.
   */
  private fun HapticPattern.toWaveform(): Waveform {
    val sortedEvents = events.sortedBy { it.startTimeMs }
    val isSingleEvent = sortedEvents.size == 1
    val timings = mutableListOf<Long>()
    val amplitudes = mutableListOf<Int>()
    var currentTime = 0L

    for (event in sortedEvents) {
      val gap = event.startTimeMs - currentTime

      // Add gap if there's delay before this event
      if (gap > 0) {
        timings += gap
        amplitudes += 0
      }

      // Add the haptic event
      timings += event.durationMs
      amplitudes += event.toAmplitude()

      // Single-event patterns are split with a 1ms primer vibration at the end of the main vibration.
      // This is to ensure vibration support on Samsung devices.
      if (isSingleEvent) {
        timings += 1L
        amplitudes += 0
        timings += 1L
        amplitudes += 1
      }

      currentTime = event.startTimeMs + event.durationMs
    }

    // Add trailing segment to ensure proper termination
    timings += 1L
    amplitudes += 0

    return Waveform(timings.toLongArray(), amplitudes.toIntArray())
  }

  private fun ScheduledHapticEvent.toAmplitude(): Int = (intensity.value * MAX_AMPLITUDE).toInt().coerceIn(1, MAX_AMPLITUDE)

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

  override var isActive: Boolean = vibrator != null
    private set

  @RequiresPermission(Manifest.permission.VIBRATE)
  override fun cancel() {
    if (!isActive) return
    isActive = false
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
internal actual fun createHapticExecutor(context: Any?): HapticExecutor {
  requireNotNull(context) { "Context is required for Android HapticExecutor" }
  require(context is Context) { "Context must be android.content.Context" }
  return DefaultAndroidHapticExecutor(context)
}
