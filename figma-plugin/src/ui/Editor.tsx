import { useEffect, useRef, useState } from "react";
import { insertEventAfter, removeEvent, setEventType, setTrailing, updateEvent } from "../core/editing";
import { LIMITS, TONE_HEX, normalizePattern, validatePattern, type PatternSpec, type Tone } from "../core/model";
import { EventInspector, RangeField } from "./EventInspector";
import { Timeline } from "./Timeline";

/** Edits not yet on the card, with the saved pattern they started from. */
export interface UnsavedEdits {
  base: PatternSpec;
  pattern: PatternSpec;
  /** What Update card last sent, so the card coming back with it isn't taken for someone else's change. */
  saving?: PatternSpec;
}

interface EditorProps {
  mode: "create" | "update";
  initial: PatternSpec;
  onSubmit: (pattern: PatternSpec) => void;
  onBack?: () => void;
  /** Edits left on this card earlier, picked up again. */
  resume?: UnsavedEdits;
  /** Reports the edits that differ from [initial], the pattern last saved to the card, or null when there are none. */
  onUnsavedChange?: (edits: UnsavedEdits | null) => void;
  /** An earlier request is still being handled. */
  busy?: boolean;
  /** Receives every edit, so a new haptic's tweaks can outlive this editor. */
  onDraftChange?: (pattern: PatternSpec) => void;
}

const TONES = Object.keys(TONE_HEX) as Tone[];

export function Editor({
  mode,
  initial,
  onSubmit,
  onBack,
  resume,
  onUnsavedChange,
  busy = false,
  onDraftChange,
}: EditorProps) {
  const [edits, setEdits] = useState<UnsavedEdits>(() => reconcile(resume ?? { base: initial, pattern: initial }, initial));
  const draft = edits.pattern;
  const setDraft = (pattern: PatternSpec) => setEdits((current) => ({ ...current, pattern }));
  const [selected, setSelected] = useState(0);
  const index = Math.min(selected, draft.events.length - 1);
  const validation = validatePattern(draft);
  const dirty = !samePattern(draft, initial);
  const changedElsewhere = dirty && !samePattern(edits.base, initial);

  const saved = useRef(initial);
  useEffect(() => {
    if (saved.current === initial) return;
    saved.current = initial;
    setEdits((current) => reconcile(current, initial));
  }, [initial]);

  useEffect(() => {
    onUnsavedChange?.(dirty ? edits : null);
  }, [dirty, edits, onUnsavedChange]);

  useEffect(() => {
    onDraftChange?.(draft);
  }, [draft, onDraftChange]);

  return (
    <div className="editor">
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

      <div className="editor-footer">
        {changedElsewhere && (
          <div className="callout changed-elsewhere">
            <p>This card changed in Figma while you were editing. Update card replaces that change with yours.</p>
            <button type="button" className="secondary" onClick={() => setEdits({ base: initial, pattern: initial })}>
              Discard my edits
            </button>
          </div>
        )}
        {!validation.ok && <p className="form-error">{validation.error}</p>}
        <button
          type="button"
          className="primary"
          disabled={!validation.ok || busy || (mode === "update" && !dirty)}
          onClick={() => {
            if (!validation.ok) return;
            const pattern = normalizePattern(validation.pattern);
            setEdits((current) => ({ ...current, saving: pattern }));
            onSubmit(pattern);
          }}
        >
          {mode === "create" ? "Create card" : "Update card"}
        </button>
      </div>
    </div>
  );
}

// The saved card can change underneath (undo, a teammate's edit, this editor's own save). Without unsaved edits,
// show the new values; with them, keep the edits, and unless the change is this editor's own save, say so, so
// Update card is a deliberate overwrite.
function reconcile(edits: UnsavedEdits, saved: PatternSpec): UnsavedEdits {
  if (samePattern(edits.pattern, edits.base) || samePattern(edits.pattern, saved)) return { base: saved, pattern: saved };
  if (edits.saving && samePattern(edits.saving, saved)) return { base: saved, pattern: edits.pattern };
  return edits;
}

function samePattern(a: PatternSpec, b: PatternSpec): boolean {
  return JSON.stringify(normalizePattern(a)) === JSON.stringify(normalizePattern(b));
}
