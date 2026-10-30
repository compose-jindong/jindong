import { NAMESPACE, parseLinkJson, parsePatternJson, type HapticLink, type PatternSpec } from "../core/model";

// Haptic data lives in shared plugin data so the Figma REST API can read it later.
// A card frame holds `pattern`; a linked layer holds `link`.

const PATTERN_KEY = "pattern";
const LINK_KEY = "link";

export function readPattern(node: BaseNode | null): PatternSpec | null {
  return node ? parsePatternJson(node.getSharedPluginData(NAMESPACE, PATTERN_KEY)) : null;
}

export function writePattern(node: BaseNode, p: PatternSpec): void {
  node.setSharedPluginData(NAMESPACE, PATTERN_KEY, JSON.stringify(p));
}

/** True for any card frame, including one whose data this plugin version can't read. */
export function isCard(node: BaseNode): boolean {
  return node.getSharedPluginData(NAMESPACE, PATTERN_KEY) !== "";
}

/**
 * The card that [node] is or sits inside, so layers picked from a card's layer list belong to the card.
 * An instance of a card that became a component resolves to that component: Figma lets the instance read
 * the component's plugin data, but only the component can be redrawn.
 */
export async function findCard(node: BaseNode): Promise<BaseNode | null> {
  for (let current: BaseNode | null = node; current && current.type !== "PAGE"; current = current.parent) {
    if (current.type === "INSTANCE") {
      const main = await current.getMainComponentAsync();
      if (main && isCard(main)) return main;
    } else if (isCard(current)) {
      return current;
    }
  }
  return null;
}

/** The stored link exactly as Figma returns it; an instance without its own value returns its component's. */
export function rawLink(node: BaseNode): string {
  return node.getSharedPluginData(NAMESPACE, LINK_KEY);
}

export function readLink(node: BaseNode): HapticLink | null {
  return parseLinkJson(node.getSharedPluginData(NAMESPACE, LINK_KEY));
}

export function writeLink(node: BaseNode, link: HapticLink): void {
  node.setSharedPluginData(NAMESPACE, LINK_KEY, JSON.stringify(link));
}

export function clearLink(node: BaseNode): void {
  node.setSharedPluginData(NAMESPACE, LINK_KEY, "");
}

/** Every card in the file, across pages, including cards a designer turned into components. */
export async function findCards(): Promise<(FrameNode | ComponentNode)[]> {
  await figma.loadAllPagesAsync();
  return figma.root.findAllWithCriteria({
    types: ["FRAME", "COMPONENT"],
    sharedPluginData: { namespace: NAMESPACE, keys: [PATTERN_KEY] },
  });
}

export function pageOf(node: BaseNode): PageNode | null {
  let current: BaseNode | null = node;
  while (current && current.type !== "PAGE") current = current.parent;
  return current?.type === "PAGE" ? current : null;
}
