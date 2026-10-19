import { ANNOTATION_CATEGORY, annotationMarkdown, mergeJindongAnnotation, type AnnotationLike } from "../core/annotation";
import { SCHEMA_VERSION, type Trigger } from "../core/model";
import { insideInstance } from "./selection";
import { clearLink, readPattern, writeLink } from "./storage";

// A link points a UI layer at a card. The Dev Mode note is written once, when linking.

export async function linkLayer(nodeId: string, cardId: string, trigger: Trigger): Promise<{ annotation: boolean }> {
  const node = await sceneNode(nodeId);
  const pattern = readPattern(await figma.getNodeByIdAsync(cardId));
  if (!pattern) throw new Error("That card no longer exists.");
  if (insideInstance(node)) throw new Error("Select the instance itself, not a layer inside it.");

  writeLink(node, { schema: SCHEMA_VERSION, cardId, trigger });
  const categoryId = await hapticsCategoryId();
  const note: AnnotationLike = { labelMarkdown: annotationMarkdown(pattern.name, trigger) };
  if (categoryId) note.categoryId = categoryId;
  return { annotation: setJindongAnnotation(node, note) };
}

export async function unlinkLayer(nodeId: string): Promise<void> {
  const node = await sceneNode(nodeId);
  clearLink(node);
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
