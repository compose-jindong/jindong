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
package io.github.compose.jindong.core.fake

import io.github.compose.jindong.core.executor.HapticExecutor
import io.github.compose.jindong.core.executor.HapticHandle
import io.github.compose.jindong.core.model.HapticPattern
import kotlinx.coroutines.delay

/**
 * Fake implementation of [HapticExecutor] for testing.
 *
 * Records all executed patterns and simulates execution delays.
 */
class FakeHapticExecutor(
  override val isSupported: Boolean = true,
) : HapticExecutor {

  private val _executedPatterns = mutableListOf<HapticPattern>()
  private val _asyncExecutedPatterns = mutableListOf<HapticPattern>()
  private var _releaseCalled = false

  val executedPatterns: List<HapticPattern> get() = _executedPatterns.toList()
  val asyncExecutedPatterns: List<HapticPattern> get() = _asyncExecutedPatterns.toList()
  val releaseCalled: Boolean get() = _releaseCalled

  override suspend fun execute(pattern: HapticPattern) {
    _executedPatterns.add(pattern)
    // Simulate minimal execution time
    delay(1)
  }

  override fun executeAsync(pattern: HapticPattern): HapticHandle {
    _asyncExecutedPatterns.add(pattern)
    return FakeHapticHandle()
  }

  override fun release() {
    _releaseCalled = true
  }

  fun reset() {
    _executedPatterns.clear()
    _asyncExecutedPatterns.clear()
    _releaseCalled = false
  }
}

/**
 * Fake implementation of [HapticHandle] for testing.
 */
class FakeHapticHandle : HapticHandle {
  private var _cancelled = false
  private var _isActive = true

  val cancelled: Boolean get() = _cancelled
  override val isActive: Boolean get() = _isActive && !_cancelled

  override fun cancel() {
    _cancelled = true
    _isActive = false
  }
}
