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
 * An immutable event on the pattern timeline. Curves replace the corresponding fixed value.
 *
 * The original four-argument constructor retains legacy continuous events, including zero-length
 * no-ops. The explicit [eventType] constructor requires positive continuous duration and zero
 * transient duration. Transients cannot have curves. All sharpness values are finite and in 0..1.
 */
public class ScheduledHapticEvent private constructor(
  public val startTimeMs: Long,
  public val durationMs: Long,
  public val intensity: HapticIntensity,
  public val iosParameters: IosHapticParameters?,
  public val eventType: HapticEventType,
  public val sharpness: Float,
  public val intensityCurve: HapticCurve?,
  public val sharpnessCurve: HapticCurve?,
  private val legacy: Boolean,
) {
  public constructor(
    startTimeMs: Long,
    durationMs: Long,
    intensity: HapticIntensity,
    iosParameters: IosHapticParameters? = null,
  ) : this(
    startTimeMs, durationMs, intensity, iosParameters, HapticEventType.CONTINUOUS,
    iosParameters?.sharpness ?: 0.5f, null, null, true,
  )

  public constructor(
    startTimeMs: Long,
    durationMs: Long,
    intensity: HapticIntensity,
    iosParameters: IosHapticParameters? = null,
    eventType: HapticEventType,
    sharpness: Float = 0.5f,
    intensityCurve: HapticCurve? = null,
    sharpnessCurve: HapticCurve? = null,
  ) : this(startTimeMs, durationMs, intensity, iosParameters, eventType, sharpness, intensityCurve, sharpnessCurve, false)

  init {
    require(startTimeMs >= 0L) { "startTimeMs must be non-negative, was $startTimeMs" }
    require(durationMs >= 0L) { "durationMs must be non-negative, was $durationMs" }
    checkedTimeAdd(startTimeMs, durationMs, "event end")
    require(intensity.value.isFinite() && intensity.value in 0f..1f) { "Intensity must be finite and in 0..1" }
    require(sharpness.isFinite() && sharpness in 0f..1f) { "Sharpness must be finite and in 0..1, was $sharpness" }
    when (eventType) {
      HapticEventType.TRANSIENT -> {
        require(durationMs == 0L) { "A transient must have zero duration" }
        require(intensityCurve == null && sharpnessCurve == null) { "A transient cannot have curves" }
      }

      HapticEventType.CONTINUOUS -> require(durationMs > 0L || legacy) { "A continuous event must have positive duration" }
    }
    require(intensityCurve == null || intensityCurve.durationMs == durationMs) { "Intensity curve must end at event durationMs" }
    require(sharpnessCurve == null || sharpnessCurve.durationMs == durationMs) { "Sharpness curve must end at event durationMs" }
  }

  /** Retains the original copy signature and every rich event property. */
  public fun copy(
    startTimeMs: Long = this.startTimeMs,
    durationMs: Long = this.durationMs,
    intensity: HapticIntensity = this.intensity,
    iosParameters: IosHapticParameters? = this.iosParameters,
  ): ScheduledHapticEvent = ScheduledHapticEvent(
    startTimeMs, durationMs, intensity, iosParameters, eventType,
    if (legacy) iosParameters?.sharpness ?: 0.5f else sharpness, intensityCurve, sharpnessCurve, legacy,
  )

  /** Copies rich properties; [eventType] is required to keep the original overload unambiguous. */
  public fun copy(
    startTimeMs: Long = this.startTimeMs,
    durationMs: Long = this.durationMs,
    intensity: HapticIntensity = this.intensity,
    iosParameters: IosHapticParameters? = this.iosParameters,
    eventType: HapticEventType,
    sharpness: Float = this.sharpness,
    intensityCurve: HapticCurve? = this.intensityCurve,
    sharpnessCurve: HapticCurve? = this.sharpnessCurve,
  ): ScheduledHapticEvent = ScheduledHapticEvent(
    startTimeMs, durationMs, intensity, iosParameters, eventType, sharpness, intensityCurve, sharpnessCurve,
    legacy && eventType == HapticEventType.CONTINUOUS,
  )

  public operator fun component1(): Long = startTimeMs
  public operator fun component2(): Long = durationMs
  public operator fun component3(): HapticIntensity = intensity
  public operator fun component4(): IosHapticParameters? = iosParameters
  public operator fun component5(): HapticEventType = eventType
  public operator fun component6(): Float = sharpness
  public operator fun component7(): HapticCurve? = intensityCurve
  public operator fun component8(): HapticCurve? = sharpnessCurve

  override fun equals(other: Any?): Boolean = other is ScheduledHapticEvent &&
    startTimeMs == other.startTimeMs && durationMs == other.durationMs && intensity == other.intensity &&
    iosParameters == other.iosParameters && eventType == other.eventType && sharpness.toBits() == other.sharpness.toBits() &&
    intensityCurve == other.intensityCurve && sharpnessCurve == other.sharpnessCurve

  override fun hashCode(): Int {
    var result = startTimeMs.hashCode()
    result = 31 * result + durationMs.hashCode()
    result = 31 * result + intensity.hashCode()
    result = 31 * result + (iosParameters?.hashCode() ?: 0)
    result = 31 * result + eventType.hashCode()
    result = 31 * result + sharpness.hashCode()
    result = 31 * result + (intensityCurve?.hashCode() ?: 0)
    return 31 * result + (sharpnessCurve?.hashCode() ?: 0)
  }

  override fun toString(): String = "ScheduledHapticEvent(startTimeMs=$startTimeMs, durationMs=$durationMs, " +
    "intensity=$intensity, iosParameters=$iosParameters, eventType=$eventType, sharpness=$sharpness, " +
    "intensityCurve=$intensityCurve, sharpnessCurve=$sharpnessCurve)"
}
