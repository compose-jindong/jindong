import { useState } from "react";
import { createRoot } from "react-dom/client";
import type { HapticLink, PatternSpec } from "../core/model";
import { presetNamed } from "../core/presets";
import type { MainToUi, SelectionState, UiToMain } from "../shared/messages";
import { App } from "./App";
import type { Bridge } from "./bridge";
import "./styles.css";

// A pretend Figma for trying the plugin window in a browser (`yarn preview:ui`). It keeps cards and
// links in memory and answers the same messages the main thread does.
// ?state=none|card|layer|linked|multiple picks the first selection; &chrome=0 hides the toolbar;
// &page=<name> names the page the cards sit on, for checking long page names in the card list;
// &dev=1 opens the window the way Dev Mode does, read-only.

const params = new URLSearchParams(location.search);
const START: Record<string, string> = {
  none: "none",
  card: "card:1",
  layer: "layer:button",
  linked: "layer:linked",
  multiple: "multiple",
};
const LAYER_NAMES: Record<string, string> = { "layer:button": "Submit button", "layer:linked": "Pay button" };

const cards = new Map<string, PatternSpec>([
  ["card:1", presetNamed("Success")],
  ["card:2", presetNamed("Heartbeat")],
]);
const links = new Map<string, HapticLink>([["layer:linked", { schema: 1, cardId: "card:1", trigger: "tap" }]]);
let selected = START[params.get("state") ?? "none"] ?? "none";
let listener: ((message: MainToUi) => void) | null = null;
let nextCard = cards.size + 1;

const bridge: Bridge = {
  send: (message) => setTimeout(() => handle(message), 0),
  subscribe: (handler) => {
    listener = handler;
    return () => {
      listener = null;
    };
  },
};

function post(message: MainToUi) {
  listener?.(message);
}

function selection(): SelectionState {
  if (selected === "none" || selected === "multiple") return { kind: selected };
  const pattern = cards.get(selected);
  if (pattern) return { kind: "card", nodeId: selected, pattern };
  const link = links.get(selected) ?? null;
  return {
    kind: "layer",
    nodeId: selected,
    nodeName: LAYER_NAMES[selected] ?? selected,
    link,
    linkedPattern: link ? (cards.get(link.cardId) ?? null) : null,
    warning: null,
  };
}

function select(id: string) {
  selected = id;
  post({ type: "selection", state: selection() });
}

function handle(message: UiToMain) {
  switch (message.type) {
    case "ready":
      post({ type: "editor", readOnly: params.get("dev") === "1" });
      select(selected);
      return;
    case "list-cards":
      post({
        type: "cards",
        cards: [...cards].map(([id, p]) => ({ id, name: p.name, tone: p.tone, pageName: params.get("page") ?? "Haptics" })),
      });
      return;
    case "create-card": {
      const id = `card:${nextCard++}`;
      cards.set(id, message.pattern);
      select(id);
      post({ type: "status", tone: "success", message: "Card created." });
      return;
    }
    case "update-card":
      cards.set(message.nodeId, message.pattern);
      select(message.nodeId);
      post({ type: "status", tone: "success", message: "Card updated." });
      return;
    case "link":
      links.set(message.nodeId, { schema: 1, cardId: message.cardId, trigger: message.trigger });
      select(message.nodeId);
      post({ type: "status", tone: "success", message: "Linked. Developers will see it in Dev Mode." });
      return;
    case "unlink":
      links.delete(message.nodeId);
      select(message.nodeId);
      post({ type: "status", tone: "success", message: "Link removed." });
      return;
  }
}

const TARGETS: [string, string][] = [
  ["none", "Nothing"],
  ["card:1", "Success card"],
  ["layer:button", "Submit button"],
  ["layer:linked", "Pay button (linked)"],
  ["multiple", "Two layers"],
];

// &theme=dark sets the variables Figma provides in its dark theme (approximate values).
if (params.get("theme") === "dark") {
  const dark: Record<string, string> = {
    "--figma-color-bg": "#2c2c2c",
    "--figma-color-bg-secondary": "#383838",
    "--figma-color-bg-hover": "#444444",
    "--figma-color-text": "#ffffff",
    "--figma-color-text-secondary": "#ffffffb3",
    "--figma-color-text-tertiary": "#ffffff66",
    "--figma-color-border": "#444444",
    "--figma-color-border-selected": "#0c8ce9",
    "--figma-color-text-danger": "#ff7a5c",
    "--figma-color-text-warning": "#f7d15f",
    "--figma-color-text-success": "#79d297",
  };
  for (const [name, value] of Object.entries(dark)) document.documentElement.style.setProperty(name, value);
}

// &status=<text> shows that status message after load, for checking how long messages wrap.
const startStatus = params.get("status");
if (startStatus) setTimeout(() => post({ type: "status", tone: "warning", message: startStatus }), 300);

// &click=<button text> presses that button once after load, for screenshots ("Preview rhythm", "Code").
const autoClick = params.get("click");
if (autoClick) {
  setTimeout(() => {
    const button = [...document.querySelectorAll("button")].find((b) => b.textContent?.trim() === autoClick);
    button?.click();
  }, 200);
}

function Preview() {
  const [, rerender] = useState(0);
  if (params.get("chrome") === "0") return <App bridge={bridge} />;
  return (
    <div style={{ display: "flex", gap: 24, padding: 24, alignItems: "flex-start", background: "#ececec", minHeight: "100%" }}>
      <div style={{ flexShrink: 0, width: 360, height: 640, background: "var(--bg)", boxShadow: "0 2px 12px rgb(0 0 0 / 15%)", borderRadius: 8, overflow: "hidden" }}>
        <App bridge={bridge} />
      </div>
      <div style={{ display: "grid", gap: 6 }}>
        <strong>Select in “Figma”</strong>
        {TARGETS.map(([id, label]) => (
          <button
            key={id}
            type="button"
            className="secondary"
            onClick={() => {
              select(id);
              rerender((n) => n + 1);
            }}
          >
            {label}
          </button>
        ))}
        <strong>Elsewhere in “Figma”</strong>
        <button
          type="button"
          className="secondary"
          onClick={() => {
            // A teammate (or undo) changes the card while the plugin is open.
            const card = cards.get("card:1")!;
            cards.set("card:1", { ...card, name: card.name === "Order placed" ? "Success" : "Order placed" });
            if (selected === "card:1") select("card:1");
          }}
        >
          Rename Success card
        </button>
      </div>
    </div>
  );
}

createRoot(document.getElementById("root")!).render(<Preview />);
