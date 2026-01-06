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
import io.github.compose.jindong.dsl.Delay
import io.github.compose.jindong.dsl.Haptic
import io.github.compose.jindong.dsl.Repeat
import io.github.compose.jindong.dsl.Sequence
import io.github.compose.jindong.model.HapticIntensity
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SampleApp() {
  MaterialTheme {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = MaterialTheme.colorScheme.background
    ) {
      JindongProvider {
        HapticPatternListScreen()
      }
    }
  }
}

@Composable
fun HapticPatternListScreen() {
  val patterns = remember { HapticPatternType.entries }

  Column(
    modifier = Modifier.fillMaxSize()
  ) {
    Text(
      text = "Jindong Haptic Patterns",
      style = MaterialTheme.typography.headlineMedium,
      modifier = Modifier.padding(16.dp)
    )

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(patterns) { pattern ->
        HapticPatternCard(pattern)
      }
    }
  }
}

@Composable
fun HapticPatternCard(patternType: HapticPatternType) {
  var triggerCount by remember { mutableIntStateOf(0) }

  Card(
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = patternType.displayName,
          style = MaterialTheme.typography.titleMedium
        )
        Text(
          text = patternType.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Button(onClick = { triggerCount++ }) {
        Text("Play")
      }
    }
  }

  HapticPatternEffect(patternType, triggerCount)
}

@Composable
fun HapticPatternEffect(patternType: HapticPatternType, trigger: Int) {
  if (trigger == 0) return

  when (patternType) {
    HapticPatternType.SINGLE_TAP -> {
      Jindong(trigger) {
        Haptic(50.milliseconds)
      }
    }
    HapticPatternType.DOUBLE_TAP -> {
      Jindong(trigger) {
        Haptic(30.milliseconds)
        Delay(50.milliseconds)
        Haptic(30.milliseconds)
      }
    }
    HapticPatternType.TRIPLE_TAP -> {
      Jindong(trigger) {
        Repeat(3) {
          Sequence {
            Haptic(25.milliseconds)
            Delay(40.milliseconds)
          }
        }
      }
    }
    HapticPatternType.LIGHT_FEEDBACK -> {
      Jindong(trigger) {
        Haptic(40.milliseconds, HapticIntensity.LIGHT)
      }
    }
    HapticPatternType.STRONG_FEEDBACK -> {
      Jindong(trigger) {
        Haptic(80.milliseconds, HapticIntensity.STRONG)
      }
    }
    HapticPatternType.HEARTBEAT -> {
      Jindong(trigger) {
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
      Jindong(trigger) {
        Haptic(50.milliseconds, HapticIntensity.MEDIUM)
        Delay(100.milliseconds)
        Haptic(100.milliseconds, HapticIntensity.STRONG)
      }
    }
    HapticPatternType.SUCCESS -> {
      Jindong(trigger) {
        Haptic(30.milliseconds, HapticIntensity.LIGHT)
        Delay(50.milliseconds)
        Haptic(60.milliseconds, HapticIntensity.MEDIUM)
        Delay(50.milliseconds)
        Haptic(100.milliseconds, HapticIntensity.STRONG)
      }
    }
    HapticPatternType.ERROR -> {
      Jindong(trigger) {
        Repeat(3) {
          Sequence {
            Haptic(100.milliseconds, HapticIntensity.HIGH)
            Delay(100.milliseconds)
          }
        }
      }
    }
    HapticPatternType.RAMP_UP -> {
      Jindong(trigger) {
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
