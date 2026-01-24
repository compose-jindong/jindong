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
package io.github.compose.jindong.model

import kotlin.time.Duration

/**
 * iOS Core Haptics properties. Ignored on Android.
 */
@ConsistentCopyVisibility
data class HapticProperties internal constructor(
  val sharpness: Float? = null,
  val attackTime: Duration? = null,
  val decayTime: Duration? = null,
  val releaseTime: Duration? = null,
) {
  init {
    attackTime?.let {
      require(!it.isNegative()) { "attackTime must be non-negative" }
    }
    decayTime?.let {
      require(!it.isNegative()) { "decayTime must be non-negative" }
    }
    releaseTime?.let {
      require(!it.isNegative()) { "releaseTime must be non-negative" }
    }
  }

  fun sharpness(value: Float): HapticProperties = copy(sharpness = value)

  fun envelope(
    attack: Duration? = null,
    decay: Duration? = null,
    release: Duration? = null,
  ): HapticProperties = copy(
    attackTime = attack ?: attackTime,
    decayTime = decay ?: decayTime,
    releaseTime = release ?: releaseTime,
  )

  companion object {
    fun sharpness(value: Float): HapticProperties = HapticProperties(sharpness = value)

    fun envelope(
      attack: Duration? = null,
      decay: Duration? = null,
      release: Duration? = null,
    ): HapticProperties = HapticProperties(
      attackTime = attack,
      decayTime = decay,
      releaseTime = release,
    )
  }
}

internal fun HapticProperties.toIosHapticParameters(): IosHapticParameters = IosHapticParameters(
  sharpness = sharpness ?: 0.5f,
  attackTime = attackTime,
  decayTime = decayTime,
  releaseTime = releaseTime,
)
