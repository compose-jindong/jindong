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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PlaybackSessionsTest :
  FunSpec({
    test("replacement stops the old native playback before starting the new one") {
      val events = mutableListOf<String>()
      val sessions = PlaybackSessions()
      val old = sessions.start { NativePlayback(1000) { events += "stop old" } }
      val next = sessions.start {
        events += "start new"
        NativePlayback(1000) { events += "stop new" }
      }
      old.cancel()
      events shouldBe listOf("stop old", "start new")
      next.isActive shouldBe true
      next.cancel()
      next.cancel()
      events shouldBe listOf("stop old", "start new", "stop new")
    }

    test("silent sessions stay active until logical expiry without native output") {
      val clock = TestTimeSource()
      val sessions = PlaybackSessions(clock)
      val silent = sessions.start { NativePlayback(100) }
      silent.isActive shouldBe true
      clock += 100.milliseconds
      silent.isActive shouldBe false
      sessions.start { NativePlayback(0) }.isActive shouldBe false
    }

    test("suspend completion waits exactly for playback and stops once") {
      runTest {
        var stops = 0
        val session = PlaybackSessions().start { NativePlayback(103) { stops++ } }
        session.awaitCompletion()
        testScheduler.currentTime shouldBe 103L
        stops shouldBe 1
        session.isActive shouldBe false
      }
    }

    test("coroutine cancellation stops native playback and propagates cancellation") {
      runTest {
        var stops = 0
        val session = PlaybackSessions().start { NativePlayback(1000) { stops++ } }
        val job = launch(start = CoroutineStart.UNDISPATCHED) { session.awaitCompletion() }
        job.cancel()
        job.join()
        job.isCancelled shouldBe true
        stops shouldBe 1
        session.isActive shouldBe false
      }
    }

    test("old suspended cleanup cannot stop the replacement") {
      runTest {
        val stops = mutableListOf<Int>()
        val sessions = PlaybackSessions()
        val old = sessions.start { NativePlayback(1000) { stops += 1 } }
        val job = launch(start = CoroutineStart.UNDISPATCHED) { old.awaitCompletion() }
        val next = sessions.start { NativePlayback(1000) { stops += 2 } }
        job.join()
        stops shouldBe listOf(1)
        next.isActive shouldBe true
        next.cancel()
      }
    }

    test("start failure leaves no current playback and permits a later start") {
      val sessions = PlaybackSessions()
      var stops = 0
      val old = sessions.start { NativePlayback(1000) { stops++ } }
      shouldThrow<IllegalStateException> { sessions.start { error("native start failed") } }
      old.isActive shouldBe false
      stops shouldBe 1
      sessions.start { null }.isActive shouldBe false
      sessions.start { NativePlayback(1000) }.isActive shouldBe true
      sessions.release { }
    }

    test("engine invalidation cancels the session without replaying it") {
      val sessions = PlaybackSessions()
      var stops = 0
      val old = sessions.start { NativePlayback(1000) { stops++ } }
      sessions.cancel()
      old.isActive shouldBe false
      stops shouldBe 1
      sessions.start { NativePlayback(1000) }.isActive shouldBe true
      sessions.release { }
    }

    test("release is idempotent and rejects later playback") {
      val sessions = PlaybackSessions()
      var stops = 0
      var releases = 0
      val active = sessions.start { NativePlayback(1000) { stops++ } }
      sessions.release { releases++ }
      sessions.release { releases++ }
      active.cancel()
      active.isActive shouldBe false
      stops shouldBe 1
      releases shouldBe 1
      shouldThrow<IllegalStateException> { sessions.start { NativePlayback(1000) } }
    }

    test("a stop failure does not replace coroutine cancellation") {
      runTest {
        val session = PlaybackSessions().start { NativePlayback(1000) { error("stop failed") } }
        var observed: Throwable? = null
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
          try {
            session.awaitCompletion()
          } catch (error: Throwable) {
            observed = error
            throw error
          }
        }
        job.cancel()
        job.join()
        (observed is CancellationException) shouldBe true
        observed!!.suppressedExceptions.single().message shouldBe "stop failed"
        session.isActive shouldBe false
      }
    }
    test("native completion waits for every player after the logical deadline") {
      runTest {
        val clock = TestTimeSource()
        val completion = NativePlaybackCompletion(2)
        var stops = 0
        val session = PlaybackSessions(clock).start { NativePlayback(100, completion) { stops++ } }
        val job = launch { session.awaitCompletion() }
        runCurrent()
        advanceTimeBy(100)
        clock += 100.milliseconds
        runCurrent()
        session.isActive shouldBe true
        job.isCompleted shouldBe false
        completion.completePlayer(0)
        completion.completePlayer(0)
        runCurrent()
        session.isActive shouldBe true
        completion.completePlayer(1)
        job.join()
        session.isActive shouldBe false
        stops shouldBe 1
        testScheduler.currentTime shouldBe 100L
      }
    }

    test("a transient at time zero waits for its native callback") {
      runTest {
        val completion = NativePlaybackCompletion(1)
        val session = PlaybackSessions().start { NativePlayback(0, completion) }
        val job = launch { session.awaitCompletion() }
        runCurrent()
        session.isActive shouldBe true
        job.isCompleted shouldBe false
        completion.completePlayer(0)
        job.join()
        session.isActive shouldBe false
      }
    }

    test("early native completion preserves trailing logical silence") {
      runTest {
        val completion = NativePlaybackCompletion(1)
        val session = PlaybackSessions().start { NativePlayback(150, completion) }
        val job = launch { session.awaitCompletion() }
        runCurrent()
        advanceTimeBy(50)
        completion.completePlayer(0)
        runCurrent()
        job.isCompleted shouldBe false
        job.join()
        testScheduler.currentTime shouldBe 150L
      }
    }

    test("synchronous start completion is retained and an old callback cannot finish its replacement") {
      runTest {
        val sessions = PlaybackSessions()
        val oldCompletion = NativePlaybackCompletion(1)
        val old = sessions.start { NativePlayback(0, oldCompletion) }
        val oldJob = launch { old.awaitCompletion() }
        runCurrent()
        val nextCompletion = NativePlaybackCompletion(1)
        val next = sessions.start { NativePlayback(0, nextCompletion) }
        oldCompletion.completePlayer(0)
        oldJob.join()
        next.isActive shouldBe true
        nextCompletion.completePlayer(0)
        next.awaitCompletion()
        sessions.start {
          val synchronous = NativePlaybackCompletion(1)
          synchronous.completePlayer(0)
          NativePlayback(0, synchronous)
        }.awaitCompletion()
      }
    }

    test("native failure propagates and cancellation can wake an unfinished native wait") {
      runTest {
        val completion = NativePlaybackCompletion(2)
        var stops = 0
        val session = PlaybackSessions().start { NativePlayback(0, completion) { stops++ } }
        completion.completePlayer(0, IllegalStateException("native failed"))
        shouldThrow<IllegalStateException> { session.awaitCompletion() }.message shouldBe "native failed"
        stops shouldBe 1
        session.failure?.message shouldBe "native failed"
        val next = PlaybackSessions().start { NativePlayback(0, NativePlaybackCompletion(1)) { stops++ } }
        val job = launch { next.awaitCompletion() }
        runCurrent()
        job.cancel()
        job.join()
        stops shouldBe 2
      }
    }
    test("native completion cleanup failures are retained for polling callers") {
      runTest {
        val completion = NativePlaybackCompletion(1)
        val session = PlaybackSessions().start { NativePlayback(0, completion) }
        val failure = IllegalStateException("completion cleanup failed")
        completion.completePlayer(0) { throw failure }
        session.isActive shouldBe false
        session.failure shouldBe failure
        shouldThrow<IllegalStateException> { session.awaitCompletion() } shouldBe failure
        session.failure shouldBe failure
      }
    }
  })
