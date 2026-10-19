import { expect, test } from "vitest";
import { normalizePattern, toTimeline, validatePattern } from "./model";
import { PRESETS, presetNamed } from "./presets";

// Absolute values copied from sample/shared/.../screens/HarnessLogic.kt: [start, duration, intensity].
const SAMPLE: Record<string, { span: number; events: [number, number, number][] }> = {
  "Single Tap": { span: 200, events: [[0, 50, 0.6]] },
  "Double Tap": { span: 300, events: [[0, 40, 0.6], [130, 40, 0.6]] },
  "Triple Tap": { span: 420, events: [[0, 40, 0.6], [130, 40, 0.6], [260, 40, 0.6]] },
  "Light Feedback": { span: 160, events: [[0, 40, 0.25]] },
  "Strong Feedback": { span: 200, events: [[0, 80, 0.75]] },
  Heartbeat: { span: 900, events: [[0, 60, 0.9], [110, 50, 0.4], [480, 60, 0.9], [590, 50, 0.4]] },
  Notification: { span: 340, events: [[0, 50, 0.4], [160, 60, 0.8]] },
  Success: { span: 460, events: [[0, 50, 0.3], [150, 50, 0.55], [300, 60, 0.8]] },
  Error: { span: 360, events: [[0, 60, 1], [120, 60, 1], [240, 60, 1]] },
  "Ramp Up": { span: 560, events: [[0, 50, 0.25], [140, 50, 0.5], [280, 50, 0.75], [420, 60, 1]] },
};

test("presets keep the sample app order", () => {
  expect(PRESETS.map((p) => p.name)).toEqual([
    "Single Tap",
    "Double Tap",
    "Triple Tap",
    "Light Feedback",
    "Strong Feedback",
    "Heartbeat",
    "Notification",
    "Success",
    "Error",
    "Ramp Up",
    "Correct",
  ]);
});

test.each(Object.entries(SAMPLE))("%s plays like the sample app", (name, sample) => {
  const t = toTimeline(presetNamed(name));
  expect(t.durationMs).toBe(sample.span);
  expect(t.events.map((e) => [e.startMs, e.durationMs, e.intensity])).toEqual(sample.events);
  expect(t.events.every((e) => e.type === "continuous" && e.sharpness === 0.5)).toBe(true);
});

test("Correct is three sharp taps 80ms apart", () => {
  const t = toTimeline(presetNamed("Correct"));
  expect(t.events.map((e) => [e.type, e.startMs, e.intensity, e.sharpness])).toEqual([
    ["transient", 0, 0.75, 0.9],
    ["transient", 80, 0.75, 0.9],
    ["transient", 160, 0.75, 0.9],
  ]);
  expect(t.durationMs).toBe(240);
});

test("every preset is valid and already normalized", () => {
  for (const p of PRESETS) {
    expect(validatePattern(p).ok).toBe(true);
    expect(normalizePattern(p)).toEqual(p);
  }
});

test("presetNamed rejects unknown names", () => {
  expect(() => presetNamed("Nope")).toThrow();
});
