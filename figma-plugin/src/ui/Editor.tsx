import { useEffect, useRef, useState } from "react";
import { insertEventAfter, removeEvent, setEventType, setTrailing, updateEvent } from "../core/editing";
import { LIMITS, TONE_HEX, normalizePattern, validatePattern, type PatternSpec, type Tone } from "../core/model";
import { EventInspector, RangeField } from "./EventInspector";
import { Timeline } from "./Timeline";

// Edits to a card reach it once the designer pauses this long, and at once if they select something else first.
const APPLY_DELAY_MS = 300;

/** The editor's pattern, with the saved one it started from and the last one sent to the card. */
interface Edits {
  base: PatternSpec;
  pattern: PatternSpec;
  /** So the card coming back with this pattern isn't taken for someone else's change. */
  sent?: PatternSpec;
}

interface EditorProps {
  mode: "create" | "update";
  /** The card's saved pattern when updating, or the preset a new haptic starts from. */
  initial: PatternSpec;
  /** Create card. */
  onSubmit?: (pattern: PatternSpec) => void;
  /** Receives each edit to a card as it settles, like Figma's own panels, so there's nothing to save. */
  onApply?: (pattern: PatternSpec) => void;
  onBack?: () => void;
  /** An earlier request is still being handled. */
  busy?: boolean;
}

const TONES = Object.keys(TONE_HEX) as Tone[];

export function Editor({ mode, initial, onSubmit, onApply, onBack, busy = false }: EditorProps) {
  const [edits, setEdits] = useState<Edits>({ base: initial, pattern: initial });
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

  const apply = useRef(onApply);
  apply.current = onApply;
  const pending = useRef<PatternSpec | null>(null);
  useEffect(() => {
    const changed = mode === "update" && validation.ok && !samePattern(draft, initial);
    pending.current = changed ? normalizePattern(validation.pattern) : null;
    if (!pending.current) return;
    const timer = setTimeout(sendPending, APPLY_DELAY_MS);
    return () => clearTimeout(timer);
  }, [draft, initial]);
  useEffect(() => () => sendPending(), []);

  function sendPending() {
    const next = pending.current;
    if (!next) return;
    pending.current = null;
    setEdits((current) => ({ ...current, sent: next }));
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
  if (samePattern(edits.pattern, edits.base) || samePattern(edits.pattern, saved)) return { base: saved, pattern: saved };
  if (edits.sent && samePattern(edits.sent, saved)) return { base: saved, pattern: edits.pattern };
  return edits;
}

function samePattern(a: PatternSpec, b: PatternSpec): boolean {
  return JSON.stringify(normalizePattern(a)) === JSON.stringify(normalizePattern(b));
}
