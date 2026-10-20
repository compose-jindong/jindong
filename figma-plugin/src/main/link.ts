import {
  ANNOTATION_CATEGORY,
  annotationLabel,
  annotationMarkdown,
  mergeJindongAnnotation,
  type AnnotationLike,
} from "../core/annotation";
import { SCHEMA_VERSION, type Trigger } from "../core/model";
import { insideInstance } from "./selection";
import { clearLink, readLink, readPattern, writeLink } from "./storage";

// A link points a UI layer at a card. The Dev Mode note is written once, when linking.

/** Whether the Dev Mode note went on: some layer types (groups, sections…) can't carry notes at all. */
export type NoteResult = "added" | "unsupported" | "rejected";

export async function linkLayer(
  nodeId: string,
  cardId: string,
  trigger: Trigger,
): Promise<{ annotation: NoteResult; layerType: string }> {
  const node = await sceneNode(nodeId);
  const pattern = readPattern(await figma.getNodeByIdAsync(cardId));
  if (!pattern) throw new Error("That card no longer exists.");
  if (insideInstance(node)) throw new Error("Select the instance itself, not a layer inside it.");

  writeLink(node, { schema: SCHEMA_VERSION, cardId, trigger });
  if (!("annotations" in node)) return { annotation: "unsupported", layerType: node.type };

  const categoryId = await hapticsCategoryId();
  const markdown: AnnotationLike = { labelMarkdown: annotationMarkdown(pattern.name, trigger) };
  const plain: AnnotationLike = { label: annotationLabel(pattern.name, trigger) };
  // Try the note with a docs link first, then plain text that still names Jindong, then without the category.
  const notes = categoryId ? [{ ...markdown, categoryId }, { ...plain, categoryId }, plain] : [markdown, plain];
  const added = notes.some((note) => setJindongAnnotation(node, note));
  return { annotation: added ? "added" : "rejected", layerType: node.type };
}

export function linkNoteStatus(result: NoteResult, layerType: string): { tone: "success" | "warning"; message: string } {
  switch (result) {
    case "added":
      return { tone: "success", message: "Linked. Developers will see it in Dev Mode." };
    case "unsupported": {
      const kind = layerType.toLowerCase().replace(/_/g, " ");
      const article = /^[aeiou]/.test(kind) ? "an" : "a";
      return {
        tone: "warning",
        message: `Linked. Figma can't put a Dev Mode note on ${article} ${kind}, so only developers using this plugin will see it. Link a frame or instance to show a note.`,
      };
    }
    case "rejected":
      return { tone: "warning", message: "Linked, but Figma did not accept the Dev Mode note." };
  }
}

export async function unlinkLayer(nodeId: string): Promise<void> {
  const node = await sceneNode(nodeId);
  clearLink(node);
  // An instance shows its main component's link once its own value is gone; that one isn't ours to remove.
  if (readLink(node)) {
    throw new Error(
      "This link comes from the main component, so every instance has it. Remove it on the main component.",
    );
  }
  setJindongAnnotation(node, null);
}

async function sceneNode(nodeId: string): Promise<SceneNode> {
  const node = await figma.getNodeByIdAsync(nodeId);
  if (!node || node.type === "DOCUMENT" || node.type === "PAGE") throw new Error("That layer no longer exists.");
  return node;
}

// Returns false when the layer can't carry notes or Figma rejects the change; the link itself stays.
function setJindongAnnotation(node: SceneNode, next: AnnotationLike | null): boolean {
  if (!("annotations" in node)) return false;
  try {
    node.annotations = mergeJindongAnnotation(node.annotations, next) as ReadonlyArray<Annotation>;
    return true;
  } catch {
    return false;
  }
}

async function hapticsCategoryId(): Promise<string | undefined> {
  try {
    const categories = await figma.annotations.getAnnotationCategoriesAsync();
    const existing = categories.find((c) => c.label === ANNOTATION_CATEGORY.label);
    return (existing ?? (await figma.annotations.addAnnotationCategoryAsync(ANNOTATION_CATEGORY))).id;
  } catch {
    return undefined;
  }
}
