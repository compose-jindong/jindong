import type { Timeline } from "../core/model";

// Preview rhythm plays the pattern as sound too, since a computer can't vibrate: a tap is a short knock and a buzz
// a hum for its length. Strength sets the volume and sharpness the pitch.

const VOLUME = 0.5;
// Square waves sound much louder than the knock's sine, so hums play quieter.
const HUM_LEVEL = 0.5;
// Exponential ramps can't reach zero.
const SILENT = 0.0001;
// A little lead, so the first event isn't clipped while the audio starts.
const LEAD_S = 0.02;

let shared: AudioContext | null = null;

function sharedContext(): AudioContext | null {
  if (!shared && typeof AudioContext !== "undefined") shared = new AudioContext();
  return shared;
}

/** Schedules [timeline] as sound and returns a function that silences it. */
export function playSound(timeline: Timeline, context: AudioContext | null = sharedContext()): () => void {
  if (!context) return () => {};
  // A browser may start audio suspended until a click; this runs from one. If it still can't play, stay silent.
  context.resume().catch(() => {});
  const output = context.createGain();
  output.gain.value = VOLUME;
  output.connect(context.destination);
  const start = context.currentTime + LEAD_S;
  for (const event of timeline.events) {
    const at = start + event.startMs / 1000;
    if (event.type === "transient") knock(context, output, at, event.intensity, event.sharpness);
    else hum(context, output, at, event.durationMs / 1000, event.intensity, event.sharpness);
  }
  return () => output.disconnect();
}

// A dull thud when soft-edged, a crisp tick when sharp.
function knock(context: AudioContext, output: AudioNode, at: number, strength: number, sharpness: number): void {
  const pitch = 300 + 1200 * sharpness;
  const length = 0.07 - 0.04 * sharpness;
  const oscillator = context.createOscillator();
  oscillator.frequency.setValueAtTime(pitch, at);
  oscillator.frequency.exponentialRampToValueAtTime(pitch / 2, at + length);
  const envelope = context.createGain();
  envelope.gain.setValueAtTime(SILENT, at);
  envelope.gain.exponentialRampToValueAtTime(Math.max(strength, SILENT), at + 0.002);
  envelope.gain.exponentialRampToValueAtTime(SILENT, at + length);
  oscillator.connect(envelope).connect(output);
  oscillator.start(at);
  oscillator.stop(at + length);
}

// A low hum like a vibration motor's, brighter and higher when sharp.
function hum(context: AudioContext, output: AudioNode, at: number, seconds: number, strength: number, sharpness: number): void {
  const oscillator = context.createOscillator();
  oscillator.type = "square";
  oscillator.frequency.setValueAtTime(110 + 170 * sharpness, at);
  const filter = context.createBiquadFilter();
  filter.type = "lowpass";
  filter.frequency.setValueAtTime(500 + 2500 * sharpness, at);
  const envelope = context.createGain();
  const level = strength * HUM_LEVEL;
  // Short fades at both ends keep the hum from clicking.
  const fade = Math.min(0.005, seconds / 3);
  envelope.gain.setValueAtTime(0, at);
  envelope.gain.linearRampToValueAtTime(level, at + fade);
  envelope.gain.setValueAtTime(level, at + seconds - fade);
  envelope.gain.linearRampToValueAtTime(0, at + seconds);
  oscillator.connect(filter).connect(envelope).connect(output);
  oscillator.start(at);
  oscillator.stop(at + seconds);
}
