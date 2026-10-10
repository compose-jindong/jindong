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

/**
 * A single non-overlapping slice of a serialized haptic timeline.
 *
 * @property startTimeMs Absolute start of this segment.
 * @property durationMs Length of this segment, always greater than 0.
 * @property intensity Winning event's intensity (NOT a sum of overlapping events); 0f when a gap.
 * @property sharpness Carried from the same winning event (iOS Core Haptics parameter).
 * @property isGap True when no event is present here. A zero-intensity event is also silent, but
 *   remains distinct so fall ramps cannot borrow time from its explicitly silent interval.
 */
internal data class HapticSegment(
  val startTimeMs: Long,
  val durationMs: Long,
  val intensity: Float,
  val sharpness: Float,
  val isGap: Boolean = false,
  val endIntensity: Float = intensity,
  val endSharpness: Float = sharpness,
)
