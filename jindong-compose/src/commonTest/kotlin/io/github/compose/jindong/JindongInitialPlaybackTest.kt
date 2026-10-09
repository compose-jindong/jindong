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
@file:OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)

package io.github.compose.jindong

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import io.github.compose.jindong.compose.JindongApplier
import io.github.compose.jindong.core.element.SequenceElement
import io.github.compose.jindong.core.ms
import io.github.compose.jindong.dsl.Haptic
import io.github.compose.jindong.executor.LocalHapticExecutor
import io.github.compose.jindong.executor.RecordingHapticExecutor
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class JindongInitialPlaybackTest :
  FunSpec({
    test("a Boolean key preserves default initial playback") {
      runComposeUiTest {
        val recorder = RecordingHapticExecutor()
        val trigger = mutableStateOf(false)
        setContent {
          CompositionLocalProvider(LocalHapticExecutor provides recorder) {
            Jindong(trigger.value) { Haptic(50.ms) }
          }
        }

        waitForIdle()
        recorder.executedPatterns.size shouldBe 1
        trigger.value = true
        waitForIdle()
        recorder.executedPatterns.size shouldBe 2
      }
    }

    test("explicit initial playback plays once on entry") {
      runComposeUiTest {
        val recorder = RecordingHapticExecutor()
        setContent {
          CompositionLocalProvider(LocalHapticExecutor provides recorder) {
            Jindong(Unit, playOnInitialComposition = true) { Haptic(50.ms) }
          }
        }

        waitForIdle()
        recorder.executedPatterns.size shouldBe 1
      }
    }

    test("initial suppression freezes non-key values until a key change") {
      runComposeUiTest {
        val recorder = RecordingHapticExecutor()
        val trigger = mutableStateOf(0)
        val duration = mutableStateOf(50)
        var compilations = 0
        setContent {
          val key by trigger
          val durationMs by duration
          CompositionLocalProvider(LocalHapticExecutor provides recorder) {
            Jindong(key, playOnInitialComposition = false) {
              compilations++
              Haptic(durationMs.ms)
            }
          }
        }

        waitForIdle()
        recorder.executedPatterns.size shouldBe 0
        compilations shouldBe 1

        duration.value = 200
        waitForIdle()
        recorder.executedPatterns.size shouldBe 0
        compilations shouldBe 1

        trigger.value = 1
        waitForIdle()
        recorder.executedPatterns.size shouldBe 1
        recorder.executedPatterns.last().events.single().durationMs shouldBe 200
        compilations shouldBe 2

        duration.value = 300
        waitForIdle()
        recorder.executedPatterns.size shouldBe 1
        recorder.executedPatterns.last().events.single().durationMs shouldBe 200
        compilations shouldBe 2

        trigger.value = 0
        waitForIdle()
        recorder.executedPatterns.size shouldBe 2
        recorder.executedPatterns.last().events.single().durationMs shouldBe 300
        compilations shouldBe 3
      }
    }

    test("changing only the initial playback option does not play or recompile") {
      runComposeUiTest {
        val recorder = RecordingHapticExecutor()
        val trigger = mutableStateOf(0)
        val playInitially = mutableStateOf(false)
        var compilations = 0
        setContent {
          CompositionLocalProvider(LocalHapticExecutor provides recorder) {
            Jindong(trigger.value, playOnInitialComposition = playInitially.value) {
              compilations++
              Haptic(50.ms)
            }
          }
        }

        waitForIdle()
        playInitially.value = true
        waitForIdle()
        recorder.executedPatterns.size shouldBe 0
        compilations shouldBe 1

        trigger.value = 1
        waitForIdle()
        recorder.executedPatterns.size shouldBe 1
        compilations shouldBe 2

        playInitially.value = false
        waitForIdle()
        recorder.executedPatterns.size shouldBe 1
        compilations shouldBe 2

        trigger.value = 2
        waitForIdle()
        recorder.executedPatterns.size shouldBe 2
        compilations shouldBe 3
      }
    }

    listOf(false, true).forEach { playInitially ->
      test("re-entry resets the initial playback policy when option is $playInitially") {
        runComposeUiTest {
          val recorder = RecordingHapticExecutor()
          val visible = mutableStateOf(true)
          val trigger = mutableStateOf(0)
          setContent {
            CompositionLocalProvider(LocalHapticExecutor provides recorder) {
              if (visible.value) {
                Jindong(trigger.value, playOnInitialComposition = playInitially) { Haptic(50.ms) }
              }
            }
          }

          val initialCount = if (playInitially) 1 else 0
          waitForIdle()
          recorder.executedPatterns.size shouldBe initialCount
          trigger.value = 1
          waitForIdle()
          recorder.executedPatterns.size shouldBe initialCount + 1

          visible.value = false
          waitForIdle()
          trigger.value = 0
          visible.value = true
          waitForIdle()
          recorder.executedPatterns.size shouldBe initialCount * 2 + 1

          trigger.value = 1
          waitForIdle()
          recorder.executedPatterns.size shouldBe initialCount * 2 + 2
        }
      }
    }

    test("a key change after the first commit but before its effect runs still plays") {
      runTest {
        val recorder = RecordingHapticExecutor()
        val trigger = mutableStateOf(0)
        val recomposer = Recomposer(coroutineContext)
        val composition = ControlledComposition(JindongApplier(SequenceElement()), recomposer)
        try {
          composition.setContent {
            CompositionLocalProvider(LocalHapticExecutor provides recorder) {
              Jindong(trigger.value, playOnInitialComposition = false) {
                Haptic((50 + trigger.value).ms)
              }
            }
          }
          recorder.executedPatterns.size shouldBe 0

          // Commit the second key while the test dispatcher still holds the first effect.
          trigger.value = 1
          composition.recordModificationsOf(setOf(trigger))
          composition.recompose() shouldBe true
          composition.applyChanges()
          composition.changesApplied()
          recorder.executedPatterns.size shouldBe 0

          runCurrent()
          recorder.executedPatterns.size shouldBe 1
          recorder.executedPatterns.single().events.single().durationMs shouldBe 51
        } finally {
          composition.dispose()
          recomposer.cancel()
        }
      }
    }
  })
