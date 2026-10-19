import { validatePattern, type PatternSpec } from "../core/model";
import type { CardSummary, MainToUi, UiToMain } from "../shared/messages";
import { createCard, updateCard } from "./card";
import { registerCodegen } from "./devmode";
import { linkLayer, unlinkLayer } from "./link";
import { readSelection } from "./selection";
import { findCards, pageOf, readPattern } from "./storage";

// Design Mode: the editor window. Dev Mode: code generation only.

if (figma.editorType === "dev") {
  if (figma.mode === "codegen") {
    registerCodegen();
  } else {
    figma.notify('Jindong edits haptics in Figma Design. In Dev Mode, pick "Jindong (Compose)" in the code panel.');
    figma.closePlugin();
  }
} else {
  figma.showUI(__html__, { width: 360, height: 640, themeColors: true });
  figma.on("selectionchange", () => void pushSelection());
  figma.on("currentpagechange", () => void pushSelection());
  figma.ui.onmessage = (message: UiToMain) => void handle(message);
}

function post(message: MainToUi): void {
  figma.ui.postMessage(message);
}

async function pushSelection(): Promise<void> {
  post({ type: "selection", state: await readSelection() });
}

async function handle(message: UiToMain): Promise<void> {
  try {
    switch (message.type) {
      case "ready":
        await pushSelection();
        return;
      case "list-cards":
        post({ type: "cards", cards: await cardSummaries() });
        return;
      case "create-card":
        await createCard(valid(message.pattern));
        await finish("success", "Card created.");
        return;
      case "update-card":
        await updateCard(message.nodeId, valid(message.pattern));
        await finish("success", "Card updated.");
        return;
      case "link": {
        const { annotation } = await linkLayer(message.nodeId, message.cardId, message.trigger);
        if (annotation) await finish("success", "Linked. Developers will see it in Dev Mode.");
        else await finish("warning", "Linked, but Figma did not accept the Dev Mode note.");
        return;
      }
      case "unlink":
        await unlinkLayer(message.nodeId);
        await finish("success", "Link removed.");
        return;
    }
  } catch (error) {
    post({ type: "status", tone: "error", message: error instanceof Error ? error.message : String(error) });
  }
}

// Data changed, so the window re-reads the selection even when the selection itself didn't move.
async function finish(tone: "success" | "warning", message: string): Promise<void> {
  await pushSelection();
  post({ type: "status", tone, message });
}

function valid(pattern: PatternSpec): PatternSpec {
  const result = validatePattern(pattern);
  if (!result.ok) throw new Error(result.error);
  return result.pattern;
}

async function cardSummaries(): Promise<CardSummary[]> {
  const cards = await findCards();
  return cards.flatMap((card) => {
    const pattern = readPattern(card);
    return pattern ? [{ id: card.id, name: pattern.name, tone: pattern.tone, pageName: pageOf(card)?.name ?? "" }] : [];
  });
}
