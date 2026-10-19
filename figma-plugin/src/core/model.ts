// The haptic pattern a card holds. Events play one after another: each waits `gapBeforeMs`
// after the previous one ends, then plays. A transient (Tap) takes no time.

export const SCHEMA_VERSION = 1;
export const NAMESPACE = "jindong";
export const LIMITS = { maxEvents: 50, maxMs: 10_000, maxNameLength: 40 } as const;

export type Tone = "neutral" | "green" | "blue" | "red" | "orange";

export const TONE_HEX: Record<Tone, string> = {
  neutral: "#A1A1AA",
  green: "#22C55E",
  blue: "#2F5FE0",
  red: "#EF4444",
  orange: "#F59E0B",
};

export type TapEvent = { type: "transient"; gapBeforeMs: number; intensity: number; sharpness: number };
export type BuzzEvent = {
  type: "continuous";
  gapBeforeMs: number;
  durationMs: number;
  intensity: number;
  sharpness: number;
};
export type HapticEvent = TapEvent | BuzzEvent;

export interface PatternSpec {
  schema: 1;
  name: string;
  description: string;
  tone: Tone;
  events: HapticEvent[];
  trailingMs: number;
}

export type Trigger = "tap" | "enter" | "state";

export interface HapticLink {
  schema: 1;
  cardId: string;
  trigger: Trigger;
}

export interface TimelineEvent {
  type: HapticEvent["type"];
  startMs: number;
  durationMs: number;
  intensity: number;
  sharpness: number;
}

export interface Timeline {
  events: TimelineEvent[];
  durationMs: number;
}

/** Places each event on an absolute timeline, the same way Jindong's `delay` and events do. */
export function toTimeline(p: PatternSpec): Timeline {
  let cursor = 0;
  const events = p.events.map((e) => {
    const startMs = cursor + e.gapBeforeMs;
    const durationMs = e.type === "continuous" ? e.durationMs : 0;
    cursor = startMs + durationMs;
    return { type: e.type, startMs, durationMs, intensity: e.intensity, sharpness: e.sharpness };
  });
  return { events, durationMs: cursor + p.trailingMs };
}

const INVALID = "Invalid haptic data.";

export function validatePattern(value: unknown): { ok: true; pattern: PatternSpec } | { ok: false; error: string } {
  if (!isRecord(value)) return { ok: false, error: INVALID };
  if (typeof value.schema === "number" && value.schema > SCHEMA_VERSION) {
    return { ok: false, error: "This card was made with a newer version of the Jindong plugin." };
  }
  const { schema, name, description, tone, events, trailingMs } = value;
  if (schema !== SCHEMA_VERSION || typeof name !== "string" || typeof description !== "string") {
    return { ok: false, error: INVALID };
  }
  if (!isTone(tone) || !Array.isArray(events) || !isMs(trailingMs)) return { ok: false, error: INVALID };
  if (name.trim().length === 0) return { ok: false, error: "Give the haptic a name." };
  if (name.trim().length > LIMITS.maxNameLength) return { ok: false, error: INVALID };
  if (events.length === 0) return { ok: false, error: "Add at least one event." };
  if (events.length > LIMITS.maxEvents) return { ok: false, error: "A haptic can have at most 50 events." };
  if (!events.every(isEvent)) return { ok: false, error: INVALID };
  return {
    ok: true,
    pattern: { schema: SCHEMA_VERSION, name, description, tone, events: events.map(copyEvent), trailingMs },
  };
}

/** Clamps editor input into data that passes [validatePattern], apart from the event count and name. */
export function normalizePattern(p: PatternSpec): PatternSpec {
  return {
    schema: SCHEMA_VERSION,
    name: p.name.trim().slice(0, LIMITS.maxNameLength).trim(),
    description: p.description,
    tone: p.tone,
    events: p.events.slice(0, LIMITS.maxEvents).map(normalizeEvent),
    trailingMs: clampMs(p.trailingMs, 0),
  };
}

export function parsePatternJson(json: string): PatternSpec | null {
  if (!json) return null;
  try {
    const result = validatePattern(JSON.parse(json));
    return result.ok ? result.pattern : null;
  } catch {
    return null;
  }
}

export function parseLinkJson(json: string): HapticLink | null {
  if (!json) return null;
  try {
    const v: unknown = JSON.parse(json);
    if (isRecord(v) && v.schema === SCHEMA_VERSION && typeof v.cardId === "string" && v.cardId && isTrigger(v.trigger)) {
      return { schema: SCHEMA_VERSION, cardId: v.cardId, trigger: v.trigger };
    }
  } catch {
    // Unreadable link data reads as no link.
  }
  return null;
}

function normalizeEvent(e: HapticEvent): HapticEvent {
  const gapBeforeMs = clampMs(e.gapBeforeMs, 0);
  const intensity = clampUnit(e.intensity);
  const sharpness = clampUnit(e.sharpness);
  if (e.type === "continuous") {
    return { type: "continuous", gapBeforeMs, durationMs: clampMs(e.durationMs, 1), intensity, sharpness };
  }
  return { type: "transient", gapBeforeMs, intensity, sharpness };
}

function copyEvent(e: HapticEvent): HapticEvent {
  if (e.type === "continuous") {
    return { type: e.type, gapBeforeMs: e.gapBeforeMs, durationMs: e.durationMs, intensity: e.intensity, sharpness: e.sharpness };
  }
  return { type: e.type, gapBeforeMs: e.gapBeforeMs, intensity: e.intensity, sharpness: e.sharpness };
}

function clampMs(v: number, min: number): number {
  if (!Number.isFinite(v)) return min;
  return Math.min(LIMITS.maxMs, Math.max(min, Math.round(v)));
}

function clampUnit(v: number): number {
  if (!Number.isFinite(v)) return 0.5;
  return Math.min(1, Math.max(0, Math.round(v * 100) / 100));
}

function isEvent(e: unknown): e is HapticEvent {
  if (!isRecord(e) || !isMs(e.gapBeforeMs) || !isUnit(e.intensity) || !isUnit(e.sharpness)) return false;
  if (e.type === "transient") return true;
  return e.type === "continuous" && isMs(e.durationMs) && e.durationMs >= 1;
}

function isMs(v: unknown): v is number {
  return typeof v === "number" && Number.isInteger(v) && v >= 0 && v <= LIMITS.maxMs;
}

// Strength and sharpness use at most two decimals, so generated Kotlin floats match the card exactly.
function isUnit(v: unknown): v is number {
  return typeof v === "number" && Number.isFinite(v) && v >= 0 && v <= 1 && Math.abs(v * 100 - Math.round(v * 100)) < 1e-9;
}

function isTone(v: unknown): v is Tone {
  return typeof v === "string" && Object.prototype.hasOwnProperty.call(TONE_HEX, v);
}

function isTrigger(v: unknown): v is Trigger {
  return v === "tap" || v === "enter" || v === "state";
}

function isRecord(v: unknown): v is Record<string, unknown> {
  return typeof v === "object" && v !== null && !Array.isArray(v);
}
