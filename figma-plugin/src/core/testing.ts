import type { BuzzEvent, HapticEvent, PatternSpec, TapEvent } from "./model";

// Test helpers for building events and patterns. Production code must not import this file.

export function tap(gapBeforeMs: number, intensity = 0.5, sharpness = 0.5): TapEvent {
  return { type: "transient", gapBeforeMs, intensity, sharpness };
}

export function buzz(gapBeforeMs: number, durationMs: number, intensity = 0.5, sharpness = 0.5): BuzzEvent {
  return { type: "continuous", gapBeforeMs, durationMs, intensity, sharpness };
}

export function pattern(events: HapticEvent[], trailingMs: number): PatternSpec {
  return { schema: 1, name: "Test", description: "", tone: "neutral", events, trailingMs };
}
