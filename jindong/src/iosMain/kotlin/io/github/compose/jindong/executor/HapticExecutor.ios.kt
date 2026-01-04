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

import io.github.compose.jindong.model.HapticPattern

/**
 * Actual implementation class for iOS.
 */
private class DefaultIosHapticExecutor : HapticExecutor {
  override suspend fun execute(pattern: HapticPattern) {
    // TODO: Implement iOS haptic execution
    // - Initialize CHHapticEngine if not started
    // - Convert pattern.events to CHHapticPattern
    // - Create CHHapticEvents (transient or continuous based on duration)
    // - Apply intensity, sharpness, and envelope parameters
    // - Support parallel events with same relativeTime
    // - Create and start CHHapticPatternPlayer
    TODO("Execute not implemented yet")
  }

  override fun isSupported(): Boolean {
    // TODO: Check if Core Haptics is supported
    // - Use CHHapticEngine.capabilitiesForHardware()
    // - Check supportsHaptics property
    return false
  }

  override fun release() {
    // TODO: Stop and release CHHapticEngine
    // - Stop any playing pattern
    // - Stop engine
    // - Set engine to null
  }
}

/**
 * Creates an iOS-specific [HapticExecutor].
 *
 * @param context Not used on iOS, can be null
 * @return IosHapticExecutor implementation
 */
internal actual fun createHapticExecutor(context: Any?): HapticExecutor {
  // TODO: Initialize CHHapticEngine
  // - Create CHHapticEngine instance
  // - Set up engine stoppedHandler and resetHandler
  // - Start engine
  return DefaultIosHapticExecutor()
}
