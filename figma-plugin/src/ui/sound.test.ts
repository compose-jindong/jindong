import { expect, test } from "vitest";
import { toTimeline } from "../core/model";
import { buzz, pattern, tap } from "../core/testing";
import { playSound } from "./sound";
import { FakeAudioContext } from "./testing";

function play(events: Parameters<typeof pattern>[0]) {
  const context = new FakeAudioContext();
  const stop = playSound(toTimeline(pattern(events, 0)), context as unknown as AudioContext);
  return { context, stop };
}

test("a tap is a short knock at its start, and a buzz hums for its length", () => {
  const { context } = play([tap(0), buzz(100, 300)]);
  const [knock, hum] = context.oscillators;
  const start = knock.startTime!;
  expect(start).toBeGreaterThanOrEqual(context.currentTime);
  expect(knock.stopTime! - start).toBeLessThan(0.1);
  expect(hum.startTime! - start).toBeCloseTo(0.1, 3);
  expect(hum.stopTime! - hum.startTime!).toBeCloseTo(0.3, 3);
});

test("strength sets the volume and sharpness the pitch", () => {
  const taps = play([tap(0, 0.2, 0.1), tap(100, 0.9, 0.9)]).context;
  const [softTap, hardTap] = taps.oscillators;
  expect(taps.envelopeOf(hardTap).gain.peak).toBeGreaterThan(taps.envelopeOf(softTap).gain.peak);
  expect(hardTap.frequency.peak).toBeGreaterThan(softTap.frequency.peak);

  const buzzes = play([buzz(0, 200, 0.2, 0.1), buzz(100, 200, 0.9, 0.9)]).context;
  const [softBuzz, hardBuzz] = buzzes.oscillators;
  expect(buzzes.envelopeOf(hardBuzz).gain.peak).toBeGreaterThan(buzzes.envelopeOf(softBuzz).gain.peak);
  expect(hardBuzz.frequency.peak).toBeGreaterThan(softBuzz.frequency.peak);
});

test("stopping silences the preview at once", () => {
  const { context, stop } = play([buzz(0, 1000)]);
  expect(context.audible(context.oscillators[0])).toBe(true);
  stop();
  expect(context.audible(context.oscillators[0])).toBe(false);
});

test("without Web Audio the preview stays silent", () => {
  expect(() => playSound(toTimeline(pattern([tap(0)], 0)), null)()).not.toThrow();
});
