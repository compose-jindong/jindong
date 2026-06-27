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
import io.github.compose.jindong.core.executor.HapticExecutor
import io.github.compose.jindong.core.executor.HapticHandle
import io.github.compose.jindong.core.model.HapticPattern
import io.github.compose.jindong.core.ms
import io.github.compose.jindong.dsl.Haptic
import io.github.compose.jindong.dsl.RepeatWithIndex
import io.github.compose.jindong.executor.LocalHapticExecutor
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Records executed patterns so a test can inspect what [Jindong] played for a given key.
 */
private class RecordingHapticExecutor : HapticExecutor {
  override val isSupported: Boolean = true

  val executedPatterns = mutableListOf<HapticPattern>()

  override suspend fun execute(pattern: HapticPattern) {
    executedPatterns += pattern
  }

  override fun executeAsync(pattern: HapticPattern): HapticHandle = NoopHapticHandle

  override fun release() = Unit

  private object NoopHapticHandle : HapticHandle {
    override val isActive: Boolean = false
    override fun cancel() = Unit
  }
}

class JindongReactiveTest :
  FunSpec({
    test("Jindong recompiles its pattern when the key changes") {
      runComposeUiTest {
        val recorder = RecordingHapticExecutor()
        val countState = mutableStateOf(2)

        setContent {
          val count by countState
          CompositionLocalProvider(LocalHapticExecutor provides recorder) {
            Jindong(count) {
              RepeatWithIndex(count) {
                Haptic(50.ms)
              }
            }
          }
        }

        waitForIdle()
        // Initial key = 2 -> RepeatWithIndex emits 2 events.
        recorder.executedPatterns.last().events.size shouldBe 2

        countState.value = 5
        waitForIdle()

        // After the key changes to 5, the captured content must recompile to 5 events,
        // not return the stale first-compile result.
        recorder.executedPatterns.last().events.size shouldBe 5
      }
    }
  })
