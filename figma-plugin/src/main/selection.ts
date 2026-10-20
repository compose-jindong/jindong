import type { LayerWarning, SelectionState } from "../shared/messages";
import { clashesWith } from "../core/codegen";
import { NAMESPACE, type HapticLink } from "../core/model";
import { findCard, readLink, readPattern } from "./storage";

export async function readSelection(): Promise<SelectionState> {
  const selection = figma.currentPage.selection;
  if (selection.length === 0) return { kind: "none" };
  if (selection.length > 1) return { kind: "multiple" };

  const node = selection[0];
  const card = await findCard(node);
  if (card) {
    const pattern = readPattern(card);
    return { kind: "card", nodeId: card.id, pattern, nameClash: pattern ? clashesOnPage(card, pattern.name) : false };
  }

  const link = readLink(node);
  const linkedPattern = link ? readPattern(await figma.getNodeByIdAsync(link.cardId)) : null;
  return {
    kind: "layer",
    nodeId: node.id,
    nodeName: node.name,
    link,
    linkedPattern,
    warning: warningFor(node),
    linkInherited: link ? await inheritsLink(node, link) : false,
  };
}

// Figma lets an instance read its main component's plugin data, so a link set on the component shows here too.
async function inheritsLink(node: SceneNode, link: HapticLink): Promise<boolean> {
  if (node.type !== "INSTANCE") return false;
  const main = await node.getMainComponentAsync();
  const mainLink = main ? readLink(main) : null;
  return mainLink !== null && mainLink.cardId === link.cardId && mainLink.trigger === link.trigger;
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
  return insideInstance(node) ? "inside-instance" : null;
}

// Duplicated cards (⌘D, copy and paste) keep their name and land on the same page.
function clashesOnPage(card: BaseNode, name: string): boolean {
  const others = figma.currentPage
    .findAllWithCriteria({ types: ["FRAME", "COMPONENT"], sharedPluginData: { namespace: NAMESPACE, keys: ["pattern"] } })
    .filter((node) => node.id !== card.id)
    .flatMap((node) => readPattern(node)?.name ?? []);
  return clashesWith(name, others);
}
