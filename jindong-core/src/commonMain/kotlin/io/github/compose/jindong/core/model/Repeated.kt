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
 * Returns a new pattern that plays this pattern back-to-back [times] times.
 *
 * Defined by folding [then], so `repeated(n)` equals `this then this then ...` n copies:
 * `repeated(1)` is this pattern, `repeated(0)` is [HapticPattern.Empty], and each copy after the
 * first is offset by one span so the copies never overlap.
 *
 * @param times non-negative repeat count
 */
public fun HapticPattern.repeated(times: Int): HapticPattern {
  require(times >= 0) { "times must be non-negative, was $times" }
  return (1 until times).fold(if (times == 0) HapticPattern.Empty else this) { acc, _ -> acc then this }
}
