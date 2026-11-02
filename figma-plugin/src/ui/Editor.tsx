import { useEffect, useRef, useState } from "react";
import { insertEventAfter, removeEvent, setEventType, setTrailing, updateEvent } from "../core/editing";
import { LIMITS, TONE_HEX, normalizePattern, validatePattern, type PatternSpec, type Tone } from "../core/model";
import { EventInspector, RangeField } from "./EventInspector";
import { Timeline } from "./Timeline";

// Edits to a card reach it once the designer pauses this long, and at once if they select something else first.
const APPLY_DELAY_MS = 300;

/** The editor's pattern, with the saved one it started from and what it sent that the card hasn't shown yet. */
interface Edits {
  base: PatternSpec;
  pattern: PatternSpec;
  /** Oldest first, so the card coming back with one of them isn't taken for someone else's change. */
  inFlight: PatternSpec[];
}

interface EditorProps {
  mode: "create" | "update";
  /** The card's saved pattern when updating, or the preset a new haptic starts from. */
  initial: PatternSpec;
  /** Create card. */
  onSubmit?: (pattern: PatternSpec) => void;
  /** Receives each edit to a card as it settles, like Figma's own panels, so there's nothing to save. */
  onApply?: (pattern: PatternSpec) => void;
  /** Counts edits the card refused. Each drops the oldest edit on its way; with none left, the card shows as it is. */
  refusedEdits?: number;
  onBack?: () => void;
  /** An earlier request is still being handled. */
  busy?: boolean;
}

const TONES = Object.keys(TONE_HEX) as Tone[];

export function Editor({ mode, initial, onSubmit, onApply, refusedEdits = 0, onBack, busy = false }: EditorProps) {
  const [edits, setEdits] = useState<Edits>({ base: initial, pattern: initial, inFlight: [] });
  const draft = edits.pattern;
  const setDraft = (pattern: PatternSpec) => setEdits((current) => ({ ...current, pattern }));
  const [selected, setSelected] = useState(0);
  const index = Math.min(selected, draft.events.length - 1);
  const validation = validatePattern(draft);

  const saved = useRef(initial);
  useEffect(() => {
    if (saved.current === initial) return;
    saved.current = initial;
    setEdits((current) => reconcile(current, initial));
  }, [initial]);

  // A refused edit never comes back, so waiting for it would stop the editor following undo and teammates.
  const refused = useRef(refusedEdits);
  useEffect(() => {
    const count = refusedEdits - refused.current;
    refused.current = refusedEdits;
    if (count <= 0) return;
    setEdits((current) => {
      const inFlight = current.inFlight.slice(count);
      return inFlight.length > 0 ? { ...current, inFlight } : { base: initial, pattern: initial, inFlight: [] };
    });
  }, [refusedEdits]);

  const apply = useRef(onApply);
  useEffect(() => {
    apply.current = onApply;
  });
  // The card will hold the last edit sent once it arrives, so a draft is compared with that, not with the card now.
  const pending = useRef<PatternSpec | null>(null);
  useEffect(() => {
    const expected = edits.inFlight.at(-1) ?? initial;
    const changed = mode === "update" && validation.ok && !samePattern(draft, expected);
    pending.current = changed ? normalizePattern(validation.pattern) : null;
    if (!pending.current) return;
    const timer = setTimeout(sendPending, APPLY_DELAY_MS);
    return () => clearTimeout(timer);
  }, [draft, initial, edits.inFlight]);
  useEffect(() => () => sendPending(), []);

  function sendPending() {
    const next = pending.current;
    if (!next) return;
    pending.current = null;
    setEdits((current) => ({ ...current, inFlight: [...current.inFlight, next] }));
    apply.current?.(next);
  }

  return (
    <div className={mode === "update" ? "editor editor-live" : "editor"}>
      {onBack && (
        <button type="button" className="back-button" onClick={onBack}>
          <svg viewBox="0 0 16 16" width="12" height="12" aria-hidden="true">
            <path d="M10 3 5 8l5 5" fill="none" stroke="currentColor" strokeWidth="1.5" />
          </svg>
          Presets
        </button>
      )}

      <div className="identity">
        <label className="text-field">
          <span>Name</span>
          <input
            value={draft.name}
            maxLength={LIMITS.maxNameLength}
            onChange={(e) => setDraft({ ...draft, name: e.target.value })}
          />
        </label>
        <div className="tones" role="radiogroup" aria-label="Color">
          {TONES.map((tone) => (
            <button
              key={tone}
              type="button"
              role="radio"
              aria-checked={draft.tone === tone}
              aria-label={tone}
              className="tone"
              style={{ background: TONE_HEX[tone] }}
              onClick={() => setDraft({ ...draft, tone })}
            />
          ))}
        </div>
        <label className="text-field description-field">
          <span>Description</span>
          <input
            value={draft.description}
            maxLength={80}
            placeholder="What it should feel like"
            onChange={(e) => setDraft({ ...draft, description: e.target.value })}
          />
        </label>
      </div>
      {mode === "update" && !validation.ok && <p className="form-error">{validation.error}</p>}

      <Timeline pattern={draft} width={320} height={96} selectedIndex={index} onSelect={setSelected} detailed />

      <EventInspector
        event={draft.events[index]}
        index={index}
        count={draft.events.length}
        canAdd={draft.events.length < LIMITS.maxEvents}
        onChange={(patch) => setDraft(updateEvent(draft, index, patch))}
        onType={(type) => setDraft(setEventType(draft, index, type))}
        onAddAfter={() => {
          setDraft(insertEventAfter(draft, index));
          setSelected(index + 1);
        }}
        onRemove={() => {
          setDraft(removeEvent(draft, index));
          setSelected(Math.max(0, index - 1));
        }}
        onSelect={setSelected}
      />

      <div className="pattern-settings">
        <RangeField
          label="Silence after"
          unit="ms"
          value={draft.trailingMs}
          min={0}
          sliderMax={1000}
          max={10_000}
          onChange={(trailingMs) => setDraft(setTrailing(draft, trailingMs))}
        />
      </div>

      {mode === "create" && (
        <div className="editor-footer">
          {!validation.ok && <p className="form-error">{validation.error}</p>}
          <button
            type="button"
            className="primary"
            disabled={!validation.ok || busy}
            onClick={() => {
              if (validation.ok) onSubmit?.(normalizePattern(validation.pattern));
            }}
          >
            Create card
          </button>
        </div>
      )}
    </div>
  );
}

// The saved card can change underneath: this editor's own edit coming back, undo, or a teammate's edit. Without
// edits still on their way, show the new values; with them, keep the edits, which reach the card next.
function reconcile(edits: Edits, saved: PatternSpec): Edits {
  // One of this editor's edits landed, and every edit sent before it with it.
  const landed = edits.inFlight.findIndex((sent) => samePattern(sent, saved));
  if (landed >= 0) return { base: saved, pattern: edits.pattern, inFlight: edits.inFlight.slice(landed + 1) };
  // A later edit is still on its way and will replace whatever this is.
  if (edits.inFlight.length > 0) return edits;
  // Keep what was typed: normalizing trims the name, so the card can come back without a space just typed.
  if (samePattern(edits.pattern, saved)) return { ...edits, base: saved };
  if (samePattern(edits.pattern, edits.base)) return { base: saved, pattern: saved, inFlight: [] };
  return edits;
}

function samePattern(a: PatternSpec, b: PatternSpec): boolean {
  return JSON.stringify(normalizePattern(a)) === JSON.stringify(normalizePattern(b));
}
