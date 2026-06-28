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
package io.github.compose.jindong.core.executor

import android.content.Context
import android.os.Build
import android.os.Vibrator
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

/**
 * Time-based expiry behaviour of [AndroidHapticHandle], the bug this change fixes: before, `isActive`
 * was decided once at construction (vibrator != null) and never noticed natural completion, so it
 * stayed `true` until [AndroidHapticHandle.cancel]. A [TestTimeSource] drives expiry deterministically.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
class AndroidHapticHandleTest {

  private lateinit var vibrator: Vibrator

  @Before
  fun setup() {
    val context: Context = ApplicationProvider.getApplicationContext()
    vibrator = context.getSystemService(Vibrator::class.java)
  }

  @Test
  fun `isActive is true right after creation`() {
    val time = TestTimeSource()
    val handle = AndroidHapticHandle(vibrator, totalDurationMs = 100L, timeSource = time)

    handle.isActive shouldBe true
  }

  @Test
  fun `isActive stays true before the duration elapses`() {
    val time = TestTimeSource()
    val handle = AndroidHapticHandle(vibrator, totalDurationMs = 100L, timeSource = time)

    time += 99.milliseconds

    handle.isActive shouldBe true
  }

  // Regression guard: the false positive the previous handle could never detect.
  @Test
  fun `isActive becomes false once the duration elapses without cancel`() {
    val time = TestTimeSource()
    val handle = AndroidHapticHandle(vibrator, totalDurationMs = 100L, timeSource = time)

    time += 100.milliseconds

    handle.isActive shouldBe false
  }

  @Test
  fun `isActive is false after cancel regardless of time`() {
    val time = TestTimeSource()
    val handle = AndroidHapticHandle(vibrator, totalDurationMs = 100L, timeSource = time)

    handle.cancel()

    handle.isActive shouldBe false
  }

  @Test
  fun `a silent pattern handle is inactive from the start`() {
    val time = TestTimeSource()
    val handle = AndroidHapticHandle(vibrator = null, totalDurationMs = 0L, timeSource = time)

    handle.isActive shouldBe false
  }
}
