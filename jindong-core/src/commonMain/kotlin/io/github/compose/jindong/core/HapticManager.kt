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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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
object HapticManager {

  private val executionMutex = Mutex()
  private val stateMutex = Mutex()
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
  fun initialize(context: Any? = null) {
    withStateLockBlocking {
      if (executor == null) {
        initializeExecutor(createHapticExecutor(context))
      }
    }
  }

  /**
   * Returns whether haptic feedback is supported on this device.
   *
   * @return true if haptic feedback is supported
   * @throws IllegalStateException if HapticManager is not initialized
   */
  val isSupported: Boolean
    get() = withStateLockBlocking {
      getOrCreateExecutorLocked().isSupported
    }

  /**
   * Executes the given pattern. This is a suspend function that waits for completion.
   *
   * Any ongoing vibration is automatically cancelled before starting the new pattern.
   *
   * @param pattern The haptic pattern to execute
   */
  suspend fun execute(pattern: HapticPattern) {
    executionMutex.withLock {
      // Drive playback through the handle path (not the suspend executor.execute) so the in-flight
      // vibration stays reachable via currentHandle: only HapticHandle.cancel() stops the motor,
      // while a coroutine-cancelled executor.execute() would abort its delay but leave the actuator
      // buzzing. Publishing the handle lets a concurrent executeAsync/execute cancel-and-restart it
      // instead of overlapping.
      val handle = withStateLock {
        currentHandle?.cancel()
        val newHandle = getOrCreateExecutorLocked().executeAsync(pattern)
        currentHandle = newHandle
        newHandle
      }
      try {
        awaitCompletion(handle)
      } finally {
        // Stop the motor and clear the slot on normal end, cancellation, or a takeover by another
        // caller. clearHandleIfCurrent guards against wiping a handle a newer call already installed.
        withContext(NonCancellable) {
          handle.cancel()
          clearHandleIfCurrent(handle)
        }
      }
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
  fun executeAsync(pattern: HapticPattern): HapticHandle = withStateLockBlocking {
    // Shares currentHandle with execute(), so an executeAsync landing mid-execute cancels the
    // in-flight handle here before starting its own, so the two paths never overlap.
    currentHandle?.cancel()
    val handle = getOrCreateExecutorLocked().executeAsync(pattern)
    currentHandle = handle
    handle
  }

  /**
   * Suspends until [handle] is no longer active — natural (duration-estimate) completion, or an
   * external [HapticHandle.cancel] from a concurrent takeover. Polls because neither Android's
   * `Vibrator` nor iOS' `CHHapticPatternPlayerProtocol` reports per-effect completion; the whole
   * library already treats completion as a best-effort estimate (see [HandleExpiry]). The delay is a
   * suspension point, so coroutine cancellation of execute() unwinds here immediately.
   */
  private suspend fun awaitCompletion(handle: HapticHandle) {
    while (handle.isActive) {
      delay(COMPLETION_POLL_INTERVAL_MS)
    }
  }

  /**
   * Cancels any ongoing haptic execution.
   */
  fun cancel() {
    withStateLockBlocking {
      currentHandle?.cancel()
      currentHandle = null
    }
  }

  /**
   * Releases resources held by the manager.
   *
   * After calling this, the manager can still be used - it will reinitialize on next use.
   */
  fun release() {
    withStateLockBlocking {
      currentHandle?.cancel()
      currentHandle = null
      executor?.release()
      executor = null
    }
  }

  internal fun initializeExecutor(executor: HapticExecutor) {
    withStateLockBlocking {
      this.executor = executor
    }
  }

  // Clears currentHandle only when it still points at [handle]. execute()'s cleanup must not wipe a
  // handle that a newer execute/executeAsync already installed after taking over.
  private fun clearHandleIfCurrent(handle: HapticHandle) {
    withStateLockBlocking {
      if (currentHandle === handle) currentHandle = null
    }
  }

  private suspend fun <T> withStateLock(action: () -> T): T = stateMutex.withLock { action() }

  private fun <T> withStateLockBlocking(action: () -> T): T = runBlocking {
    stateMutex.withLock { action() }
  }

  private fun getOrCreateExecutorLocked(): HapticExecutor {
    val existing = executor
    if (existing != null) return existing

    // Try to get platform context automatically
    val context = getPlatformContext()
    val newExecutor = createHapticExecutor(context)
    executor = newExecutor
    return newExecutor
  }

  // Playback has no OS completion callback, so execute() polls the handle. 4ms keeps the caller's
  // resume within one frame of natural end while cancellation (a takeover) still unwinds instantly
  // at the next suspension point.
  private const val COMPLETION_POLL_INTERVAL_MS = 4L
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
suspend fun HapticPattern.play() {
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
fun HapticPattern.playAsync(): HapticHandle = HapticManager.executeAsync(this)

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
suspend fun playHaptic(block: HapticPatternScope.() -> Unit) {
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
