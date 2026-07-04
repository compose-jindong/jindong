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
package io.github.compose.jindong.sample.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Self-defined [ImageVector]s for the harness, replacing emoji/text glyphs that render as inconsistent
 * color emoji on iOS. All icons live on a 24x24 viewport; filled icons (Play) tint via the [Icon]
 * `tint`, stroked icons (chevrons, close, theme) use a 2px stroke also driven by `tint`.
 *
 * Built lazily and memoized so the path is allocated once per call site.
 */
object JindongIcons {

  /** Solid right-pointing play triangle. */
  val Play: ImageVector
    get() = cache("play") {
      filled {
        moveTo(8f, 5f)
        lineTo(19f, 12f)
        lineTo(8f, 19f)
        close()
      }
    }

  /** Crescent moon (light theme indicator: tap to go dark). */
  val Moon: ImageVector
    get() = cache("moon") {
      filled {
        // Crescent = outer circle minus an offset circle, expressed as a single subpath.
        moveTo(21f, 14.5f)
        arcToRelative(8f, 8f, 0f, true, true, -9.5f, -11f)
        arcToRelative(6.2f, 6.2f, 0f, false, false, 9.5f, 11f)
        close()
      }
    }

  /** Sun with rays (dark theme indicator: tap to go light). */
  val Sun: ImageVector
    get() = cache("sun") {
      stroked {
        // Core
        moveTo(12f, 8.2f)
        arcToRelative(3.8f, 3.8f, 0f, true, true, 0f, 7.6f)
        arcToRelative(3.8f, 3.8f, 0f, true, true, 0f, -7.6f)
        close()
        // Rays
        moveTo(12f, 2.5f)
        lineTo(12f, 4.5f)
        moveTo(12f, 19.5f)
        lineTo(12f, 21.5f)
        moveTo(2.5f, 12f)
        lineTo(4.5f, 12f)
        moveTo(19.5f, 12f)
        lineTo(21.5f, 12f)
        moveTo(5.2f, 5.2f)
        lineTo(6.6f, 6.6f)
        moveTo(17.4f, 17.4f)
        lineTo(18.8f, 18.8f)
        moveTo(18.8f, 5.2f)
        lineTo(17.4f, 6.6f)
        moveTo(6.6f, 17.4f)
        lineTo(5.2f, 18.8f)
      }
    }

  /** Left chevron (back). */
  val ChevronLeft: ImageVector
    get() = cache("chevron-left") {
      stroked {
        moveTo(15f, 5f)
        lineTo(8f, 12f)
        lineTo(15f, 19f)
      }
    }

  /** Right chevron (navigation affordance). */
  val ChevronRight: ImageVector
    get() = cache("chevron-right") {
      stroked {
        moveTo(9f, 5f)
        lineTo(16f, 12f)
        lineTo(9f, 19f)
      }
    }

  /** Down caret (accordion expand indicator). */
  val CaretDown: ImageVector
    get() = cache("caret-down") {
      stroked {
        moveTo(5f, 9f)
        lineTo(12f, 16f)
        lineTo(19f, 9f)
      }
    }

  /** Close / delete (X). */
  val Close: ImageVector
    get() = cache("close") {
      stroked {
        moveTo(6f, 6f)
        lineTo(18f, 18f)
        moveTo(18f, 6f)
        lineTo(6f, 18f)
      }
    }
}

private val iconCache = HashMap<String, ImageVector>()

private fun cache(name: String, build: ImageVector.Builder.() -> Unit): ImageVector = iconCache.getOrPut(name) {
  ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
  ).apply(build).build()
}

/** Adds a filled path tinted by the [Icon] color (fillAlpha 1). */
private fun ImageVector.Builder.filled(pathBuilder: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) {
  path(
    fill = SolidColor(Color.Black),
    stroke = null,
    pathBuilder = pathBuilder,
  )
}

/** Adds a 2px stroked path tinted by the [Icon] color (no fill). */
private fun ImageVector.Builder.stroked(pathBuilder: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) {
  path(
    fill = null,
    stroke = SolidColor(Color.Black),
    strokeLineWidth = 2f,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
    pathBuilder = pathBuilder,
  )
}
