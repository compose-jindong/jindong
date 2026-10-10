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
 * Handle for controlling an ongoing haptic execution.
 *
 * Returned by [HapticExecutor.executeAsync] to allow explicit cancellation
 * of haptic patterns.
 */
interface HapticHandle {
  /**
   * Cancels the ongoing haptic execution.
   * If the execution has already completed, this is a no-op.
   */
  fun cancel()

  /**
   * Returns true while the haptic execution is still considered active, i.e. neither cancelled nor
   * completed.
   *
   * Android completion is estimated from the selected native playback duration. Core Haptics
   * waits for every advanced player to finish, including the hardware-defined transient pulse.
   * Both platforms preserve trailing logical silence. Native scheduling may introduce jitter.
   * [cancel] flips this to `false` immediately; an empty or unsupported pattern ends immediately.
   */
  val isActive: Boolean

  /** Native playback or completion cleanup failure, retained after cancellation; null on success. */
  val failure: Throwable?
    get() = null
}
