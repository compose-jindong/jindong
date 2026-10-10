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

/** The native path selected for a complete pattern. */
public enum class HapticPlaybackBackend {
  SILENT,
  UNSUPPORTED,
  CUSTOM,
  ANDROID_ENVELOPE,
  ANDROID_PRIMITIVES,
  ANDROID_WAVEFORM,
  IOS_CORE_HAPTICS,
}

/** Device capabilities used to select playback, without application-side OS checks. */
public data class HapticDeviceCapabilities(
  val supportsHaptics: Boolean,
  val supportsAmplitudeControl: Boolean,
  val supportsEnvelopeEffects: Boolean = false,
)

/**
 * Describes a planned playback without starting the motor.
 *
 * [logicalDurationMs] is the authored timeline; [estimatedNativeDurationMs] includes any native
 * pulse or compatibility tail. Neither duration is a measurement of physical actuator completion.
 * [approximations] reports changes such as lost sharpness or curve sampling. Native startup errors
 * are thrown by execution, rather than reported as successful playback here.
 */
public data class HapticPlaybackDiagnostics(
  val backend: HapticPlaybackBackend,
  val capabilities: HapticDeviceCapabilities,
  val logicalDurationMs: Long,
  val estimatedNativeDurationMs: Long,
  val approximations: List<String> = emptyList(),
  val unsupportedReason: String? = null,
)
