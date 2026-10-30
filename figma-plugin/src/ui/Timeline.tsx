import { useEffect, useState, type CSSProperties } from "react";
import { displayWindowMs, layoutBars } from "../core/cardLayout";
import { TONE_HEX, toTimeline, type PatternSpec } from "../core/model";

interface TimelineProps {
  pattern: PatternSpec;
  width: number;
  height: number;
  selectedIndex?: number;
  onSelect?: (index: number) => void;
  /** Adds the axis labels and a button that replays the rhythm in real time. */
  detailed?: boolean;
}

const HIT_WIDTH = 12;
const FLASH_MS = 160;

export function Timeline({ pattern, width, height, selectedIndex, onSelect, detailed = false }: TimelineProps) {
  const [playCount, setPlayCount] = useState(0);
  const [playing, setPlaying] = useState(false);
  const [lit, setLit] = useState<ReadonlySet<number>>(new Set());
  const timeline = toTimeline(pattern);
  const windowMs = displayWindowMs(timeline.durationMs);
  const inset = detailed ? 8 : 4;
  const box = { x: inset, y: inset, width: width - inset * 2, height: height - inset };
  const bars = layoutBars(timeline, box, windowMs);
  const tone = TONE_HEX[pattern.tone];
  const sweep = (timeline.durationMs / windowMs) * box.width;
  const litMs = (e: { startMs: number; durationMs: number }) => e.startMs + Math.max(e.durationMs, FLASH_MS);
  const playMs = Math.max(timeline.durationMs, ...timeline.events.map(litMs));

  // Playback runs on timers rather than on animation, which never runs under Reduce Motion: each event is lit
  // while it plays, and playback ends on time. It plays the rhythm as it was when Preview rhythm was pressed.
  useEffect(() => {
    if (!playing) return;
    const timers = timeline.events.flatMap((event, index) => [
      setTimeout(() => setLit((current) => new Set(current).add(index)), event.startMs),
      setTimeout(
        () =>
          setLit((current) => {
            const next = new Set(current);
            next.delete(index);
            return next;
          }),
        litMs(event),
      ),
    ]);
    timers.push(setTimeout(() => setPlaying(false), playMs));
    return () => {
      timers.forEach(clearTimeout);
      setLit(new Set());
    };
  }, [playing, playCount]);

  return (
    <div className={detailed ? "timeline timeline-detailed" : "timeline"}>
      <svg
        key={playCount}
        viewBox={`0 0 ${width} ${height}`}
        className={playing ? "timeline-svg playing" : "timeline-svg"}
        aria-hidden={!detailed}
        role={detailed ? "img" : undefined}
        aria-label={detailed ? `${pattern.name} rhythm, ${timeline.durationMs} ms` : undefined}
      >
        {bars.map((bar) => {
          const selected = bar.index === selectedIndex;
          const hitX = bar.kind === "continuous" ? bar.x : bar.x - HIT_WIDTH / 2;
          const hitWidth = bar.kind === "continuous" ? Math.max(bar.width, HIT_WIDTH) : HIT_WIDTH;
          const className = ["bar", selected && "selected", lit.has(bar.index) && "lit"].filter(Boolean).join(" ");
          return (
            <g key={bar.index} className={className}>
              {bar.kind === "continuous" ? (
                <rect x={bar.x} y={bar.y} width={bar.width} height={bar.height} rx={1.5} fill={tone} />
              ) : (
                <>
                  <rect x={bar.x} y={bar.y} width={2} height={bar.height} fill={tone} />
                  <circle cx={bar.x + 1} cy={bar.y} r={3} fill={tone} />
                </>
              )}
              {onSelect && (
                <rect
                  className="bar-hit"
                  x={hitX}
                  y={0}
                  width={hitWidth}
                  height={height}
                  onClick={() => onSelect(bar.index)}
                />
              )}
            </g>
          );
        })}
        {playing && (
          <line
            className="playhead"
            x1={box.x}
            x2={box.x}
            y1={0}
            y2={height}
            style={{ "--sweep": `${sweep}px`, animationDuration: `${timeline.durationMs}ms` } as CSSProperties}
          />
        )}
      </svg>
      {detailed && (
        <div className="timeline-meta">
          <span>0</span>
          <button
            type="button"
            className="play-button"
            onClick={() => {
              setPlayCount((count) => count + 1);
              setPlaying(true);
            }}
          >
            <svg viewBox="0 0 10 10" width="8" height="8" aria-hidden="true">
              <path d="M2 1 9 5 2 9Z" fill="currentColor" />
            </svg>
            Preview rhythm
          </button>
          <span>{windowMs} ms</span>
        </div>
      )}
    </div>
  );
}
