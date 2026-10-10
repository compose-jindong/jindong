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
@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package io.github.compose.jindong.core.executor

import io.github.compose.jindong.core.model.HapticEventType
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreHaptics.CHHapticAdvancedPatternPlayerProtocol
import platform.CoreHaptics.CHHapticDynamicParameterIDHapticIntensityControl
import platform.CoreHaptics.CHHapticDynamicParameterIDHapticSharpnessControl
import platform.CoreHaptics.CHHapticEngine
import platform.CoreHaptics.CHHapticEvent
import platform.CoreHaptics.CHHapticEventParameter
import platform.CoreHaptics.CHHapticEventParameterIDHapticIntensity
import platform.CoreHaptics.CHHapticEventParameterIDHapticSharpness
import platform.CoreHaptics.CHHapticEventTypeHapticContinuous
import platform.CoreHaptics.CHHapticEventTypeHapticTransient
import platform.CoreHaptics.CHHapticParameterCurve
import platform.CoreHaptics.CHHapticParameterCurveControlPoint
import platform.CoreHaptics.CHHapticPattern
import platform.Foundation.NSError

internal interface IosHapticDriver {
  val supportsHaptics: Boolean
  fun createEngine(): IosHapticEngine
}

internal interface IosHapticEngine {
  val currentTimeSeconds: Double
  var onStopped: () -> Unit
  var onReset: () -> Unit
  fun start()
  fun createPlayer(plan: IosPlayerPlan): IosHapticPlayer
  fun dispose()
}

internal interface IosHapticPlayer {
  var onCompletion: (Throwable?) -> Unit
  fun start(timeSeconds: Double)
  fun stop()
}

internal object CoreHapticsDriver : IosHapticDriver {
  override val supportsHaptics: Boolean by lazy { CHHapticEngine.capabilitiesForHardware().supportsHaptics() }
  override fun createEngine(): IosHapticEngine = memScoped {
    val error = alloc<ObjCObjectVar<NSError?>>()
    val engine = CHHapticEngine(error.ptr)
    check(error.value == null) { "Could not create haptic engine: ${error.value?.localizedDescription}" }
    CoreHapticsEngine(engine)
  }
}

private class CoreHapticsEngine(private val native: CHHapticEngine) : IosHapticEngine {
  override val currentTimeSeconds: Double get() = native.currentTime
  override var onStopped: () -> Unit = { }
    set(value) {
      field = value
      native.stoppedHandler = { _ -> value() }
    }
  override var onReset: () -> Unit = { }
    set(value) {
      field = value
      native.resetHandler = value
    }

  override fun start() = memScoped {
    val error = alloc<ObjCObjectVar<NSError?>>()
    check(native.startAndReturnError(error.ptr) && error.value == null) {
      "Could not start haptic engine: ${error.value?.localizedDescription}"
    }
  }

  override fun createPlayer(plan: IosPlayerPlan): IosHapticPlayer = memScoped {
    val pattern = plan.toCHHapticPattern()
    val error = alloc<ObjCObjectVar<NSError?>>()
    val player = native.createAdvancedPlayerWithPattern(pattern, error.ptr)
    if (player == null || error.value != null) {
      val failure = IllegalStateException("Could not create haptic player: ${error.value?.localizedDescription}")
      try {
        player?.let { CoreHapticsPlayer(it).stop() }
      } catch (cleanupError: Throwable) {
        failure.addSuppressed(cleanupError)
      }
      throw failure
    }
    CoreHapticsPlayer(player)
  }

  override fun dispose() {
    native.stopWithCompletionHandler(null)
  }
}

private class CoreHapticsPlayer(private val native: CHHapticAdvancedPatternPlayerProtocol) : IosHapticPlayer {
  override var onCompletion: (Throwable?) -> Unit = { }
    set(value) {
      field = value
      native.completionHandler = { error ->
        value(error?.let { IllegalStateException("Core Haptics player failed: ${it.localizedDescription}") })
      }
    }

  override fun start(timeSeconds: Double) = memScoped {
    val error = alloc<ObjCObjectVar<NSError?>>()
    check(native.startAtTime(timeSeconds, error.ptr) && error.value == null) {
      "Could not start haptic player: ${error.value?.localizedDescription}"
    }
  }

  override fun stop() = memScoped {
    val error = alloc<ObjCObjectVar<NSError?>>()
    check(native.cancelAndReturnError(error.ptr) && error.value == null) {
      "Could not stop haptic player: ${error.value?.localizedDescription}"
    }
  }
}

internal fun IosPlayerPlan.toCHHapticPattern(): CHHapticPattern = memScoped {
  val error = alloc<ObjCObjectVar<NSError?>>()
  val pattern = CHHapticPattern(
    events = events.map { event ->
      val parameters = listOf(
        CHHapticEventParameter(CHHapticEventParameterIDHapticIntensity, event.intensity),
        CHHapticEventParameter(CHHapticEventParameterIDHapticSharpness, event.sharpness),
      )
      if (event.eventType == HapticEventType.TRANSIENT) {
        CHHapticEvent(CHHapticEventTypeHapticTransient, parameters, event.startTimeMs / 1000.0)
      } else {
        CHHapticEvent(CHHapticEventTypeHapticContinuous, parameters, event.startTimeMs / 1000.0, event.durationMs / 1000.0)
      }
    },
    parameterCurves = curves.map { curve ->
      CHHapticParameterCurve(
        parameterID = when (curve.parameter) {
          IosCurveParameter.INTENSITY -> CHHapticDynamicParameterIDHapticIntensityControl
          IosCurveParameter.SHARPNESS -> CHHapticDynamicParameterIDHapticSharpnessControl
        },
        controlPoints = curve.points.map { CHHapticParameterCurveControlPoint(it.timeMs / 1000.0, it.value) },
        relativeTime = curve.startTimeMs / 1000.0,
      )
    },
    error = error.ptr,
  )
  check(error.value == null) { "Could not create haptic pattern: ${error.value?.localizedDescription}" }
  pattern
}
