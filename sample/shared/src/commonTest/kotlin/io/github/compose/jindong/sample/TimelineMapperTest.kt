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
package io.github.compose.jindong.sample

import androidx.compose.ui.graphics.Color
import io.github.compose.jindong.core.dsl.buildHapticPattern
import io.github.compose.jindong.core.dsl.hapticCurve
import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.ms
import io.github.compose.jindong.sample.components.TimelineBar
import io.github.compose.jindong.sample.components.TimelineMapper
import io.github.compose.jindong.sample.screens.celebrationFeedback
import io.github.compose.jindong.sample.screens.correctFeedback
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimelineMapperTest {
  @Test
  fun curveBarsPreserveRelativeControlPointBoundariesAndPeak() {
    val bars = TimelineMapper.toBars(celebrationFeedback, 540L) { Color.Red }
    assertEquals(5, bars.size)
    val rise = bars[3]
    val fall = bars[4]
    assertEquals(240f / 540f, rise.leftFraction, 0.00001f)
    assertEquals(80f / 540f, rise.widthFraction, 0.00001f)
    assertEquals(320f / 540f, fall.leftFraction, 0.00001f)
    assertEquals(220f / 540f, fall.widthFraction, 0.00001f)
    assertEquals(0f, rise.heightFraction)
    assertEquals(0.8f * 0.98f, rise.endHeightFraction)
    assertEquals(rise.endHeightFraction, fall.heightFraction)
    assertEquals(0f, fall.endHeightFraction)
    assertEquals(1f, fall.leftFraction + fall.widthFraction, 0.00001f)
  }

  @Test
  fun transientsKeepVisibleWidthAtTheirAuthoredTimes() {
    val bars = TimelineMapper.toBars(correctFeedback, 240L) { Color.Red }
    assertEquals(listOf(0f, 80f / 240f, 160f / 240f), bars.map { it.leftFraction })
    assertTrue(bars.all { it.widthFraction > 0f && it.heightFraction > 0f })
  }

  @Test
  fun silentFixedEventsAndZeroCurvesDoNotGainMinimumHeight() {
    val pattern = buildHapticPattern {
      haptic(20.ms, HapticIntensity.Custom(0f))
      transient(HapticIntensity.Custom(0f))
      continuous(
        10.ms,
        intensityCurve = hapticCurve {
          point(0.ms, 0f)
          point(10.ms, 0f)
        },
      )
    }
    val bars = TimelineMapper.toBars(pattern, 30L) { Color.Red }
    assertEquals(3, bars.size)
    assertTrue(bars.all { it.heightFraction == 0f && it.endHeightFraction == 0f })
  }

  @Test
  fun invalidWindowProducesNoBarsAndFixedBarsKeepTheirEndpointHeight() {
    assertEquals(emptyList(), TimelineMapper.toBars(correctFeedback, 0L) { Color.Red })
    assertEquals(emptyList(), TimelineMapper.toBars(correctFeedback, -1L) { Color.Red })
    assertEquals(0.5f, TimelineBar(0f, 0.2f, 0.5f, Color.Red).endHeightFraction)
  }
}
