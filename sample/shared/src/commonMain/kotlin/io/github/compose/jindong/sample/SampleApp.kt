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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.compose.jindong.Jindong
import io.github.compose.jindong.JindongProvider
import io.github.compose.jindong.JindongScope
import io.github.compose.jindong.dsl.Delay
import io.github.compose.jindong.dsl.Haptic
import io.github.compose.jindong.dsl.Repeat
import io.github.compose.jindong.dsl.Sequence
import io.github.compose.jindong.model.HapticIntensity
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SampleApp() {
  MaterialTheme {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = MaterialTheme.colorScheme.background,
    ) {
      JindongProvider {
        HapticPatternListScreen()
      }
    }
  }
}

@Composable
private fun HapticPatternListScreen() {
  val patterns = remember { HapticPatternType.entries }

  Column(
    modifier = Modifier.fillMaxSize(),
  ) {
    Text(
      text = stringResource(Res.string.haptic_patterns_title),
      style = MaterialTheme.typography.headlineMedium,
      modifier = Modifier.padding(16.dp),
    )

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(patterns) { pattern ->
        HapticPatternCard(pattern)
      }
    }
  }
}

@Composable
private fun HapticPatternCard(patternType: HapticPatternType) {
  var triggerCount by remember { mutableIntStateOf(0) }

  Card(
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = patternType.displayName,
          style = MaterialTheme.typography.titleMedium,
        )
        Text(
          text = patternType.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Button(onClick = { triggerCount++ }) {
        Text(stringResource(Res.string.play_button))
      }
    }
  }

  HapticPatternEffect(patternType, triggerCount)
}

@Composable
private fun HapticPatternEffect(patternType: HapticPatternType, trigger: Int) {
  if (trigger == 0) return

  val pattern: @Composable JindongScope.() -> Unit = when (patternType) {
    HapticPatternType.SINGLE_TAP -> {
      { Haptic(50.milliseconds) }
    }

    HapticPatternType.DOUBLE_TAP -> {
      {
        Haptic(30.milliseconds)
        Delay(50.milliseconds)
        Haptic(30.milliseconds)
      }
    }

    HapticPatternType.TRIPLE_TAP -> {
      {
        Repeat(3) {
          Sequence {
            Haptic(25.milliseconds)
            Delay(40.milliseconds)
          }
        }
      }
    }

    HapticPatternType.LIGHT_FEEDBACK -> {
      { Haptic(40.milliseconds, HapticIntensity.LIGHT) }
    }

    HapticPatternType.STRONG_FEEDBACK -> {
      { Haptic(80.milliseconds, HapticIntensity.STRONG) }
    }

    HapticPatternType.HEARTBEAT -> {
      {
        Repeat(2) {
          Sequence {
            Haptic(60.milliseconds, HapticIntensity.STRONG)
            Delay(80.milliseconds)
            Haptic(40.milliseconds, HapticIntensity.MEDIUM)
            Delay(400.milliseconds)
          }
        }
      }
    }

    HapticPatternType.NOTIFICATION -> {
      {
        Haptic(50.milliseconds, HapticIntensity.MEDIUM)
        Delay(100.milliseconds)
        Haptic(100.milliseconds, HapticIntensity.STRONG)
      }
    }

    HapticPatternType.SUCCESS -> {
      {
        Haptic(30.milliseconds, HapticIntensity.LIGHT)
        Delay(50.milliseconds)
        Haptic(60.milliseconds, HapticIntensity.MEDIUM)
        Delay(50.milliseconds)
        Haptic(100.milliseconds, HapticIntensity.STRONG)
      }
    }

    HapticPatternType.ERROR -> {
      {
        Repeat(3) {
          Sequence {
            Haptic(100.milliseconds, HapticIntensity.HIGH)
            Delay(100.milliseconds)
          }
        }
      }
    }

    HapticPatternType.RAMP_UP -> {
      {
        Haptic(50.milliseconds, HapticIntensity.LIGHT)
        Delay(30.milliseconds)
        Haptic(50.milliseconds, HapticIntensity.MEDIUM)
        Delay(30.milliseconds)
        Haptic(50.milliseconds, HapticIntensity.STRONG)
        Delay(30.milliseconds)
        Haptic(50.milliseconds, HapticIntensity.HIGH)
      }
    }
  }

  Jindong(trigger, content = pattern)
}

enum class HapticPatternType(
  val displayName: String,
  val description: String,
) {
  SINGLE_TAP("Single Tap", "Simple single vibration"),
  DOUBLE_TAP("Double Tap", "Two quick taps"),
  TRIPLE_TAP("Triple Tap", "Three quick taps"),
  LIGHT_FEEDBACK("Light Feedback", "Gentle vibration"),
  STRONG_FEEDBACK("Strong Feedback", "Strong vibration"),
  HEARTBEAT("Heartbeat", "Heart beating pattern"),
  NOTIFICATION("Notification", "Alert notification pattern"),
  SUCCESS("Success", "Success confirmation pattern"),
  ERROR("Error", "Error alert pattern"),
  RAMP_UP("Ramp Up", "Gradually increasing intensity"),
}
