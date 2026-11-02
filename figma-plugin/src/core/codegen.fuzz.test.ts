import { expect, test } from "vitest";
import { patternCode } from "./codegen";
import { toTimeline, validatePattern, type HapticEvent, type PatternSpec } from "./model";

// Replays generated Kotlin the way Jindong's DSL does (delays advance time, a transient takes none) and
// checks it lands on the card's timeline for many random patterns. Gradle compiles the fixed golden cases;
// this covers the combinations those cases don't.

// mulberry32: 32-bit integer steps, so the sequence doesn't lose precision and cycle early the way a large
// floating-point multiply does.
function random(seed: number) {
  let state = seed >>> 0;
  return () => {
    state = (state + 0x6d2b79f5) >>> 0;
    let t = state;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4_294_967_296;
  };
}

function randomPattern(next: () => number): PatternSpec {
  const unit = () => [0, 0.25, 0.5, 0.75, 1, Math.round(next() * 100) / 100][Math.floor(next() * 6)];
  const ms = (max: number) => (next() < 0.3 ? 0 : Math.floor(next() * max));
  const events: HapticEvent[] = Array.from({ length: 1 + Math.floor(next() * 8) }, () =>
    next() < 0.4
      ? { type: "transient", gapBeforeMs: ms(400), intensity: unit(), sharpness: unit() }
      : { type: "continuous", gapBeforeMs: ms(400), durationMs: 1 + Math.floor(next() * 300), intensity: unit(), sharpness: unit() },
  );
  return { schema: 1, name: "Fuzz", description: "", tone: "neutral", events, trailingMs: ms(300) };
}

const NAMED: Record<string, number> = { LIGHT: 0.25, MEDIUM: 0.5, STRONG: 0.75, HIGH: 1 };

function replay(code: string) {
  const body = code.slice(code.indexOf("{\n") + 2, code.lastIndexOf("\n}")).split("\n").map((l) => l.trim());
  let cursor = 0;
  const events: { type: string; startMs: number; durationMs: number; intensity: number; sharpness: number }[] = [];
  for (const line of body) {
    const call = line.match(/^(delay|continuous|transient)\((.*)\)$/);
    if (!call) throw new Error(`Unexpected line: ${line}`);
    const [, name, rawArgs] = call;
    const args = rawArgs ? rawArgs.split(", ") : [];
    if (name === "delay") {
      cursor += Number(args[0].replace(".ms", ""));
      continue;
    }
    let durationMs = 0;
    if (name === "continuous") durationMs = Number(args.shift()!.replace(".ms", ""));
    let intensity = 0.5;
    let sharpness = 0.5;
    for (const arg of args) {
      const named = arg.match(/^HapticIntensity\.([A-Z]+)$/);
      const custom = arg.match(/^HapticIntensity\.Custom\(([\d.]+)f\)$/);
      const sharp = arg.match(/^sharpness = ([\d.]+)f$/);
      if (named) intensity = NAMED[named[1]];
      else if (custom) intensity = Number(custom[1]);
      else if (sharp) sharpness = Number(sharp[1]);
      else throw new Error(`Unexpected argument: ${arg}`);
    }
    events.push({ type: name, startMs: cursor, durationMs, intensity, sharpness });
    cursor += durationMs;
  }
  return { events, durationMs: cursor };
}

test("generated Kotlin replays to the card's timeline for 2,000 random patterns", () => {
  const next = random(42);
  const seen = new Set<string>();
  for (let i = 0; i < 2000; i++) {
    const pattern = randomPattern(next);
    seen.add(JSON.stringify(pattern));
    expect(validatePattern(pattern).ok).toBe(true);
    const timeline = toTimeline(pattern);
    const replayed = replay(patternCode(pattern));
    expect(replayed.durationMs).toBe(timeline.durationMs);
    expect(replayed.events).toEqual(
      timeline.events.map((e) => ({
        type: e.type === "transient" ? "transient" : "continuous",
        startMs: e.startMs,
        durationMs: e.durationMs,
        intensity: e.intensity,
        sharpness: e.sharpness,
      })),
    );
  }
  // A generator that cycles early would test the same patterns over and over. Tiny patterns can repeat by chance.
  expect(seen.size).toBeGreaterThan(1900);
});
