import { clashesWith, uniqueCardName } from "../core/codegen";
import { validatePattern, type PatternSpec } from "../core/model";
import type { CardSummary, MainToUi, SelectionState, UiToMain } from "../shared/messages";
import { createCard, updateCard } from "./card";
import { registerCodegen } from "./devmode";
import { linkLayer, linkNoteStatus, unlinkLayer } from "./link";
import { readSelection } from "./selection";
import { findCards, pageOf, readPattern } from "./storage";

// Design Mode: the editor window. Dev Mode: code generation, and the same window read-only, since plugins can't
// change the file there.

// Reading a selection waits on card lookups, so an older read can finish after a newer one; only the
// latest read reaches the window.
let selectionReads = 0;

// Undo or a teammate's edit can change the shown card without changing the selection, so changes to the
// selected layer or its card re-send the selection.
let watchedIds = new Set<string>();
let watchedPage: PageNode | null = null;

// Requests run one at a time, so a second Create card sees the first card's name. handle() never rejects.
let requests: Promise<void> = Promise.resolve();

const readOnly = figma.editorType === "dev";

if (readOnly && figma.mode === "codegen") {
  registerCodegen();
} else {
  figma.showUI(__html__, { width: 360, height: 640, themeColors: true });
  watchCurrentPage();
  figma.on("selectionchange", () => void pushSelection().catch(postError));
  figma.on("currentpagechange", () => {
    watchCurrentPage();
    void pushSelection().catch(postError);
  });
  figma.ui.onmessage = (message: UiToMain) => {
    requests = requests.then(() => handle(message));
  };
}

function post(message: MainToUi): void {
  figma.ui.postMessage(message);
}

async function pushSelection(): Promise<void> {
  const read = ++selectionReads;
  const state = await readSelection();
  if (read !== selectionReads) return;
  watchedIds = new Set(nodeIdsOf(state));
  post({ type: "selection", state });
}

function watchCurrentPage(): void {
  watchedPage?.off("nodechange", onNodeChange);
  watchedPage = figma.currentPage;
  watchedPage.on("nodechange", onNodeChange);
}

function onNodeChange(event: NodeChangeEvent): void {
  if (event.nodeChanges.some((change) => watchedIds.has(change.id))) void pushSelection().catch(postError);
}

function nodeIdsOf(state: SelectionState): string[] {
  if (state.kind === "card") return [state.nodeId];
  if (state.kind === "layer") return state.link ? [state.nodeId, state.link.cardId] : [state.nodeId];
  return [];
}

async function handle(message: UiToMain): Promise<void> {
  try {
    if (readOnly && message.type !== "ready" && message.type !== "list-cards") {
      throw new Error("Dev Mode is read-only. Switch to Design Mode to change haptics.");
    }
    switch (message.type) {
      case "ready":
        post({ type: "editor", readOnly });
        await pushSelection();
        return;
      case "list-cards":
        try {
          post({ type: "cards", cards: await cardSummaries() });
        } catch (error) {
          // The Link panel waits for a list; give it one before reporting what went wrong.
          post({ type: "cards", cards: [] });
          throw error;
        }
        return;
      case "create-card": {
        const pattern = valid(message.pattern);
        const name = uniqueCardName(pattern.name, await cardNames());
        await createCard({ ...pattern, name });
        if (name === pattern.name) await finish("success", "Card created.");
        else await finish("success", `Card created as “${name}”, because another card already uses that name.`);
        return;
      }
      case "update-card": {
        const pattern = valid(message.pattern);
        await updateCard(message.nodeId, pattern);
        if (clashesWith(pattern.name, await cardNames(message.nodeId))) {
          await finish(
            "warning",
            `Card updated, but another card is also called “${pattern.name}”. Rename one so their code doesn't clash.`,
          );
        } else {
          await finish("success", "Card updated.");
        }
        return;
      }
      case "link": {
        const { annotation, layerType, inherited } = await linkLayer(message.nodeId, message.cardId, message.trigger);
        if (inherited) {
          await finish("success", "Linked through its main component, so this instance keeps following the component.");
          return;
        }
        const status = linkNoteStatus(annotation, layerType);
        await finish(status.tone, status.message);
        return;
      }
      case "unlink":
        if ((await unlinkLayer(message.nodeId)).inheritsFromMain) {
          await finish("success", "Link removed. This instance now follows its main component's haptic.");
        } else {
          await finish("success", "Link removed.");
        }
        return;
    }
  } catch (error) {
    postError(error);
  }
}

function postError(error: unknown): void {
  post({ type: "status", tone: "error", message: error instanceof Error ? error.message : String(error) });
}

// Data changed, so the window re-reads the selection even when the selection itself didn't move.
async function finish(tone: "success" | "warning", message: string): Promise<void> {
  await pushSelection();
  post({ type: "status", tone, message });
  // Without this, one Cmd+Z would undo every plugin action since the window opened.
  figma.commitUndo();
}

function valid(pattern: PatternSpec): PatternSpec {
  const result = validatePattern(pattern);
  if (!result.ok) throw new Error(result.error);
  return result.pattern;
}

// Names of the file's readable cards, except [excludeId], for keeping generated Kotlin values distinct.
async function cardNames(excludeId?: string): Promise<string[]> {
  const cards = await findCards();
  return cards.flatMap((card) => {
    const pattern = card.id === excludeId ? null : readPattern(card);
    return pattern ? [pattern.name] : [];
  });
}

async function cardSummaries(): Promise<CardSummary[]> {
  const cards = await findCards();
  return cards.flatMap((card) => {
    const pattern = readPattern(card);
    return pattern ? [{ id: card.id, name: pattern.name, tone: pattern.tone, pageName: pageOf(card)?.name ?? "" }] : [];
  });
}
