import type { LayerWarning, SelectionState } from "../shared/messages";
import { isCard, readLink, readPattern } from "./storage";

export async function readSelection(): Promise<SelectionState> {
  const selection = figma.currentPage.selection;
  if (selection.length === 0) return { kind: "none" };
  if (selection.length > 1) return { kind: "multiple" };

  const node = selection[0];
  if (isCard(node)) return { kind: "card", nodeId: node.id, pattern: readPattern(node) };

  const link = readLink(node);
  const linkedPattern = link ? readPattern(await figma.getNodeByIdAsync(link.cardId)) : null;
  return { kind: "layer", nodeId: node.id, nodeName: node.name, link, linkedPattern, warning: warningFor(node) };
}

/** True for layers inside an instance; links belong on the instance itself. */
export function insideInstance(node: BaseNode): boolean {
  for (let parent = node.parent; parent; parent = parent.parent) {
    if (parent.type === "INSTANCE") return true;
  }
  return false;
}

// A link on a main component doesn't reach its instances, so the Link panel says so.
function warningFor(node: SceneNode): LayerWarning {
  if (node.type === "COMPONENT" || node.type === "COMPONENT_SET") return "main-component";
  return insideInstance(node) ? "inside-instance" : null;
}
