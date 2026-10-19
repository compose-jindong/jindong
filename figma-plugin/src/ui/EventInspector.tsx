import { useId } from "react";
import type { BuzzEvent, HapticEvent } from "../core/model";

type EventPatch = Partial<Omit<BuzzEvent, "type">>;

interface EventInspectorProps {
  event: HapticEvent;
  index: number;
  count: number;
  canAdd: boolean;
  onChange: (patch: EventPatch) => void;
  onType: (type: HapticEvent["type"]) => void;
  onAddAfter: () => void;
  onRemove: () => void;
}

export function EventInspector({ event, index, count, canAdd, onChange, onType, onAddAfter, onRemove }: EventInspectorProps) {
  return (
    <section className="inspector" aria-label="Selected event">
      <div className="inspector-head">
        <span className="inspector-title">
          Event {index + 1} of {count}
        </span>
        <Segmented
          label="Event type"
          value={event.type}
          options={[
            { value: "transient", label: "Tap" },
            { value: "continuous", label: "Buzz" },
          ]}
          onChange={onType}
        />
      </div>
      <RangeField
        label="Pause before"
        unit="ms"
        value={event.gapBeforeMs}
        min={0}
        sliderMax={1000}
        max={10_000}
        onChange={(gapBeforeMs) => onChange({ gapBeforeMs })}
      />
      {event.type === "continuous" && (
        <RangeField
          label="Length"
          unit="ms"
          value={event.durationMs}
          min={1}
          sliderMax={1000}
          max={10_000}
          onChange={(durationMs) => onChange({ durationMs })}
        />
      )}
      <RangeField
        label="Strength"
        unit="%"
        value={Math.round(event.intensity * 100)}
        min={0}
        sliderMax={100}
        max={100}
        onChange={(percent) => onChange({ intensity: percent / 100 })}
      />
      <RangeField
        label="Sharpness"
        unit="%"
        value={Math.round(event.sharpness * 100)}
        min={0}
        sliderMax={100}
        max={100}
        onChange={(percent) => onChange({ sharpness: percent / 100 })}
      />
      <div className="inspector-actions">
        <button type="button" className="secondary" onClick={onAddAfter} disabled={!canAdd}>
          Add after
        </button>
        <button type="button" className="secondary" onClick={onRemove} disabled={count <= 1}>
          Delete
        </button>
      </div>
    </section>
  );
}

interface RangeFieldProps {
  label: string;
  unit: string;
  value: number;
  min: number;
  /** The slider covers the common range; the number box accepts up to [max]. */
  sliderMax: number;
  max: number;
  onChange: (value: number) => void;
}

export function RangeField({ label, unit, value, min, sliderMax, max, onChange }: RangeFieldProps) {
  const id = useId();
  return (
    <div className="range-field">
      <label htmlFor={id}>{label}</label>
      <input
        id={id}
        type="range"
        min={min}
        max={sliderMax}
        value={Math.min(value, sliderMax)}
        onChange={(e) => onChange(e.target.valueAsNumber)}
      />
      <span className="number-field">
        <input
          type="number"
          aria-label={`${label} (${unit})`}
          min={min}
          max={max}
          value={value}
          onChange={(e) => {
            const next = e.target.valueAsNumber;
            if (Number.isFinite(next)) onChange(next);
          }}
        />
        <span className="unit">{unit}</span>
      </span>
    </div>
  );
}

interface SegmentedProps<T extends string> {
  label: string;
  value: T;
  options: { value: T; label: string }[];
  onChange: (value: T) => void;
}

export function Segmented<T extends string>({ label, value, options, onChange }: SegmentedProps<T>) {
  return (
    <div className="segmented" role="group" aria-label={label}>
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          aria-pressed={option.value === value}
          onClick={() => onChange(option.value)}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}
