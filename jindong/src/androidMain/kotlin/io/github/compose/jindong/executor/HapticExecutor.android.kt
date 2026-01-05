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
 * Actual implementation class for Android.
 */
private class DefaultAndroidHapticExecutor : HapticExecutor {

  override suspend fun execute(pattern: HapticPattern) {
    TODO("Execute not implemented yet")
  }

  override fun executeAsync(pattern: HapticPattern): HapticHandle {
    TODO("ExecuteAsync not implemented yet")
  }

  // TODO: Check if device supports haptics
  // - Verify API level >= 26
  // - Check if vibrator exists and hasVibrator()
  override val isSupported: Boolean
    get() = false

  override fun release() {
    // TODO: Release vibrator resources if needed
  }
}

/**
 * Creates an Android-specific [HapticExecutor].
 *
 * @param context Must be an instance of `android.content.Context`
 * @return AndroidHapticExecutor implementation
 * @throws IllegalArgumentException if context is null or not a Context
 */
internal actual fun createHapticExecutor(context: Any?): HapticExecutor {
  // TODO: Validate context and initialize vibrator
  // requireNotNull(context) { "Context is required for Android HapticExecutor" }
  // require(context is Context) { "Context must be android.content.Context" }
  return DefaultAndroidHapticExecutor()
}
