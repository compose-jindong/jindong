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
package io.github.compose.jindong.core.dsl

import io.github.compose.jindong.core.element.DelayElement
import io.github.compose.jindong.core.element.HapticElement
import io.github.compose.jindong.core.element.PatternElement
import io.github.compose.jindong.core.element.RepeatElement
import io.github.compose.jindong.core.element.SequenceElement
import io.github.compose.jindong.core.element.VibrationElement
import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.model.HapticPattern
import io.github.compose.jindong.core.model.IosHapticParameters
import kotlin.time.Duration

/**
 * DSL scope for building haptic patterns.
 *
 * This scope provides functions to define haptic events, delays, sequences,
 * and repetitions in a declarative manner.
 *
 * ## Example
 * ```kotlin
 * val pattern = buildHapticPattern {
 *     haptic(100.ms)
 *     delay(50.ms)
 *     repeat(3) {
 *         haptic(25.ms, HapticIntensity.STRONG)
 *     }
 * }
 * ```
 *
 * @see buildHapticPattern
 */
@HapticDslMarker
class HapticPatternScope internal constructor() {

  internal val rootElement: SequenceElement = SequenceElement()

  /**
   * Adds a haptic vibration event.
   *
   * @param duration Duration of the vibration
   * @param intensity Vibration intensity (default: [HapticIntensity.MEDIUM])
   * @param iosParameters iOS Core Haptics parameters (optional, ignored on Android)
   */
  fun haptic(
    duration: Duration,
    intensity: HapticIntensity = HapticIntensity.MEDIUM,
    iosParameters: IosHapticParameters? = null,
  ) {
    rootElement.children.add(
      VibrationElement(
        durationMs = duration.inWholeMilliseconds,
        intensity = intensity,
        iosParameters = iosParameters,
      ),
    )
  }

  /**
   * Adds a delay (silent pause) between haptic events.
   *
   * @param duration Duration of the delay
   */
  fun delay(duration: Duration) {
    rootElement.children.add(
      DelayElement(durationMs = duration.inWholeMilliseconds),
    )
  }

  /**
   * Repeats the given block [count] times.
   *
   * Example:
   * ```kotlin
   * repeat(3) {
   *     haptic(50.ms)
   *     delay(25.ms)
   * }
   * ```
   *
   * @param count Number of repetitions
   * @param block Block to repeat
   */
  fun repeat(count: Int, block: HapticPatternScope.() -> Unit) {
    val repeatElement = RepeatElement(count)
    val innerScope = HapticPatternScope()
    innerScope.block()
    repeatElement.children.addAll(innerScope.rootElement.children)
    rootElement.children.add(repeatElement)
  }

  /**
   * Repeats the given block [count] times with access to the current iteration index.
   *
   * This is useful for creating patterns that change with each iteration,
   * such as fading effects.
   *
   * Example:
   * ```kotlin
   * repeatWithIndex(5) { index ->
   *     val intensity = HapticIntensity.Custom(1.0f - (index * 0.2f))
   *     haptic(50.ms, intensity)
   *     delay(25.ms)
   * }
   * ```
   *
   * @param count Number of repetitions
   * @param block Block to repeat with iteration index (0-based)
   */
  fun repeatWithIndex(count: Int, block: HapticPatternScope.(index: Int) -> Unit) {
    for (i in 0 until count) {
      block(i)
    }
  }

  /**
   * Groups child elements into a sequence.
   *
   * This is mainly useful for organizing complex patterns, as elements
   * are already sequential by default.
   *
   * Example:
   * ```kotlin
   * sequence {
   *     haptic(100.ms)
   *     delay(50.ms)
   * }
   * ```
   *
   * @param block Block containing sequenced elements
   */
  fun sequence(block: HapticPatternScope.() -> Unit) {
    val sequenceElement = SequenceElement()
    val innerScope = HapticPatternScope()
    innerScope.block()
    sequenceElement.children.addAll(innerScope.rootElement.children)
    rootElement.children.add(sequenceElement)
  }

  /**
   * Includes an existing [HapticPattern] at the current position.
   *
   * This enables pattern composition and reuse.
   *
   * Example:
   * ```kotlin
   * val clickPattern = buildHapticPattern {
   *     haptic(50.ms, HapticIntensity.MEDIUM)
   * }
   *
   * val composedPattern = buildHapticPattern {
   *     include(clickPattern)
   *     delay(100.ms)
   *     include(clickPattern)
   * }
   * ```
   *
   * @param pattern The pattern to include
   */
  fun include(pattern: HapticPattern) {
    rootElement.children.add(PatternElement(pattern))
  }

  internal fun build(): HapticPattern {
    val events = rootElement.collectEvents(startTimeMs = 0L)
    return HapticPattern(events = events, rootElement = rootElement)
  }
}

/**
 * Builds a [HapticPattern] using the haptic DSL.
 *
 * This is the main entry point for creating haptic patterns without Compose.
 *
 * ## Example
 * ```kotlin
 * val pattern = buildHapticPattern {
 *     haptic(100.ms)
 *     delay(50.ms)
 *     repeat(2) {
 *         haptic(30.ms, HapticIntensity.STRONG)
 *     }
 * }
 *
 * // Execute the pattern
 * lifecycleScope.launch {
 *     pattern.play()
 * }
 * ```
 *
 * @param block The DSL block for defining the pattern
 * @return The constructed [HapticPattern]
 * @see HapticPatternScope
 */
fun buildHapticPattern(block: HapticPatternScope.() -> Unit): HapticPattern {
  val scope = HapticPatternScope()
  scope.block()
  return scope.build()
}
