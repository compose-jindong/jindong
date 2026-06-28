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

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.compose.jindong.sample.theme.JindongTheme

/**
 * Uppercase mono section label (e.g. `DEVICE CAPABILITY`, `MODULES`). Renders [text] uppercased in
 * the [JindongTypography.sectionLabel] role with [JindongColors.text3].
 */
@Composable
fun MonoLabel(
  text: String,
  modifier: Modifier = Modifier,
) {
  Text(
    text = text.uppercase(),
    style = JindongTheme.typography.sectionLabel,
    color = JindongTheme.colors.text3,
    modifier = modifier,
  )
}
