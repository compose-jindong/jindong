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
package io.github.compose.jindong

import androidx.compose.runtime.CompositionLocalProvider
import io.github.compose.jindong.core.executor.HapticDeviceCapabilities
import io.github.compose.jindong.core.executor.HapticPlaybackBackend
import io.github.compose.jindong.core.executor.HapticPlaybackDiagnostics
import io.github.compose.jindong.core.model.HapticPattern
import io.github.compose.jindong.executor.LocalHapticExecutor
import io.github.compose.jindong.executor.RecordingHapticExecutor
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class HapticPlaybackDiagnosticsTest :
  FunSpec({
    test("diagnostics use the provider executor without playing the pattern") {
      val expected = HapticPlaybackDiagnostics(HapticPlaybackBackend.IOS_CORE_HAPTICS, HapticDeviceCapabilities(true, true), 120, 100)
      val executor = object : io.github.compose.jindong.core.executor.HapticExecutor by RecordingHapticExecutor() {
        override fun diagnose(pattern: HapticPattern): HapticPlaybackDiagnostics {
          pattern.durationMs shouldBe 120L
          return expected
        }
      }
      var actual: HapticPlaybackDiagnostics? = null
      compilePattern {
        CompositionLocalProvider(LocalHapticExecutor provides executor) {
          actual = rememberHapticPlaybackDiagnostics(HapticPattern(emptyList(), 120))
        }
      }
      actual shouldBe expected
    }
  })
