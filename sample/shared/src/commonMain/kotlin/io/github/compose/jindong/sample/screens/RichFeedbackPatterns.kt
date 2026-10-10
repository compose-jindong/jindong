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
package io.github.compose.jindong.sample.screens

import io.github.compose.jindong.core.dsl.buildHapticPattern
import io.github.compose.jindong.core.dsl.hapticCurve
import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.model.HapticPattern
import io.github.compose.jindong.core.ms

internal val correctFeedback: HapticPattern = buildHapticPattern {
  repeat(3) {
    transient(HapticIntensity.STRONG, sharpness = 0.9f)
    delay(80.ms)
  }
}

internal val celebrationFeedback: HapticPattern = buildHapticPattern {
  include(correctFeedback)
  continuous(
    300.ms,
    intensityCurve = hapticCurve {
      point(0.ms, 0f)
      point(80.ms, 0.8f)
      point(300.ms, 0f)
    },
    sharpnessCurve = hapticCurve {
      point(0.ms, 0.2f)
      point(300.ms, 0.8f)
    },
  )
}

internal val errorFeedback: HapticPattern = buildHapticPattern {
  parallel {
    continuous(
      300.ms,
      sharpness = 0.2f,
      intensityCurve = hapticCurve {
        point(0.ms, 0.7f)
        point(120.ms, 0.3f)
        point(300.ms, 0f)
      },
    )
    transient(HapticIntensity.HIGH, sharpness = 0.9f)
  }
  delay(100.ms)
}

internal data class RichFeedbackPreset(val name: String, val pattern: HapticPattern)

internal val richFeedbackPresets: List<RichFeedbackPreset> = listOf(
  RichFeedbackPreset("Correct", correctFeedback),
  RichFeedbackPreset("Celebration", celebrationFeedback),
  RichFeedbackPreset("Error", errorFeedback),
)
