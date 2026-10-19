import { useCallback, useEffect, useRef, useState, type ReactNode } from "react";
import { codeSections } from "../core/codegen";
import type { PatternSpec } from "../core/model";
import type { CardSummary, MainToUi, SelectionState } from "../shared/messages";
import { figmaBridge, type Bridge } from "./bridge";
import { CodePanel } from "./CodePanel";
import { Editor } from "./Editor";
import { Gallery } from "./Gallery";
import { LinkPanel } from "./LinkPanel";

type Status = Extract<MainToUi, { type: "status" }>;
type Tab = "main" | "code";

const STATUS_MS = 4000;

export function App({ bridge = figmaBridge }: { bridge?: Bridge }) {
  const [selection, setSelection] = useState<SelectionState | null>(null);
  const [draft, setDraft] = useState<PatternSpec | null>(null);
  const [cards, setCards] = useState<CardSummary[] | null>(null);
  const [tab, setTab] = useState<Tab>("main");
  const [status, setStatus] = useState<Status | null>(null);
  const selectedNodeId = useRef<string | null>(null);

  useEffect(() => {
    const unsubscribe = bridge.subscribe((message) => {
      if (message.type === "selection") {
        // The main thread re-sends the selection after every change; only a new layer resets the tab.
        const nodeId = nodeIdOf(message.state);
        if (nodeId !== selectedNodeId.current) setTab("main");
        selectedNodeId.current = nodeId;
        setSelection(message.state);
        // A new card or layer takes over the window, so an unsaved preset pick ends here.
        if (message.state.kind !== "none") setDraft(null);
        setCards(null);
      } else if (message.type === "cards") {
        setCards(message.cards);
      } else {
        setStatus(message);
      }
    });
    bridge.send({ type: "ready" });
    return unsubscribe;
  }, [bridge]);

  useEffect(() => {
    if (!status) return;
    const timer = setTimeout(() => setStatus(null), STATUS_MS);
    return () => clearTimeout(timer);
  }, [status]);

  const requestCards = useCallback(() => bridge.send({ type: "list-cards" }), [bridge]);

  function screen() {
    switch (selection?.kind) {
      case undefined:
        return null;
      case "none":
        return draft ? (
          <Editor
            key={draft.name}
            mode="create"
            initial={draft}
            onBack={() => setDraft(null)}
            onSubmit={(pattern) => bridge.send({ type: "create-card", pattern })}
          />
        ) : (
          <Gallery onPick={setDraft} />
        );
      case "card": {
        const { nodeId, pattern } = selection;
        if (!pattern) return <Notice>This card can't be read. It may come from a newer version of the plugin.</Notice>;
        return (
          <Tabbed
            mainLabel="Edit"
            tab={tab}
            onTab={setTab}
            code={<CodePanel sections={codeSections({ pattern, link: null, linkedPattern: null })} />}
          >
            <Editor
              key={nodeId}
              mode="update"
              initial={pattern}
              onSubmit={(next) => bridge.send({ type: "update-card", nodeId, pattern: next })}
            />
          </Tabbed>
        );
      }
      case "layer": {
        const { nodeId, link, linkedPattern } = selection;
        const panel = (
          <LinkPanel
            key={nodeId}
            state={selection}
            cards={cards}
            onRequestCards={requestCards}
            onLink={(cardId, trigger) => bridge.send({ type: "link", nodeId, cardId, trigger })}
            onUnlink={() => bridge.send({ type: "unlink", nodeId })}
          />
        );
        if (!link || !linkedPattern) return panel;
        return (
          <Tabbed
            mainLabel="Link"
            tab={tab}
            onTab={setTab}
            code={<CodePanel sections={codeSections({ pattern: null, link, linkedPattern })} />}
          >
            {panel}
          </Tabbed>
        );
      }
      case "multiple":
        return <Notice>Select one layer to link a haptic, or clear the selection to make a new one.</Notice>;
    }
  }

  return (
    <div className="app">
      <main className="screen">{screen()}</main>
      <div role="status" className={status ? `status status-${status.tone}` : "status"}>
        {status?.message}
      </div>
    </div>
  );
}

// The main view stays mounted while Code is showing, so an unsaved edit survives a look at the code.
function Tabbed({
  mainLabel,
  tab,
  onTab,
  code,
  children,
}: {
  mainLabel: string;
  tab: Tab;
  onTab: (tab: Tab) => void;
  code: ReactNode;
  children: ReactNode;
}) {
  return (
    <>
      <div className="tabs" role="tablist">
        <button type="button" role="tab" aria-selected={tab === "main"} onClick={() => onTab("main")}>
          {mainLabel}
        </button>
        <button type="button" role="tab" aria-selected={tab === "code"} onClick={() => onTab("code")}>
          Code
        </button>
      </div>
      <div hidden={tab !== "main"}>{children}</div>
      {tab === "code" && code}
    </>
  );
}

function Notice({ children }: { children: string }) {
  return <p className="notice">{children}</p>;
}

function nodeIdOf(state: SelectionState | null): string | null {
  return state && "nodeId" in state ? state.nodeId : null;
}
