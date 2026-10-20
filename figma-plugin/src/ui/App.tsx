import { useCallback, useEffect, useRef, useState, type ReactNode } from "react";
import { codeSections } from "../core/codegen";
import type { PatternSpec } from "../core/model";
import type { CardSummary, MainToUi, SelectionState, UiToMain } from "../shared/messages";
import { figmaBridge, type Bridge } from "./bridge";
import { CodePanel } from "./CodePanel";
import { Editor } from "./Editor";
import { Gallery } from "./Gallery";
import { LinkPanel } from "./LinkPanel";

type Status = Extract<MainToUi, { type: "status" }>;
type Tab = "main" | "code";

// Confirmations clear quickly; warnings and errors stay long enough to read.
const STATUS_MS = { success: 4000, warning: 10_000, error: 10_000 } as const;
const BUSY_TIMEOUT_MS = 10_000;
const UNSAVED_NOTICE = "This card has unsaved changes. Press Update card to include them in the code.";

export function App({ bridge = figmaBridge }: { bridge?: Bridge }) {
  const [selection, setSelection] = useState<SelectionState | null>(null);
  // A new haptic being shaped before it becomes a card. It outlives selection changes, so clicking a
  // layer and coming back doesn't throw the tweaks away; a created card ends it.
  const [draft, setDraft] = useState<{ id: number; pattern: PatternSpec } | null>(null);
  const draftIds = useRef(0);
  const creating = useRef(false);
  const [cards, setCards] = useState<CardSummary[] | null>(null);
  const [tab, setTab] = useState<Tab>("main");
  const [status, setStatus] = useState<Status | null>(null);
  const [unsaved, setUnsaved] = useState(false);
  const [busy, setBusy] = useState(false);
  const busyRef = useRef(false);
  const selectedNodeId = useRef<string | null>(null);

  useEffect(() => {
    const unsubscribe = bridge.subscribe((message) => {
      if (message.type === "selection") {
        // The main thread re-sends the selection after every change; only a new layer resets the tab
        // and the card list (re-reading the list means searching every page).
        const nodeId = nodeIdOf(message.state);
        if (nodeId !== selectedNodeId.current) {
          setTab("main");
          setUnsaved(false);
          setCards(null);
        }
        selectedNodeId.current = nodeId;
        setSelection(message.state);
      } else if (message.type === "cards") {
        setCards(message.cards);
      } else {
        setStatus(message);
        if (creating.current) {
          creating.current = false;
          if (message.tone !== "error") setDraft(null);
        }
        // Every action ends with a status, success or error, so the buttons come back here.
        busyRef.current = false;
        setBusy(false);
      }
    });
    bridge.send({ type: "ready" });
    return unsubscribe;
  }, [bridge]);

  useEffect(() => {
    if (!status) return;
    const timer = setTimeout(() => setStatus(null), STATUS_MS[status.tone]);
    return () => clearTimeout(timer);
  }, [status]);

  const requestCards = useCallback(() => bridge.send({ type: "list-cards" }), [bridge]);

  // A double click must not create two cards: one action at a time until its status arrives.
  function act(message: UiToMain) {
    if (busyRef.current) return;
    busyRef.current = true;
    creating.current = message.type === "create-card";
    setBusy(true);
    bridge.send(message);
  }

  const keepDraft = useCallback(
    (pattern: PatternSpec) => setDraft((current) => (current && current.pattern !== pattern ? { ...current, pattern } : current)),
    [],
  );

  // If the main thread never answers, don't leave the buttons disabled for good.
  useEffect(() => {
    if (!busy) return;
    const timer = setTimeout(() => {
      busyRef.current = false;
      setBusy(false);
    }, BUSY_TIMEOUT_MS);
    return () => clearTimeout(timer);
  }, [busy]);

  function screen() {
    switch (selection?.kind) {
      case undefined:
        return null;
      case "none":
        return draft ? (
          <Editor
            key={draft.id}
            mode="create"
            initial={draft.pattern}
            onBack={() => setDraft(null)}
            onSubmit={(pattern) => act({ type: "create-card", pattern })}
            onDraftChange={keepDraft}
            busy={busy}
          />
        ) : (
          <Gallery onPick={(pattern) => setDraft({ id: ++draftIds.current, pattern })} />
        );
      case "card": {
        const { nodeId, pattern } = selection;
        if (!pattern) return <Notice>This card can't be read. It may come from a newer version of the plugin.</Notice>;
        return (
          <Tabbed
            mainLabel="Edit"
            tab={tab}
            onTab={setTab}
            code={
              <CodePanel
                sections={codeSections({ pattern, link: null, linkedPattern: null })}
                notice={unsaved ? UNSAVED_NOTICE : undefined}
              />
            }
          >
            <Editor
              key={nodeId}
              mode="update"
              initial={pattern}
              onSubmit={(next) => act({ type: "update-card", nodeId, pattern: next })}
              onDirtyChange={setUnsaved}
              busy={busy}
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
            onLink={(cardId, trigger) => act({ type: "link", nodeId, cardId, trigger })}
            onUnlink={() => act({ type: "unlink", nodeId })}
            busy={busy}
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
