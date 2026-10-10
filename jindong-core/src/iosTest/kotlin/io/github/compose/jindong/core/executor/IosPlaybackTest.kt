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

import io.github.compose.jindong.core.model.HapticControlPoint
import io.github.compose.jindong.core.model.HapticCurve
import io.github.compose.jindong.core.model.HapticEventType
import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.model.HapticPattern
import io.github.compose.jindong.core.model.ScheduledHapticEvent
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import platform.CoreHaptics.CHHapticEventParameterIDHapticIntensity
import platform.CoreHaptics.CHHapticEventParameterIDHapticSharpness
import platform.CoreHaptics.CHHapticEventTypeHapticContinuous
import platform.CoreHaptics.CHHapticEventTypeHapticTransient
import platform.CoreHaptics.CHHapticPatternKeyEvent
import platform.CoreHaptics.CHHapticPatternKeyEventParameters
import platform.CoreHaptics.CHHapticPatternKeyEventType
import platform.CoreHaptics.CHHapticPatternKeyParameterCurve
import platform.CoreHaptics.CHHapticPatternKeyParameterCurveControlPoints
import platform.CoreHaptics.CHHapticPatternKeyParameterID
import platform.CoreHaptics.CHHapticPatternKeyParameterValue
import platform.CoreHaptics.CHHapticPatternKeyPattern
import platform.CoreHaptics.CHHapticPatternKeyTime
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

class IosPlaybackTest :
  FunSpec({
    test("all independent players share one absolute engine start and one cancellation handle") {
      val fixture = IosFixture()
      val pattern = mixedPattern()
      val handle = fixture.executor.executeAsync(pattern)
      fixture.engine.players.size shouldBe 3
      fixture.engine.players.map { it.startTimes.single() } shouldBe listOf(12.01, 12.01, 12.01)
      handle.cancel()
      fixture.engine.players.map { it.stops } shouldBe listOf(1, 1, 1)
      fixture.executor.release()
      fixture.engine.disposals shouldBe 1
    }

    test("completion needs every player and never truncates the last transient") {
      val clock = TestTimeSource()
      val fixture = IosFixture(clock)
      val handle = fixture.executor.executeAsync(mixedPattern())
      clock += 510.milliseconds
      handle.isActive shouldBe true
      fixture.engine.players[0].onCompletion(null)
      fixture.engine.players[1].onCompletion(null)
      fixture.drainCallbacks()
      handle.isActive shouldBe true
      fixture.engine.players[2].onCompletion(null)
      fixture.drainCallbacks()
      handle.isActive shouldBe false
      fixture.engine.players.map { it.stops } shouldBe listOf(0, 0, 0)
      fixture.executor.release()
      fixture.engine.players.map { it.stops } shouldBe listOf(0, 0, 0)
    }

    test("native completion preserves trailing logical silence and the scheduling lead") {
      val clock = TestTimeSource()
      val fixture = IosFixture(clock)
      val handle = fixture.executor.executeAsync(HapticPattern(listOf(iosEvent(0, 0, HapticEventType.TRANSIENT)), 100))
      fixture.engine.players.single().onCompletion(null)
      fixture.drainCallbacks()
      clock += 100.milliseconds
      handle.isActive shouldBe true
      clock += 10.milliseconds
      handle.isActive shouldBe false
      fixture.executor.release()
    }

    test("partial player start or creation failure stops every created player and allows retry") {
      for (failCreation in listOf(false, true)) {
        val fixture = IosFixture()
        fixture.engine.failCreationAt = if (failCreation) 1 else null
        fixture.engine.failStartAt = if (failCreation) null else 1
        shouldThrow<IllegalStateException> { fixture.executor.executeAsync(mixedPattern()) }
        fixture.engine.players.all { it.stops == 1 } shouldBe true
        fixture.engine.failCreationAt = null
        fixture.engine.failStartAt = null
        val next = fixture.executor.executeAsync(HapticPattern(listOf(iosEvent(0, 100))))
        next.isActive shouldBe true
        fixture.executor.release()
      }
    }

    test("reset invalidates immediately and its queued callback cannot cancel a replacement engine") {
      for (reset in listOf(true, false)) {
        val fixture = IosFixture()
        val old = fixture.executor.executeAsync(mixedPattern())
        val oldCompletion = fixture.engine.players.first().onCompletion
        val invalidation = if (reset) fixture.engine.onReset else fixture.engine.onStopped
        invalidation()
        val next = fixture.executor.executeAsync(HapticPattern(listOf(iosEvent(0, 100))))
        fixture.driver.engines.size shouldBe 2
        oldCompletion(null)
        invalidation()
        fixture.drainCallbacks()
        old.isActive shouldBe false
        next.isActive shouldBe true
        fixture.driver.engines[0].disposals shouldBe 1
        fixture.driver.engines[1].players.single().stops shouldBe 0
        fixture.executor.release()
      }
    }

    test("reset cancels current feedback without replay and prepares the next trigger") {
      val fixture = IosFixture()
      val handle = fixture.executor.executeAsync(mixedPattern())
      fixture.engine.onReset()
      fixture.drainCallbacks()
      handle.isActive shouldBe false
      fixture.engine.players.map { it.stops } shouldBe listOf(1, 1, 1)
      fixture.driver.engines.size shouldBe 1
      fixture.executor.executeAsync(HapticPattern(listOf(iosEvent(0, 100))))
      fixture.driver.engines.size shouldBe 2
      fixture.executor.release()
    }

    test("synchronous native callbacks are queued outside native start and stop") {
      val clock = TestTimeSource()
      val fixture = IosFixture(clock)
      fixture.engine.completeDuringStart = true
      fixture.engine.completeDuringStop = true
      val handle = fixture.executor.executeAsync(mixedPattern())
      fixture.callbacks.size shouldBe 3
      handle.isActive shouldBe true
      clock += 510.milliseconds
      fixture.drainCallbacks()
      handle.isActive shouldBe false
      fixture.engine.players.all { it.stops == 0 } shouldBe true
      fixture.executor.release()
    }

    test("native completion failure cancels all players and does not affect a later session") {
      val fixture = IosFixture()
      val old = fixture.executor.executeAsync(mixedPattern())
      val failureCallback = fixture.engine.players.first().onCompletion
      failureCallback(IllegalStateException("native callback failed"))
      fixture.drainCallbacks()
      old.isActive shouldBe false
      old.failure?.message shouldBe "native callback failed"
      fixture.engine.players.all { it.stops == 1 } shouldBe true
      val next = fixture.executor.executeAsync(HapticPattern(listOf(iosEvent(0, 100))))
      failureCallback(null)
      fixture.drainCallbacks()
      next.isActive shouldBe true
      fixture.executor.release()
    }

    test("release detaches native callbacks and queued old completion cannot restart playback") {
      val fixture = IosFixture()
      val handle = fixture.executor.executeAsync(mixedPattern())
      fixture.engine.players.first().onCompletion(null)
      fixture.engine.onReset()
      fixture.executor.release()
      fixture.drainCallbacks()
      handle.isActive shouldBe false
      fixture.driver.engines.size shouldBe 1
      fixture.engine.disposals shouldBe 1
      shouldThrow<IllegalStateException> { fixture.executor.executeAsync(mixedPattern()) }
    }

    test("native adapter exports transient types and normalized absolute parameter curves") {
      val plan = mixedPattern().iosPlaybackPlan(HapticDeviceCapabilities(true, true))
      val curved = plan.players.first().toCHHapticPattern().exportDictionaryAndReturnError(null)!!
      val entries = curved[CHHapticPatternKeyPattern] as List<*>
      val nativeEvent = entries.mapNotNull { (it as Map<*, *>)[CHHapticPatternKeyEvent] as? Map<*, *> }.single()
      nativeEvent[CHHapticPatternKeyEventType] shouldBe CHHapticEventTypeHapticContinuous
      val parameters = (nativeEvent[CHHapticPatternKeyEventParameters] as List<*>).associate { entry ->
        val parameter = entry as Map<*, *>
        parameter[CHHapticPatternKeyParameterID] to (parameter[CHHapticPatternKeyParameterValue] as Number).toFloat()
      }
      parameters[CHHapticEventParameterIDHapticIntensity] shouldBe 1f
      parameters[CHHapticEventParameterIDHapticSharpness] shouldBe 0f
      val curves = entries.mapNotNull { (it as Map<*, *>)[CHHapticPatternKeyParameterCurve] as? Map<*, *> }
      curves.size shouldBe 2
      (curves.first()[CHHapticPatternKeyTime] as Number).toDouble() shouldBe 0.025
      val points = curves.first()[CHHapticPatternKeyParameterCurveControlPoints] as List<*>
      points.map { ((it as Map<*, *>)[CHHapticPatternKeyParameterValue] as Number).toFloat() } shouldBe listOf(0f, 1f)
      points.map { ((it as Map<*, *>)[CHHapticPatternKeyTime] as Number).toDouble() } shouldBe listOf(0.0, 0.1)
      val constant = plan.players[1].toCHHapticPattern().exportDictionaryAndReturnError(null)!!
      val transient = (((constant[CHHapticPatternKeyPattern] as List<*>).single() as Map<*, *>)[CHHapticPatternKeyEvent] as Map<*, *>)
      transient[CHHapticPatternKeyEventType] shouldBe CHHapticEventTypeHapticTransient
    }
  })

private fun mixedPattern(): HapticPattern {
  val rise = HapticCurve(listOf(HapticControlPoint(0, 0f), HapticControlPoint(100, 1f)))
  return HapticPattern(
    listOf(
      iosEvent(25, 100, intensityCurve = rise, sharpnessCurve = rise),
      iosEvent(500, 0, HapticEventType.TRANSIENT),
      iosEvent(25, 100, intensityCurve = rise),
    ),
  )
}

private fun iosEvent(
  start: Long,
  duration: Long,
  type: HapticEventType = HapticEventType.CONTINUOUS,
  intensityCurve: HapticCurve? = null,
  sharpnessCurve: HapticCurve? = null,
): ScheduledHapticEvent = ScheduledHapticEvent(
  startTimeMs = start,
  durationMs = duration,
  intensity = HapticIntensity.STRONG,
  eventType = type,
  intensityCurve = intensityCurve,
  sharpnessCurve = sharpnessCurve,
)

private class IosFixture(clock: TestTimeSource = TestTimeSource()) {
  val callbacks = mutableListOf<() -> Unit>()
  val driver = FakeIosDriver()
  val engine: FakeIosEngine get() = driver.engines.first()
  val executor = DefaultIosHapticExecutor(clock, driver) { callbacks += it }
  init {
    driver.engines += FakeIosEngine()
  }
  fun drainCallbacks() {
    while (callbacks.isNotEmpty()) callbacks.removeAt(0).invoke()
  }
}

private class FakeIosDriver : IosHapticDriver {
  override val supportsHaptics: Boolean = true
  val engines = mutableListOf<FakeIosEngine>()
  private var created = 0
  override fun createEngine(): IosHapticEngine {
    if (created == engines.size) engines += FakeIosEngine()
    return engines[created++]
  }
}

private class FakeIosEngine : IosHapticEngine {
  override val currentTimeSeconds: Double = 12.0
  override var onStopped: () -> Unit = { }
  override var onReset: () -> Unit = { }
  val players = mutableListOf<FakeIosPlayer>()
  var failCreationAt: Int? = null
  var failStartAt: Int? = null
  var completeDuringStart = false
  var completeDuringStop = false
  var disposals = 0
  override fun start() { }
  override fun createPlayer(plan: IosPlayerPlan): IosHapticPlayer {
    check(failCreationAt != players.size) { "player creation failed" }
    return FakeIosPlayer(players.size, this).also { players += it }
  }
  override fun dispose() {
    disposals++
  }
}

private class FakeIosPlayer(private val index: Int, private val engine: FakeIosEngine) : IosHapticPlayer {
  override var onCompletion: (Throwable?) -> Unit = { }
  val startTimes = mutableListOf<Double>()
  var stops = 0
  override fun start(timeSeconds: Double) {
    check(engine.failStartAt != index) { "player start failed" }
    startTimes += timeSeconds
    if (engine.completeDuringStart) onCompletion(null)
  }
  override fun stop() {
    stops++
    if (engine.completeDuringStop) onCompletion(null)
  }
}
