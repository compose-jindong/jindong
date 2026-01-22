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

/**
 * DSL marker to prevent implicit nesting of Jindong scopes.
 */
@DslMarker
annotation class JindongScopeMarker

/**
 * Scope for defining haptic patterns using DSL.
 *
 * This interface is used as a receiver for haptic DSL composables like
 * [Haptic], [Delay], [Sequence], etc.
 *
 * Example:
 * ```
 * Jindong(trigger) {  // JindongScope receiver
 *     Haptic(100.ms)
 *     Delay(50.ms)
 *     Haptic(200.ms)
 * }
 * ```
 */
@JindongScopeMarker
interface JindongScope

/**
 * Default implementation of [JindongScope].
 */
internal class JindongScopeImpl : JindongScope
