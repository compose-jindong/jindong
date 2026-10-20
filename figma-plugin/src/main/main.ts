import { clashesWith, uniqueCardName } from "../core/codegen";
import { validatePattern, type PatternSpec } from "../core/model";
import type { CardSummary, MainToUi, UiToMain } from "../shared/messages";
import { createCard, updateCard } from "./card";
import { registerCodegen } from "./devmode";
import { linkLayer, linkNoteStatus, unlinkLayer } from "./link";
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
  figma.on("selectionchange", () => void pushSelection().catch(postError));
  figma.on("currentpagechange", () => void pushSelection().catch(postError));
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
        const { annotation, layerType } = await linkLayer(message.nodeId, message.cardId, message.trigger);
        const status = linkNoteStatus(annotation, layerType);
        await finish(status.tone, status.message);
        return;
      }
      case "unlink":
        await unlinkLayer(message.nodeId);
        await finish("success", "Link removed.");
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
