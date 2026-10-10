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
package io.github.compose.jindong.core.model

/**
 * Scales event boundaries and the complete timeline by the same finite positive [factor].
 * Milliseconds round to the nearest integer, with ties rounded up. A positive event must retain at
 * least 1ms; use a larger factor if its boundaries round to the same millisecond.
 */
public fun HapticPattern.timeStretch(factor: Float): HapticPattern {
  require(factor.isFinite() && factor > 0f) { "factor must be finite and positive, was $factor" }
  if (factor == 1f) return this
  val duration = scaleTime(durationMs, factor)
  return HapticPattern(
    events.mapIndexed { index, event ->
      val start = scaleTime(event.startTimeMs, factor)
      val end = scaleTime(checkedTimeAdd(event.startTimeMs, event.durationMs, "event[$index] end"), factor)
      require(event.durationMs == 0L || end > start) {
        "event[$index] rounds to 0ms; use a factor that retains at least 1ms"
      }
      fun stretch(curve: HapticCurve?): HapticCurve? = curve?.let {
        HapticCurve(
          it.points.map { point ->
            point.copy(timeMs = scaleTime(checkedTimeAdd(event.startTimeMs, point.timeMs, "curve time"), factor) - start)
          },
        )
      }
      event.copy(
        startTimeMs = start,
        durationMs = end - start,
        eventType = event.eventType,
        intensityCurve = stretch(event.intensityCurve),
        sharpnessCurve = stretch(event.sharpnessCurve),
      )
    },
    duration,
  )
}

// Multiply the integer time by the Float's exact binary value, without losing large-ms boundaries.
internal fun scaleTime(value: Long, factor: Float): Long {
  if (value == 0L) return 0L
  val bits = factor.toBits()
  val exponentBits = (bits ushr 23) and 0xff
  val significand = ((bits and 0x7fffff) or if (exponentBits == 0) 0 else 0x800000).toULong()
  val exponent = if (exponentBits == 0) -149 else exponentBits - 150
  val lowProduct = (value.toULong() and 0xffffffffuL) * significand
  val highProduct = (value.toULong() shr 32) * significand
  val low = lowProduct + (highProduct shl 32)
  val high = (highProduct shr 32) + if (low < lowProduct) 1uL else 0uL
  val result: ULong
  val roundUp: ULong
  if (exponent >= 0) {
    require(exponent < 63 && high == 0uL && low <= (Long.MAX_VALUE.toULong() shr exponent)) {
      "timeStretch: millisecond duration overflows ($value * $factor)"
    }
    result = low shl exponent
    roundUp = 0uL
  } else {
    val shift = -exponent
    when {
      shift >= 128 -> return 0L

      shift > 64 -> {
        result = high shr (shift - 64)
        roundUp = (high shr (shift - 65)) and 1uL
      }

      shift == 64 -> {
        result = high
        roundUp = low shr 63
      }

      else -> {
        require(high shr shift == 0uL) { "timeStretch: millisecond duration overflows ($value * $factor)" }
        result = (high shl (64 - shift)) or (low shr shift)
        roundUp = (low shr (shift - 1)) and 1uL
      }
    }
  }
  require(result <= Long.MAX_VALUE.toULong() - roundUp) {
    "timeStretch: millisecond duration overflows ($value * $factor)"
  }
  return (result + roundUp).toLong()
}
