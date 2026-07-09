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
 * The span of a pattern: the latest event end on its timeline, or 0 for an empty pattern.
 *
 * This is the raw timeline extent of the scheduled events, independent of any tree structure or
 * platform post-processing. It is the shared basis for [reversed] mirroring, [then] offsetting,
 * executor playback length, and [io.github.compose.jindong.core.element.PatternElement] duration.
 */
internal fun HapticPattern.spanMs(): Long = events.maxOfOrNull { it.startTimeMs + it.durationMs } ?: 0L
