# Android Host Tests

Robolectric-based Android tests that run on JVM without requiring an emulator.

Use Java 21 or later for the SDK 36 capability tests, as required by [Robolectric](https://robolectric.org/compatibility_table/).
The library still targets JVM 17.
An explicit `org.gradle.java.home` setting overrides `JAVA_HOME`.

## Running Tests

```bash
# Run Android host tests only
./gradlew :jindong-core:testAndroidHostTest

# Run all tests (including commonTest, iosTest, etc.)
./gradlew :jindong-core:allTests
```

> **Note**: Use `--rerun-tasks` when you need to re-run tests that Gradle considers up-to-date (e.g., after changing test configuration or debugging flaky tests).

## Test Scenarios

- API 26 waveform conversion, intensity zero, compatibility tails, and cancellation.
- SDK 36 envelope builders with reported hardware limits and waveform fallback.
- Primitive composition with queried support and native duration.
- Zero-duration transient completion and native start failure cleanup.
- Common pure planning tests for mixed events, curves, primitive spacing, and diagnostics.


## Configuration

- **SDK**: API 26 for existing waveform tests; API 36 for capability playback tests.
- **Runner**: `RobolectricTestRunner`

Robolectric validates effect construction and adapter behavior. These tests do not measure physical haptic quality.
