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

import android.os.VibrationEffect

/**
 * Captures vibration pattern details for verification.
 * Since we create VibrationEffect objects in tests, we can capture their parameters
 * to verify they match expected patterns.
 */
sealed class VibrationPattern {

  data class OneShot(
    val duration: Long,
    val amplitude: Int,
  ) : VibrationPattern() {
    fun createEffect(): VibrationEffect = VibrationEffect.createOneShot(duration, amplitude)
  }

  data class Waveform(
    val timings: LongArray,
    val amplitudes: IntArray,
    val repeat: Int,
  ) : VibrationPattern() {
    fun createEffect(): VibrationEffect = VibrationEffect.createWaveform(timings, amplitudes, repeat)

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as Waveform

      if (!timings.contentEquals(other.timings)) return false
      if (!amplitudes.contentEquals(other.amplitudes)) return false
      if (repeat != other.repeat) return false

      return true
    }

    override fun hashCode(): Int {
      var result = timings.contentHashCode()
      result = 31 * result + amplitudes.contentHashCode()
      result = 31 * result + repeat
      return result
    }
  }
}
