import { describe, expect, test } from "vitest";
import { CARD, displayWindowMs, layoutBars, type BarShape } from "./cardLayout";
import { toTimeline } from "./model";
import { presetNamed } from "./presets";
import { buzz, pattern, tap } from "./testing";

test("the visible window adds 10% and never drops below 200ms", () => {
  expect([460, 0, 100, 1000].map(displayWindowMs)).toEqual([506, 200, 200, 1100]);
});

describe("layoutBars", () => {
  test("Success draws three buzzes at their times and strengths", () => {
    const bars = layoutBars(toTimeline(presetNamed("Success")), CARD.timeline, 506);
    expect(bars.map((b) => b.kind)).toEqual(["continuous", "continuous", "continuous"]);
    expect(bars.map((b) => b.index)).toEqual([0, 1, 2]);
    expect(bars[0].x).toBe(14);
    expect(width(bars[0])).toBeCloseTo(24.9, 2);
    expect(bars[1].x).toBeCloseTo(88.7, 2);
    expect(bars[2].height).toBeCloseTo(60.8, 5);
    expect(bars[2].y).toBeCloseTo(79.2, 5);
  });

  test("taps sit at their start time", () => {
    const bars = layoutBars(toTimeline(presetNamed("Correct")), CARD.timeline, 264);
    expect(bars.map((b) => b.kind)).toEqual(["transient", "transient", "transient"]);
    expect(bars[1].x).toBeCloseTo(90.36, 2);
    expect(bars[1].height).toBeCloseTo(57, 5);
  });

  test("a zero-length single tap stays finite", () => {
    const t = toTimeline(pattern([tap(0)], 0));
    const [bar] = layoutBars(t, CARD.timeline, displayWindowMs(t.durationMs));
    expect(bar).toMatchObject({ kind: "transient", index: 0, x: 14 });
    for (const value of Object.values(bar)) {
      if (typeof value === "number") expect(Number.isFinite(value)).toBe(true);
    }
  });

  test("tiny and silent buzzes stay visible", () => {
    const [bar] = layoutBars(toTimeline(pattern([buzz(0, 1, 0)], 0)), CARD.timeline, 200);
    expect(width(bar)).toBe(2);
    expect(bar.height).toBe(1);
    expect(bar.y).toBe(139);
  });
});

function width(bar: BarShape): number {
  if (bar.kind !== "continuous") throw new Error("not a buzz");
  return bar.width;
}
