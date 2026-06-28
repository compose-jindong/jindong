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

import io.github.compose.jindong.Jindong
import io.github.compose.jindong.core.executor.HapticExecutor
import io.github.compose.jindong.core.executor.HapticHandle
import io.github.compose.jindong.core.model.HapticPattern

/**
 * Records executed patterns so a test can inspect what [Jindong] played for a given key.
 */
internal class RecordingHapticExecutor : HapticExecutor {
  override val isSupported: Boolean = true

  val executedPatterns = mutableListOf<HapticPattern>()

  override suspend fun execute(pattern: HapticPattern) {
    executedPatterns += pattern
  }

  override fun executeAsync(pattern: HapticPattern): HapticHandle = NoopHapticHandle

  override fun release() = Unit

  internal object NoopHapticHandle : HapticHandle {
    override val isActive: Boolean = false
    override fun cancel() = Unit
  }
}
