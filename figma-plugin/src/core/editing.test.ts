import { expect, test } from "vitest";
import { NEW_BUZZ, insertEventAfter, removeEvent, setEventType, setTrailing, updateEvent } from "./editing";
import { toTimeline } from "./model";
import { presetNamed } from "./presets";

test("adding an event puts a default buzz right after the selected one", () => {
  const p = insertEventAfter(presetNamed("Success"), 0);
  expect(p.events).toHaveLength(4);
  expect(p.events[1]).toEqual(NEW_BUZZ);
  expect(p.events[2]).toEqual(presetNamed("Success").events[1]);
});

test("a pattern keeps between 1 and 50 events", () => {
  const full = { ...presetNamed("Success"), events: Array(50).fill(NEW_BUZZ) };
  expect(insertEventAfter(full, 0)).toEqual(full);
  const one = { ...presetNamed("Success"), events: [NEW_BUZZ] };
  expect(removeEvent(one, 0)).toEqual(one);
  expect(removeEvent(presetNamed("Success"), 1).events).toEqual([
    presetNamed("Success").events[0],
    presetNamed("Success").events[2],
  ]);
});

test("switching type keeps the data valid", () => {
  const p = setEventType(presetNamed("Success"), 0, "transient");
  expect(p.events[0]).toEqual({ type: "transient", gapBeforeMs: 0, intensity: 0.3, sharpness: 0.5 });
  expect(toTimeline(p).events[1].startMs).toBe(100);
  expect(setEventType(p, 0, "continuous").events[0]).toEqual({
    type: "continuous",
    gapBeforeMs: 0,
    durationMs: 50,
    intensity: 0.3,
    sharpness: 0.5,
  });
});

test("edits are clamped into valid data", () => {
  const p = updateEvent(presetNamed("Success"), 0, { gapBeforeMs: -10, intensity: 0.333 });
  expect(p.events[0]).toMatchObject({ gapBeforeMs: 0, intensity: 0.33 });
});

test("editing an event leaves the name being typed alone", () => {
  expect(updateEvent({ ...presetNamed("Success"), name: "Pay " }, 0, { gapBeforeMs: 10 }).name).toBe("Pay ");
});

test("silence after is clamped without touching the name", () => {
  const p = setTrailing({ ...presetNamed("Success"), name: " Pay" }, -5);
  expect(p.trailingMs).toBe(0);
  expect(p.name).toBe(" Pay");
  expect(setTrailing(presetNamed("Success"), 12.4).trailingMs).toBe(12);
});

test("edits leave the preset itself untouched", () => {
  updateEvent(presetNamed("Success"), 0, { durationMs: 999 });
  expect(presetNamed("Success").events[0]).toEqual({
    type: "continuous",
    gapBeforeMs: 0,
    durationMs: 50,
    intensity: 0.3,
    sharpness: 0.5,
  });
});
