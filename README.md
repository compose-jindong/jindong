<p align="center">
  <img src="public/jindong-banner.png" alt="Jindong Banner" />
</p>

<p align="center">
  <strong>Declarative Haptic Feedback Library for Compose Multiplatform</strong>
</p>

<p align="center">
  <a href="https://github.com/user/jindong/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License" /></a>
</p>

---

## About

**Jindong (진동)** is the Korean word for "vibration". We chose this name to reflect the library's core purpose, as it's a familiar term for the Korean creators of this library.

Jindong provides a declarative DSL for haptic feedback in Compose Multiplatform applications(supports Android and iOS). Built on Compose Runtime, it lets you define complex haptic patterns with a simple, intuitive API.

```kotlin
Jindong(trigger) {
    Haptic(100.ms)
    Delay(50.ms)
    Haptic(50.ms, HapticIntensity.STRONG)
}
```

## Features

- **Declarative API** - Define haptic patterns like you define UI with Compose
- **Cross-platform based on Compose Multiplatform** - Unified APIs for Android and iOS
- **Compose Integration** - Works naturally with Compose state and effects

## Installation

Add Jindong to your `build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.compose-jindong:jindong:<version>")
        }
    }
}
```

### Platform Requirements

| Platform | Minimum Version |
|----------|-----------------|
| Android  | API 24          |
| iOS      | iOS 13          |

### Android Setup

Add the vibration permission to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.VIBRATE" />
```

## Quick Start

```kotlin
@Composable
fun App() {
    var count by remember { mutableStateOf(0) }

    JindongProvider {
        Jindong(count) {
            Haptic(100.ms)
        }

        Button(onClick = { count++ }) {
            Text("Tap me!")
        }
    }
}
```

## Documentation

For full documentation, visit [compose-jindong.github.io/jindong](https://compose-jindong.github.io/jindong).

- [Why Jindong](https://compose-jindong.github.io/jindong/docs/guide/why-jindong) - Motivation and benefits
- [Getting Started](https://compose-jindong.github.io/jindong/docs/guide/getting-started) - Installation and setup
- [Quick Start](https://compose-jindong.github.io/jindong/docs/guide/quick-start) - Build your first haptic pattern
- [API Reference](https://compose-jindong.github.io/jindong/docs/api/core/jindong) - Complete API documentation

## License

```
Copyright 2026 compose-jindong

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
