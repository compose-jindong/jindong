import type { Trigger } from "./model";
import { DOCS_URL } from "./version";

// The Dev Mode note on a linked layer. Developers without the plugin see it too, so it names Jindong.

export const TRIGGER_LABEL: Record<Trigger, string> = {
  tap: "On tap",
  enter: "On screen enter",
  state: "On state change",
};

export const ANNOTATION_CATEGORY = { label: "Haptics", color: "blue" } as const;

const MARKER = "Jindong haptic";

export interface AnnotationLike {
  label?: string;
  labelMarkdown?: string;
  properties?: ReadonlyArray<{ type: string }>;
  categoryId?: string;
}

export function annotationMarkdown(cardName: string, trigger: Trigger): string {
  return `**${MARKER}:** ${escapeMarkdown(cardName)} · ${TRIGGER_LABEL[trigger]}\nMade with [Jindong](${DOCS_URL})`;
}

// A card name is shown as written, never as emphasis, code or a link.
function escapeMarkdown(text: string): string {
  return text.replace(/[\\`*_[\]<>#]/g, "\\$&");
}

/** The same note without markdown, for when Figma refuses links in notes. */
export function annotationLabel(cardName: string, trigger: Trigger): string {
  return `${MARKER}: ${cardName} · ${TRIGGER_LABEL[trigger]} — made with Jindong`;
}

// Only a note that starts like ours is ours; a teammate's note may mention Jindong haptics too.
export function isJindongAnnotation(a: AnnotationLike): boolean {
  const text = (a.labelMarkdown ?? a.label ?? "").trimStart();
  return text.startsWith(`**${MARKER}:**`) || text.startsWith(`${MARKER}:`);
}

/** Replaces our note with [next] (or removes it when null) and keeps everyone else's notes. */
export function mergeJindongAnnotation(existing: readonly AnnotationLike[], next: AnnotationLike | null): AnnotationLike[] {
  const kept = existing.filter((a) => !isJindongAnnotation(a)).map(writable);
  return next ? [...kept, next] : kept;
}

// Figma reads back both label forms but accepts only one when writing.
function writable(a: AnnotationLike): AnnotationLike {
  const result: AnnotationLike = {};
  if (a.labelMarkdown !== undefined) result.labelMarkdown = a.labelMarkdown;
  else if (a.label !== undefined) result.label = a.label;
  if (a.properties !== undefined) result.properties = a.properties;
  if (a.categoryId !== undefined) result.categoryId = a.categoryId;
  return result;
}
