import { LIMITS, normalizePattern, type BuzzEvent, type HapticEvent, type PatternSpec } from "./model";

// Editor operations. Each returns a new pattern and leaves the input (often a preset) untouched.

export const NEW_BUZZ: BuzzEvent = { type: "continuous", gapBeforeMs: 50, durationMs: 50, intensity: 0.5, sharpness: 0.5 };

const DEFAULT_LENGTH_MS = 50;

export function insertEventAfter(p: PatternSpec, index: number): PatternSpec {
  if (p.events.length >= LIMITS.maxEvents) return p;
  const events = [...p.events];
  events.splice(index + 1, 0, { ...NEW_BUZZ });
  return { ...p, events };
}

export function removeEvent(p: PatternSpec, index: number): PatternSpec {
  if (p.events.length <= 1) return p;
  return { ...p, events: p.events.filter((_, i) => i !== index) };
}

export function updateEvent(
  p: PatternSpec,
  index: number,
  patch: Partial<Omit<BuzzEvent, "type">>,
): PatternSpec {
  const events = p.events.map((e, i) => (i === index ? ({ ...e, ...patch } as HapticEvent) : e));
  // Only the events are clamped: the name may be mid-typing ("Pay "), and is tidied when the card is saved.
  return { ...p, events: normalizePattern({ ...p, events }).events };
}

export function setTrailing(p: PatternSpec, trailingMs: number): PatternSpec {
  return { ...p, trailingMs: normalizePattern({ ...p, trailingMs }).trailingMs };
}

export function setEventType(p: PatternSpec, index: number, type: HapticEvent["type"]): PatternSpec {
  const events = p.events.map((e, i): HapticEvent => {
    if (i !== index || e.type === type) return e;
    const { gapBeforeMs, intensity, sharpness } = e;
    return type === "transient"
      ? { type, gapBeforeMs, intensity, sharpness }
      : { type, gapBeforeMs, durationMs: DEFAULT_LENGTH_MS, intensity, sharpness };
  });
  return { ...p, events };
}
