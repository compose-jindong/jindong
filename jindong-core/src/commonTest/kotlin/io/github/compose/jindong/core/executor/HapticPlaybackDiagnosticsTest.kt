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

import io.github.compose.jindong.core.HapticManager
import io.github.compose.jindong.core.fake.FakeHapticExecutor
import io.github.compose.jindong.core.model.HapticPattern
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class HapticPlaybackDiagnosticsTest : FunSpec({
  test("custom executor can describe a pattern without playing it") {
    val executor = FakeHapticExecutor(hasAmplitudeControl = false)
    val diagnostic = executor.diagnose(HapticPattern(emptyList(), 120L))

    diagnostic.backend shouldBe HapticPlaybackBackend.CUSTOM
    diagnostic.capabilities shouldBe HapticDeviceCapabilities(true, false)
    diagnostic.logicalDurationMs shouldBe 120L
    diagnostic.estimatedNativeDurationMs shouldBe 120L
    executor.executedPatterns shouldBe emptyList()
    executor.asyncExecutedPatterns shouldBe emptyList()
  }

  test("unsupported executor reports no native duration") {
    val diagnostic = FakeHapticExecutor(isSupported = false, hasAmplitudeControl = false)
      .diagnose(HapticPattern(emptyList(), 120L))

    diagnostic.backend shouldBe HapticPlaybackBackend.UNSUPPORTED
    diagnostic.logicalDurationMs shouldBe 120L
    diagnostic.estimatedNativeDurationMs shouldBe 0L
    diagnostic.unsupportedReason shouldBe "Haptic hardware is unavailable"
  }

  test("manager delegates diagnosis without starting a session") {
    val executor = FakeHapticExecutor()
    HapticManager.initializeExecutor(executor)
    try {
      HapticManager.diagnose(HapticPattern.Empty).backend shouldBe HapticPlaybackBackend.CUSTOM
      executor.issuedHandles shouldBe emptyList()
    } finally {
      HapticManager.release()
    }
  }
})
