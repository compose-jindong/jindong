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
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package io.github.compose.jindong.core.executor

import io.github.compose.jindong.core.dsl.buildHapticPattern
import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.model.HapticPattern
import io.github.compose.jindong.core.model.ScheduledHapticEvent
import io.github.compose.jindong.core.ms
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import platform.CoreHaptics.CHHapticPatternKeyEvent
import platform.CoreHaptics.CHHapticPatternKeyEventDuration
import platform.CoreHaptics.CHHapticPatternKeyPattern
import platform.CoreHaptics.CHHapticPatternKeyTime
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

class IosSilentPatternTest :
  FunSpec({
    test("all-zero pattern skips native conversion and returns cancellable silence") {
      val executor = DefaultIosHapticExecutor()
      val pattern = HapticPattern(listOf(event(20, 30, 0f), event(100, 50, 0f)))

      with(executor) { pattern.toCHHapticPattern().shouldBeNull() }
      val handle = executor.executeAsync(pattern)
      handle.isActive shouldBe true
      handle.cancel()
      handle.isActive shouldBe false
    }

    test("silent handles expire at the logical end including trailing silence") {
      val clock = TestTimeSource()
      val executor = DefaultIosHapticExecutor(clock)
      val pattern = HapticPattern(listOf(event(0, 50, 0f)), durationMs = 100L)
      val handle = executor.executeAsync(pattern)
      clock += 50.milliseconds
      handle.isActive shouldBe true
      clock += 50.milliseconds
      handle.isActive shouldBe false
      executor.release()
    }

    test("delay-only execute waits without creating a native player") {
      @OptIn(ExperimentalCoroutinesApi::class)
      runTest {
        val executor = DefaultIosHapticExecutor()
        val silence = buildHapticPattern { delay(100.ms) }
        with(executor) { silence.toCHHapticPattern().shouldBeNull() }
        executor.execute(silence)
        testScheduler.currentTime shouldBe 100L
        executor.release()
      }
    }

    test("native adapter omits silent events without shifting a positive event") {
      val executor = DefaultIosHapticExecutor()
      val pattern = HapticPattern(listOf(event(0, 100, 0f), event(25, 50, 1f), event(100, 50, 0f)))
      val native = with(executor) { pattern.toCHHapticPattern()!! }
      val exported = native.exportDictionaryAndReturnError(null)!!
      val entries = exported[CHHapticPatternKeyPattern] as List<*>
      entries.size shouldBe 1
      val nativeEvent = (entries.single() as Map<*, *>)[CHHapticPatternKeyEvent] as Map<*, *>
      (nativeEvent[CHHapticPatternKeyTime] as Number).toDouble() shouldBe 0.025
      (nativeEvent[CHHapticPatternKeyEventDuration] as Number).toDouble() shouldBe 0.05
      pattern.rawSpanMs() shouldBe 150L
    }
  })

private fun event(start: Long, duration: Long, intensity: Float): ScheduledHapticEvent = ScheduledHapticEvent(
  startTimeMs = start,
  durationMs = duration,
  intensity = HapticIntensity.Custom(intensity),
)
