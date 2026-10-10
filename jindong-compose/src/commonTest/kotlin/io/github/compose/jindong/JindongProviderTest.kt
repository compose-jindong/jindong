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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import io.github.compose.jindong.core.executor.HapticExecutor
import io.github.compose.jindong.core.executor.HapticHandle
import io.github.compose.jindong.core.model.HapticPattern
import io.github.compose.jindong.core.ms
import io.github.compose.jindong.dsl.Haptic
import io.github.compose.jindong.executor.LocalHapticExecutor
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.awaitCancellation

class JindongProviderTest :
  FunSpec({
    test("key changes and composition exit cancel the suspended playback") {
      runComposeUiTest {
        val executor = ReleasingExecutor()
        val trigger = mutableStateOf(0)
        val visible = mutableStateOf(true)
        setContent {
          CompositionLocalProvider(LocalHapticExecutor provides executor) {
            if (visible.value) Jindong(trigger.value) { Haptic(1000.ms) }
          }
        }
        waitForIdle()
        trigger.value = 1
        waitForIdle()
        val startsBeforeReplacement = executor.starts
        val cancellationsBeforeReplacement = executor.cancellations
        trigger.value = 2
        waitForIdle()
        executor.starts shouldBe startsBeforeReplacement + 1
        executor.cancellations shouldBe cancellationsBeforeReplacement + 1
        visible.value = false
        waitForIdle()
        executor.cancellations shouldBe cancellationsBeforeReplacement + 2
      }
    }
    test("replacement disposes the previous executor and exit disposes the current one") {
      runComposeUiTest {
        val first = ReleasingExecutor()
        val second = ReleasingExecutor()
        val current = mutableStateOf<HapticExecutor>(first)
        val visible = mutableStateOf(true)
        setContent {
          if (visible.value) ProvideHapticExecutor(current.value) { }
        }
        waitForIdle()
        first.releases shouldBe 0
        current.value = second
        waitForIdle()
        first.releases shouldBe 1
        second.releases shouldBe 0
        visible.value = false
        waitForIdle()
        first.releases shouldBe 1
        second.releases shouldBe 1
      }
    }
  })

private class ReleasingExecutor : HapticExecutor {
  var releases = 0
  var starts = 0
  var cancellations = 0
  override val isSupported = true
  override suspend fun execute(pattern: HapticPattern) {
    starts++
    try {
      awaitCancellation()
    } finally {
      cancellations++
    }
  }
  override fun executeAsync(pattern: HapticPattern): HapticHandle = error("unused")
  override fun release() {
    releases++
  }
}
