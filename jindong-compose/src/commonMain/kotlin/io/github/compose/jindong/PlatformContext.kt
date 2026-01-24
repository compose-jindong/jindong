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
package io.github.compose.jindong

import androidx.compose.runtime.Composable

/**
 * Returns the platform-specific context needed for haptic feedback.
 *
 * - **Android**: Returns `android.content.Context` from `LocalContext.current`
 * - **iOS**: Returns `null` (no context needed)
 *
 * This is used internally by [JindongProvider] to initialize the haptic executor.
 */
@Composable
internal expect fun platformContext(): Any?
