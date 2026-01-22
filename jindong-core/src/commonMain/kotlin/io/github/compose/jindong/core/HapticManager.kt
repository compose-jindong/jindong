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
package io.github.compose.jindong.core

import io.github.compose.jindong.core.dsl.HapticPatternScope
import io.github.compose.jindong.core.dsl.buildHapticPattern
import io.github.compose.jindong.core.executor.HapticExecutor
import io.github.compose.jindong.core.executor.HapticHandle
import io.github.compose.jindong.core.executor.createHapticExecutor
import io.github.compose.jindong.core.model.HapticPattern
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Singleton manager for haptic pattern execution.
 *
 * [HapticManager] provides a convenient way to play haptic patterns without
 * manually managing executor lifecycle. It automatically:
 * - Initializes the platform-specific executor on first use
 * - Cancels any ongoing vibration when starting a new pattern
 * - Provides thread-safe pattern execution via Mutex
 *
 * ## Usage
 *
 * ### With existing pattern
 * ```kotlin
 * val pattern = buildHapticPattern {
 *     haptic(100.ms)
 *     delay(50.ms)
 *     haptic(100.ms)
 * }
 *
 * // Suspend function - waits for completion
 * pattern.play()
 *
 * // Or fire-and-forget
 * pattern.playAsync()
 * ```
 *
 * ### Inline pattern execution
 * ```kotlin
 * playHaptic {
 *     haptic(100.ms)
 *     delay(50.ms)
 *     haptic(100.ms, HapticIntensity.STRONG)
 * }
 * ```
 *
 * ## Platform Setup
 *
 * ### Android
 * The context is automatically obtained via [JindongInitializer] ContentProvider.
 * No manual setup is required for most apps.
 *
 * For manual initialization (e.g., in multi-process apps):
 * ```kotlin
 * HapticManager.initialize(applicationContext)
 * ```
 *
 * ### iOS
 * No setup required. The executor is created automatically.
 *
 * @see buildHapticPattern
 * @see play
 * @see playAsync
 * @see playHaptic
 */
public object HapticManager {

  private val mutex = Mutex()
  private var executor: HapticExecutor? = null
  private var currentHandle: HapticHandle? = null

  /**
   * Initializes the HapticManager with the given context.
   *
   * On Android, this is typically called automatically by [JindongInitializer].
   * Call this manually only if automatic initialization doesn't work (e.g., multi-process apps).
   *
   * On iOS, this is a no-op as no context is needed.
   *
   * @param context Platform-specific context (Android: Context, iOS: null)
   */
  public fun initialize(context: Any? = null) {
    if (executor == null) {
      executor = createHapticExecutor(context)
    }
  }

  /**
   * Returns whether haptic feedback is supported on this device.
   *
   * @return true if haptic feedback is supported
   * @throws IllegalStateException if HapticManager is not initialized
   */
  public val isSupported: Boolean
    get() = requireExecutor().isSupported

  /**
   * Executes the given pattern. This is a suspend function that waits for completion.
   *
   * Any ongoing vibration is automatically cancelled before starting the new pattern.
   *
   * @param pattern The haptic pattern to execute
   */
  public suspend fun execute(pattern: HapticPattern) {
    mutex.withLock {
      currentHandle?.cancel()
      currentHandle = null
      requireExecutor().execute(pattern)
    }
  }

  /**
   * Executes the given pattern asynchronously (fire-and-forget).
   *
   * Any ongoing vibration is automatically cancelled before starting the new pattern.
   *
   * @param pattern The haptic pattern to execute
   * @return A [HapticHandle] for cancelling the execution
   */
  public fun executeAsync(pattern: HapticPattern): HapticHandle {
    currentHandle?.cancel()
    val handle = requireExecutor().executeAsync(pattern)
    currentHandle = handle
    return handle
  }

  /**
   * Cancels any ongoing haptic execution.
   */
  public fun cancel() {
    currentHandle?.cancel()
    currentHandle = null
  }

  /**
   * Releases resources held by the manager.
   *
   * After calling this, the manager can still be used - it will reinitialize on next use.
   */
  public fun release() {
    currentHandle?.cancel()
    currentHandle = null
    executor?.release()
    executor = null
  }

  private fun requireExecutor(): HapticExecutor {
    return executor ?: run {
      // Try to get platform context automatically
      val context = getPlatformContext()
      val newExecutor = createHapticExecutor(context)
      executor = newExecutor
      newExecutor
    }
  }
}

/**
 * Executes this haptic pattern using [HapticManager].
 *
 * This is a suspend function that waits for the pattern to complete.
 * Any ongoing vibration is automatically cancelled.
 *
 * Example:
 * ```kotlin
 * val pattern = buildHapticPattern {
 *     haptic(100.ms)
 *     delay(50.ms)
 *     haptic(100.ms)
 * }
 *
 * lifecycleScope.launch {
 *     pattern.play()
 * }
 * ```
 */
public suspend fun HapticPattern.play() {
  HapticManager.execute(this)
}

/**
 * Executes this haptic pattern asynchronously using [HapticManager].
 *
 * This is a fire-and-forget method that returns immediately.
 * Any ongoing vibration is automatically cancelled.
 *
 * Example:
 * ```kotlin
 * val pattern = buildHapticPattern {
 *     haptic(100.ms)
 * }
 *
 * // Fire and forget
 * val handle = pattern.playAsync()
 *
 * // Optionally cancel later
 * handle.cancel()
 * ```
 *
 * @return A [HapticHandle] for cancelling the execution
 */
public fun HapticPattern.playAsync(): HapticHandle {
  return HapticManager.executeAsync(this)
}

/**
 * Builds and executes a haptic pattern inline.
 *
 * This is a convenience function that combines [buildHapticPattern] and [play].
 *
 * Example:
 * ```kotlin
 * lifecycleScope.launch {
 *     playHaptic {
 *         haptic(100.ms)
 *         delay(50.ms)
 *         haptic(100.ms, HapticIntensity.STRONG)
 *     }
 * }
 * ```
 *
 * @param block The DSL block for defining the pattern
 */
public suspend fun playHaptic(block: HapticPatternScope.() -> Unit) {
  val pattern = buildHapticPattern(block)
  pattern.play()
}

/**
 * Platform-specific function to get the application context.
 *
 * - Android: Returns the application Context from [JindongInitializer]
 * - iOS: Returns null (no context needed)
 */
internal expect fun getPlatformContext(): Any?
