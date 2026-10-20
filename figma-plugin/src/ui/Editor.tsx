import { useEffect, useState } from "react";
import { insertEventAfter, removeEvent, setEventType, setTrailing, updateEvent } from "../core/editing";
import { LIMITS, TONE_HEX, normalizePattern, validatePattern, type PatternSpec, type Tone } from "../core/model";
import { EventInspector, RangeField } from "./EventInspector";
import { Timeline } from "./Timeline";

interface EditorProps {
  mode: "create" | "update";
  initial: PatternSpec;
  onSubmit: (pattern: PatternSpec) => void;
  onBack?: () => void;
  /** Reports whether the draft differs from [initial], the pattern last saved to the card. */
  onDirtyChange?: (dirty: boolean) => void;
  /** An earlier request is still being handled. */
  busy?: boolean;
}

const TONES = Object.keys(TONE_HEX) as Tone[];

export function Editor({ mode, initial, onSubmit, onBack, onDirtyChange, busy = false }: EditorProps) {
  const [draft, setDraft] = useState(initial);
  const [selected, setSelected] = useState(0);
  const index = Math.min(selected, draft.events.length - 1);
  const validation = validatePattern(draft);
  const dirty = JSON.stringify(normalizePattern(draft)) !== JSON.stringify(normalizePattern(initial));

  useEffect(() => {
    onDirtyChange?.(dirty);
  }, [dirty, onDirtyChange]);

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
        {!validation.ok && <p className="form-error">{validation.error}</p>}
        <button
          type="button"
          className="primary"
          disabled={!validation.ok || busy}
          onClick={() => {
            if (validation.ok) onSubmit(normalizePattern(validation.pattern));
          }}
        >
          {mode === "create" ? "Create card" : "Update card"}
        </button>
      </div>
    </div>
  );
}
