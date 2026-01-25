# Android Host Tests

Robolectric-based Android tests that run on JVM without requiring an emulator.

## Running Tests

```bash
# Run Android host tests only
./gradlew :jindong-core:testAndroidHostTest

# Run all tests (including commonTest, iosTest, etc.)
./gradlew :jindong-core:allTests
```

> **Note**: Use `--rerun-tasks` when you need to re-run tests that Gradle considers up-to-date (e.g., after changing test configuration or debugging flaky tests).

## Test Scenarios

To be updated...

| Category | Test | Jindong Mapping |
|----------|------|-----------------|
| **OneShot** | High intensity (255) | `HapticIntensity.HIGH` |
| | Medium intensity (128) | `HapticIntensity.MEDIUM` |
| | Low intensity (64) | `HapticIntensity.LIGHT` |
| **Waveform** | Simple sequence | `Sequence { Haptic, Delay, Haptic }` |
| | Complex sequence | `Repeat { Haptic, Delay }` |
| **Edge Cases** | Minimum duration (1ms) | - |
| | Minimum amplitude (1) | - |
| **Control** | Cancel | `executor.cancel()` |


## Configuration

- **SDK**: API 26 (Android 8.0)
- **Runner**: `RobolectricTestRunner`
