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
package io.github.compose.jindong.core.model

/**
 * An immutable compiled timeline. [durationMs] includes leading and trailing silence.
 *
 * Events-only construction defaults to the latest event end. Builders pass the complete timeline
 * duration. Equality includes both events and duration, so silence changes invalidate Compose keys.
 */
public class HapticPattern(
  events: List<ScheduledHapticEvent>,
  public val durationMs: Long = events.eventSpanMs(),
) {
  init {
    checkedEventCount(events.size.toLong(), "pattern")
  }

  public val events: List<ScheduledHapticEvent> = object : AbstractList<ScheduledHapticEvent>() {
    private val snapshot = events.toList()
    override val size: Int get() = snapshot.size
    override fun get(index: Int): ScheduledHapticEvent = snapshot[index]
  }

  init {
    require(durationMs >= this.events.eventSpanMs()) {
      "durationMs must be non-negative and cover every event end, was $durationMs"
    }
  }

  public fun copy(
    events: List<ScheduledHapticEvent> = this.events,
    durationMs: Long = this.durationMs,
  ): HapticPattern = HapticPattern(events, durationMs)

  public operator fun component1(): List<ScheduledHapticEvent> = events

  public operator fun component2(): Long = durationMs

  override fun equals(other: Any?): Boolean = other is HapticPattern && events == other.events && durationMs == other.durationMs

  override fun hashCode(): Int = 31 * events.hashCode() + durationMs.hashCode()

  override fun toString(): String = "HapticPattern(events=$events, durationMs=$durationMs)"

  public companion object {
    public val Empty: HapticPattern = HapticPattern(emptyList())
  }
}
