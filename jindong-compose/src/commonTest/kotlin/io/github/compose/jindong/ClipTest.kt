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
@file:OptIn(ExperimentalTestApi::class)

package io.github.compose.jindong

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import io.github.compose.jindong.core.dsl.buildHapticPattern
import io.github.compose.jindong.core.ms
import io.github.compose.jindong.dsl.Clip
import io.github.compose.jindong.dsl.Delay
import io.github.compose.jindong.executor.LocalHapticExecutor
import io.github.compose.jindong.executor.RecordingHapticExecutor
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private val onePulse = buildHapticPattern { haptic(50.ms) }
private val twoPulses = buildHapticPattern {
  haptic(50.ms)
  delay(50.ms)
  haptic(50.ms)
}

class ClipTest :
  FunSpec({
    test("Clip emits the clipped pattern's events, offset by preceding nodes") {
      runComposeUiTest {
        val recorder = RecordingHapticExecutor()

        setContent {
          CompositionLocalProvider(LocalHapticExecutor provides recorder) {
            Jindong(Unit) {
              Delay(200.ms)
              Clip(onePulse)
            }
          }
        }

        waitForIdle()

        val events = recorder.executedPatterns.last().events
        events.size shouldBe 1
        // PatternElement.collectEvents offsets the clip by the 200ms Delay preceding it.
        events.single().startTimeMs shouldBe 200
        events.single().durationMs shouldBe 50
      }
    }

    test("Clip re-fires with the new pattern when it is threaded through the key") {
      runComposeUiTest {
        val recorder = RecordingHapticExecutor()
        val patternState = mutableStateOf(onePulse)

        setContent {
          val pattern by patternState
          CompositionLocalProvider(LocalHapticExecutor provides recorder) {
            Jindong(pattern) {
              Clip(pattern)
            }
          }
        }

        waitForIdle()
        recorder.executedPatterns.last().events.size shouldBe 1

        patternState.value = twoPulses
        waitForIdle()

        // The pattern is a key, so the composition recompiles and the new clip's events are emitted.
        recorder.executedPatterns.last().events.size shouldBe 2
      }
    }

    test("Clip freezes on the first pattern when it is not a key") {
      runComposeUiTest {
        val recorder = RecordingHapticExecutor()
        val patternState = mutableStateOf(onePulse)

        setContent {
          val pattern by patternState
          CompositionLocalProvider(LocalHapticExecutor provides recorder) {
            // Unit is the only key: swapping patternState never invalidates the compiled result.
            Jindong(Unit) {
              Clip(pattern)
            }
          }
        }

        waitForIdle()
        recorder.executedPatterns.last().events.size shouldBe 1

        patternState.value = twoPulses
        waitForIdle()

        // Contract guard: without threading the pattern through the key the first compile is stale,
        // so the executor still holds the one-event pattern.
        recorder.executedPatterns.last().events.size shouldBe 1
      }
    }
  })
