import type { LayerWarning, SelectionState } from "../shared/messages";
import { clashesWith } from "../core/codegen";
import { findCard, findCardsOnPage, isCard, linkFromLibrary, rawLink, readLink, readPattern } from "./storage";

export async function readSelection(): Promise<SelectionState> {
  const selection = figma.currentPage.selection;
  if (selection.length === 0) return { kind: "none" };
  if (selection.length > 1) return { kind: "multiple" };

  const node = selection[0];
  const card = await findCard(node);
  if (card) {
    const pattern = readPattern(card);
    return {
      kind: "card",
      nodeId: card.id,
      pattern,
      nameClash: pattern ? clashesOnPage(card, pattern.name) : false,
      fromLibrary: card.type === "COMPONENT" && card.remote === true,
    };
  }

  const link = readLink(node);
  const fromLibrary = link ? await linkFromLibrary(node) : false;
  const linkedCard = link && !fromLibrary ? await figma.getNodeByIdAsync(link.cardId) : null;
  const linkedPattern = readPattern(linkedCard);
  return {
    kind: "layer",
    nodeId: node.id,
    nodeName: node.name,
    link,
    linkedPattern,
    warning: warningFor(node),
    linkInherited: link ? await inheritsLink(node) : false,
    linkedCardUnreadable: linkedCard !== null && isCard(linkedCard) && linkedPattern === null,
    linkFromLibrary: fromLibrary,
  };
}

// Figma lets an instance read its main component's plugin data. An inherited link is the component's data
// exactly; an instance's own copy records the instance as the layer it was set on. Layers inside an instance
// can't be linked here, so a link on one comes from the component.
async function inheritsLink(node: SceneNode): Promise<boolean> {
  if (insideInstance(node)) return true;
  if (node.type !== "INSTANCE") return false;
  const main = await node.getMainComponentAsync();
  return main !== null && rawLink(main) !== "" && rawLink(main) === rawLink(node);
}

// A link on a layer inside a main component reaches that layer in every instance.
function insideMainComponent(node: BaseNode): boolean {
  for (let parent = node.parent; parent; parent = parent.parent) {
    if (parent.type === "COMPONENT") return true;
  }
  return false;
}

/** True for layers inside an instance; links belong on the instance itself. */
export function insideInstance(node: BaseNode): boolean {
  for (let parent = node.parent; parent; parent = parent.parent) {
    if (parent.type === "INSTANCE") return true;
  }
  return false;
}

// A link on a main component reaches every instance, so the Link panel says so.
function warningFor(node: SceneNode): LayerWarning {
  if (node.type === "COMPONENT") return "main-component";
  // Instances inherit from their variant, never from the set that holds it.
  if (node.type === "COMPONENT_SET") return "component-set";
  if (insideInstance(node)) return "inside-instance";
  return insideMainComponent(node) ? "inside-main-component" : null;
}

// Duplicated cards (⌘D, copy and paste) keep their name and land on the same page.
function clashesOnPage(card: BaseNode, name: string): boolean {
  const others = findCardsOnPage(figma.currentPage)
    .filter((node) => node.id !== card.id)
    .flatMap((node) => readPattern(node)?.name ?? []);
  return clashesWith(name, others);
}
