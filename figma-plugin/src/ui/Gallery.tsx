import { toTimeline, type PatternSpec } from "../core/model";
import { PRESETS } from "../core/presets";
import { Timeline } from "./Timeline";

export function Gallery({ onPick }: { onPick: (pattern: PatternSpec) => void }) {
  return (
    <div className="gallery">
      <header className="gallery-header">
        <h1>Jindong</h1>
        <p>Pick a haptic to start.</p>
      </header>
      <ul className="preset-grid">
        {PRESETS.map((preset) => (
          <li key={preset.name}>
            <button type="button" className="preset" onClick={() => onPick(preset)}>
              <Timeline pattern={preset} width={140} height={36} />
              <span className="preset-name">{preset.name}</span>
              <span className="preset-description">{preset.description}</span>
              <span className="preset-duration">{toTimeline(preset).durationMs} ms</span>
            </button>
          </li>
        ))}
      </ul>
      <p className="gallery-hint">
        Shape it, put it on the canvas, then select a button to link it. Developers get the Jindong code in Dev
        Mode.
      </p>
    </div>
  );
}
