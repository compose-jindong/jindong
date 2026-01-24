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
package io.github.compose.jindong.core.executor

/**
 * Factory function to create a platform-specific [HapticExecutor].
 *
 * The context parameter is platform-specific:
 * - Android: Requires `android.content.Context`
 * - iOS: Can be null (no context needed)
 *
 * For most use cases, prefer using [HapticManager] which handles executor
 * creation and lifecycle automatically.
 *
 * @param context Platform-specific context (Android: Context, iOS: null)
 * @return A platform-specific HapticExecutor implementation
 * @throws IllegalArgumentException if required context is not provided on Android
 */
public expect fun createHapticExecutor(context: Any? = null): HapticExecutor
