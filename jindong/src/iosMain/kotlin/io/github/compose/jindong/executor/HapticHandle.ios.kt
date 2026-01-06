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
@file:Suppress("ktlint:standard:filename")

package io.github.compose.jindong.executor

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.CoreHaptics.CHHapticPatternPlayerProtocol
import platform.Foundation.NSError

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class IosHapticHandle(
  private var player: CHHapticPatternPlayerProtocol?,
) : HapticHandle {

  private var _isActive = player != null

  override val isActive: Boolean
    get() = _isActive

  override fun cancel() {
    if (!_isActive) return
    _isActive = false

    memScoped {
      val errorPtr = alloc<ObjCObjectVar<NSError?>>()
      player?.stopAtTime(0.0, errorPtr.ptr)
    }
    player = null
  }
}
