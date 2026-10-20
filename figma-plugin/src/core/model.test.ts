import { describe, expect, test } from "vitest";
import {
  normalizePattern,
  parseLinkJson,
  parsePatternJson,
  toTimeline,
  validatePattern,
  type PatternSpec,
} from "./model";
import { buzz, pattern, tap } from "./testing";

const SUCCESS: PatternSpec = {
  schema: 1,
  name: "Success",
  description: "LIGHT→MEDIUM→STRONG",
  tone: "green",
  events: [buzz(0, 50, 0.3), buzz(100, 50, 0.55), buzz(100, 60, 0.8)],
  trailingMs: 100,
};

describe("toTimeline", () => {
  test("places events after their pauses", () => {
    const t = toTimeline(SUCCESS);
    expect(t.events.map((e) => e.startMs)).toEqual([0, 150, 300]);
    expect(t.events.map((e) => e.durationMs)).toEqual([50, 50, 60]);
    expect(t.durationMs).toBe(460);
  });

  test("taps take no time", () => {
    expect(toTimeline(pattern([tap(0), buzz(0, 80)], 0)).events.map((e) => e.startMs)).toEqual([0, 0]);
    const t = toTimeline(pattern([tap(0), tap(80), tap(80)], 80));
    expect(t.events.map((e) => e.startMs)).toEqual([0, 80, 160]);
    expect(t.durationMs).toBe(240);
  });
});

describe("validatePattern", () => {
  test("rejects bad data with readable errors", () => {
    expect(validatePattern({ ...SUCCESS, events: [] })).toEqual({ ok: false, error: "Add at least one event." });
    expect(validatePattern({ ...SUCCESS, events: Array(51).fill(tap(10)) })).toEqual({
      ok: false,
      error: "A haptic can have at most 50 events.",
    });
    expect(validatePattern({ ...SUCCESS, name: "   " })).toEqual({ ok: false, error: "Give the haptic a name." });
    expect(validatePattern({ ...SUCCESS, schema: 2 })).toEqual({
      ok: false,
      error: "This card was made with a newer version of the Jindong plugin.",
    });
  });

  test.each([
    ["a zero-length buzz", buzz(0, 0)],
    ["a negative pause", buzz(-1, 50)],
    ["a fractional pause", buzz(1.5, 50)],
    ["strength above 1", { ...tap(0), intensity: 1.2 }],
    ["a NaN sharpness", { ...tap(0), sharpness: NaN }],
    ["three decimals", { ...tap(0), intensity: 0.555 }],
    ["an unknown event type", { type: "wobble", gapBeforeMs: 0 }],
  ])("rejects %s", (_, bad) => {
    expect(validatePattern({ ...SUCCESS, events: [bad] })).toEqual({ ok: false, error: "Invalid haptic data." });
  });

  test("accepts a well-formed pattern", () => {
    expect(validatePattern(SUCCESS)).toEqual({ ok: true, pattern: SUCCESS });
  });
});

describe("normalizePattern", () => {
  test("clamps editor input into valid data", () => {
    const n = normalizePattern(
      pattern(
        [
          { type: "continuous", gapBeforeMs: -5, durationMs: 0, intensity: 1.234, sharpness: NaN },
          { ...tap(99_999), intensity: 0.555 },
        ],
        12.6,
      ),
    );
    expect(n.events[0]).toEqual({ type: "continuous", gapBeforeMs: 0, durationMs: 1, intensity: 1, sharpness: 0.5 });
    expect(n.events[1]).toMatchObject({ gapBeforeMs: 10_000, intensity: 0.56 });
    expect(n.trailingMs).toBe(13);
    expect(normalizePattern({ ...SUCCESS, name: "  " + "x".repeat(50) }).name).toBe("x".repeat(40));
    expect(validatePattern(n).ok).toBe(true);
  });

  test("drops a stray length from taps", () => {
    const n = normalizePattern(pattern([{ ...tap(0), durationMs: 30 } as never], 0));
    expect(n.events[0]).toEqual(tap(0));
  });
});

describe("parsing", () => {
  test("tolerates empty and broken JSON", () => {
    expect(parsePatternJson("")).toBeNull();
    expect(parsePatternJson("{bad")).toBeNull();
    expect(parsePatternJson(JSON.stringify(SUCCESS))).toEqual(SUCCESS);
  });

  test("keeps the card name a link remembers", () => {
    expect(parseLinkJson(JSON.stringify({ schema: 1, cardId: "1:2", trigger: "tap", cardName: "Success" }))).toEqual({
      schema: 1,
      cardId: "1:2",
      trigger: "tap",
      cardName: "Success",
    });
  });

  test("reads only known link triggers", () => {
    expect(parseLinkJson(JSON.stringify({ schema: 1, cardId: "1:2", trigger: "hover" }))).toBeNull();
    expect(parseLinkJson(JSON.stringify({ schema: 1, cardId: "1:2", trigger: "tap" }))).toEqual({
      schema: 1,
      cardId: "1:2",
      trigger: "tap",
    });
  });
});
