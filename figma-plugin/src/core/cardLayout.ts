import type { Timeline } from "./model";

// Geometry shared by the canvas card and the plugin window's timeline: width is time, height is strength.

export interface Box {
  x: number;
  y: number;
  width: number;
  height: number;
}

export type BarShape =
  | { kind: "continuous"; index: number; x: number; y: number; width: number; height: number }
  | { kind: "transient"; index: number; x: number; y: number; height: number };

export const CARD = {
  width: 280,
  height: 178,
  radius: 12,
  timeline: { x: 14, y: 56, width: 252, height: 72 },
  headroom: 8,
  minBarWidth: 2,
  minBarHeight: 1,
  tapLineWidth: 2,
  tapDot: 6,
} as const;

/** Milliseconds shown across the timeline, matching the sample app: 10% beyond the end, at least 200ms. */
export function displayWindowMs(durationMs: number): number {
  return Math.max(Math.floor(durationMs * 1.1), 200);
}

export function layoutBars(timeline: Timeline, box: Box, windowMs: number): BarShape[] {
  const scale = box.width / windowMs;
  const maxHeight = box.height - CARD.headroom;
  return timeline.events.map((event, index) => {
    const x = box.x + event.startMs * scale;
    const height = Math.max(event.intensity * maxHeight, CARD.minBarHeight);
    const y = box.y + box.height - height;
    if (event.type === "transient") return { kind: "transient", index, x, y, height };
    return { kind: "continuous", index, x, y, width: Math.max(event.durationMs * scale, CARD.minBarWidth), height };
  });
}
