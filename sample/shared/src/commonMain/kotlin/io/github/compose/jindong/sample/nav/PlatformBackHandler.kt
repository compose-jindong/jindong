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
package io.github.compose.jindong.sample.nav

import androidx.compose.runtime.Composable

/**
 * Intercepts the platform back action while [enabled]. On Android this is the system back
 * gesture/button (via `androidx.activity.compose.BackHandler`); on iOS there is no system back, so
 * the actual is a no-op and the app-bar chevron remains the affordance.
 */
@Composable
expect fun PlatformBackHandler(
  enabled: Boolean,
  onBack: () -> Unit,
)
