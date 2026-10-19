import { useEffect, useState } from "react";
import type { PatternSpec } from "../core/model";
import type { MainToUi, SelectionState } from "../shared/messages";
import { figmaBridge, type Bridge } from "./bridge";
import { Editor } from "./Editor";
import { Gallery } from "./Gallery";

type Status = Extract<MainToUi, { type: "status" }>;

const STATUS_MS = 4000;

export function App({ bridge = figmaBridge }: { bridge?: Bridge }) {
  const [selection, setSelection] = useState<SelectionState | null>(null);
  const [draft, setDraft] = useState<PatternSpec | null>(null);
  const [status, setStatus] = useState<Status | null>(null);

  useEffect(() => {
    const unsubscribe = bridge.subscribe((message) => {
      if (message.type === "selection") {
        setSelection(message.state);
        // A new card or layer takes over the window, so an unsaved preset pick ends here.
        if (message.state.kind !== "none") setDraft(null);
      } else if (message.type === "status") {
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
          <Editor
            key={nodeId}
            mode="update"
            initial={pattern}
            onSubmit={(next) => bridge.send({ type: "update-card", nodeId, pattern: next })}
          />
        );
      }
      case "layer":
        return <Notice>Link panel — coming in Task 8</Notice>;
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

function Notice({ children }: { children: string }) {
  return <p className="notice">{children}</p>;
}
