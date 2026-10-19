import type { BuzzEvent, HapticEvent, PatternSpec, TapEvent, Tone } from "./model";

// The sample app's presets (sample/shared/.../screens/HarnessLogic.kt and RichFeedbackPatterns.kt),
// rewritten as pauses so "Heartbeat" feels the same in the plugin and in the sample app.
// Presets with curves (Celebration, the rich Error) wait for curve support.

function buzz(gapBeforeMs: number, durationMs: number, intensity: number): BuzzEvent {
  return { type: "continuous", gapBeforeMs, durationMs, intensity, sharpness: 0.5 };
}

function tap(gapBeforeMs: number, intensity: number, sharpness: number): TapEvent {
  return { type: "transient", gapBeforeMs, intensity, sharpness };
}

function preset(name: string, description: string, tone: Tone, events: HapticEvent[], trailingMs: number): PatternSpec {
  return { schema: 1, name, description, tone, events, trailingMs };
}

export const PRESETS: readonly PatternSpec[] = [
  preset("Single Tap", "50ms single", "neutral", [buzz(0, 50, 0.6)], 150),
  preset("Double Tap", "tap-pause-tap", "neutral", [buzz(0, 40, 0.6), buzz(90, 40, 0.6)], 130),
  preset("Triple Tap", "(tap+pause) × 3", "neutral", [buzz(0, 40, 0.6), buzz(90, 40, 0.6), buzz(90, 40, 0.6)], 120),
  preset("Light Feedback", "40ms LIGHT", "green", [buzz(0, 40, 0.25)], 120),
  preset("Strong Feedback", "80ms STRONG", "blue", [buzz(0, 80, 0.75)], 120),
  preset(
    "Heartbeat",
    "strong-soft × 2, long rest",
    "red",
    [buzz(0, 60, 0.9), buzz(50, 50, 0.4), buzz(320, 60, 0.9), buzz(50, 50, 0.4)],
    260,
  ),
  preset("Notification", "soft → strong", "orange", [buzz(0, 50, 0.4), buzz(110, 60, 0.8)], 120),
  preset("Success", "LIGHT→MEDIUM→STRONG", "green", [buzz(0, 50, 0.3), buzz(100, 50, 0.55), buzz(100, 60, 0.8)], 100),
  preset("Error", "HIGH × 3, urgent", "red", [buzz(0, 60, 1), buzz(60, 60, 1), buzz(60, 60, 1)], 60),
  preset(
    "Ramp Up",
    "LIGHT→MEDIUM→STRONG→HIGH",
    "blue",
    [buzz(0, 50, 0.25), buzz(90, 50, 0.5), buzz(90, 50, 0.75), buzz(90, 60, 1)],
    80,
  ),
  preset("Correct", "Three crisp taps", "green", [tap(0, 0.75, 0.9), tap(80, 0.75, 0.9), tap(80, 0.75, 0.9)], 80),
];

export function presetNamed(name: string): PatternSpec {
  const found = PRESETS.find((p) => p.name === name);
  if (!found) throw new Error(`No preset named ${name}`);
  return found;
}
