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
import io.github.compose.jindong.core.ms
import io.github.compose.jindong.dsl.Haptic
import io.github.compose.jindong.executor.LocalHapticExecutor
import io.github.compose.jindong.executor.RecordingHapticExecutor
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Guards the reactive contract at the [Jindong] level: keys are the only playback trigger, and a
 * value read inside `content` but left out of the keys is frozen at the last key change.
 *
 * The Clip-level counterpart lives in [ClipTest]; this covers a parameter read directly by an
 * inline node.
 */
class JindongContractTest :
  FunSpec({
    test("a parameter read inside content but absent from keys does not re-fire") {
      runComposeUiTest {
        val recorder = RecordingHapticExecutor()
        val triggerKey = mutableStateOf(0)
        val durationMs = mutableStateOf(50)

        setContent {
          val key by triggerKey
          val duration by durationMs
          CompositionLocalProvider(LocalHapticExecutor provides recorder) {
            // duration shapes the pattern but is not a key, so changing it must not fire.
            Jindong(key) {
              Haptic(duration.ms)
            }
          }
        }

        waitForIdle()
        recorder.executedPatterns.size shouldBe 1
        recorder.executedPatterns.last().events.single().durationMs shouldBe 50

        durationMs.value = 200
        waitForIdle()

        // Contract: only a key change triggers playback. Mutating a parameter leaves the executor
        // untouched, so no new pattern is recorded.
        recorder.executedPatterns.size shouldBe 1
        recorder.executedPatterns.last().events.single().durationMs shouldBe 50
      }
    }

    test("a key change re-fires and recompiles content with the current parameter") {
      runComposeUiTest {
        val recorder = RecordingHapticExecutor()
        val triggerKey = mutableStateOf(0)
        val durationMs = mutableStateOf(50)

        setContent {
          val key by triggerKey
          val duration by durationMs
          CompositionLocalProvider(LocalHapticExecutor provides recorder) {
            Jindong(key) {
              Haptic(duration.ms)
            }
          }
        }

        waitForIdle()
        recorder.executedPatterns.size shouldBe 1
        recorder.executedPatterns.last().events.single().durationMs shouldBe 50

        // Update the parameter first: it stays frozen until a key change picks it up.
        durationMs.value = 200
        triggerKey.value = 1
        waitForIdle()

        // The key changed, so playback fires again and the recompiled pattern reflects the
        // parameter value read at that point.
        recorder.executedPatterns.size shouldBe 2
        recorder.executedPatterns.last().events.single().durationMs shouldBe 200
      }
    }
  })
