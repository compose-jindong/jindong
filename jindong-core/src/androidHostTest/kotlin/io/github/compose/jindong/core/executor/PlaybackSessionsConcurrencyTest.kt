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

import io.kotest.matchers.shouldBe
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class PlaybackSessionsConcurrencyTest {
  @Test
  fun `concurrent start cannot overtake the previous native start or stop`() {
    val sessions = PlaybackSessions()
    val events = Collections.synchronizedList(mutableListOf<String>())
    val started = CountDownLatch(1)
    val allowFirst = CountDownLatch(1)
    val secondRequested = CountDownLatch(1)
    lateinit var old: HapticHandle
    lateinit var next: HapticHandle
    val firstThread = thread {
      old = sessions.start {
        events += "start old"
        started.countDown()
        check(allowFirst.await(5, TimeUnit.SECONDS))
        NativePlayback(10_000) { events += "stop old" }
      }
    }
    check(started.await(5, TimeUnit.SECONDS))
    val secondThread = thread {
      secondRequested.countDown()
      next = sessions.start {
        events += "start new"
        NativePlayback(10_000) { events += "stop new" }
      }
    }
    check(secondRequested.await(5, TimeUnit.SECONDS))
    allowFirst.countDown()
    firstThread.join(5_000)
    secondThread.join(5_000)
    firstThread.isAlive shouldBe false
    secondThread.isAlive shouldBe false
    old.cancel()
    events.toList() shouldBe listOf("start old", "stop old", "start new")
    next.isActive shouldBe true
    sessions.release { }
    events.toList() shouldBe listOf("start old", "stop old", "start new", "stop new")
  }
}
