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
package io.github.compose.jindong.core

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Creates a [Duration] from this [Int] value in milliseconds.
 *
 * Example:
 * ```kotlin
 * val pattern = buildHapticPattern {
 *     haptic(100.ms)
 *     delay(50.ms)
 * }
 * ```
 */
public val Int.ms: Duration
  get() = this.milliseconds

/**
 * Creates a [Duration] from this [Long] value in milliseconds.
 *
 * Example:
 * ```kotlin
 * val durationMs = 150L
 * val pattern = buildHapticPattern {
 *     haptic(durationMs.ms)
 * }
 * ```
 */
public val Long.ms: Duration
  get() = this.milliseconds
