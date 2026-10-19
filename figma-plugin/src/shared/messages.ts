import type { HapticLink, PatternSpec, Tone, Trigger } from "../core/model";

// Messages between the plugin window (UI) and the Figma main thread.

export type LayerWarning = "main-component" | "inside-instance" | null;

export type SelectionState =
  | { kind: "none" }
  | { kind: "multiple" }
  | { kind: "card"; nodeId: string; pattern: PatternSpec | null }
  | {
      kind: "layer";
      nodeId: string;
      nodeName: string;
      link: HapticLink | null;
      linkedPattern: PatternSpec | null;
      warning: LayerWarning;
    };

export interface CardSummary {
  id: string;
  name: string;
  tone: Tone;
  pageName: string;
}

export type UiToMain =
  | { type: "ready" }
  | { type: "list-cards" }
  | { type: "create-card"; pattern: PatternSpec }
  | { type: "update-card"; nodeId: string; pattern: PatternSpec }
  | { type: "link"; nodeId: string; cardId: string; trigger: Trigger }
  | { type: "unlink"; nodeId: string };

export type MainToUi =
  | { type: "selection"; state: SelectionState }
  | { type: "cards"; cards: CardSummary[] }
  | { type: "status"; tone: "success" | "warning" | "error"; message: string };
