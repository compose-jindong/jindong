import { CARD, displayWindowMs, layoutBars } from "../core/cardLayout";
import { NAMESPACE, TONE_HEX, toTimeline, type PatternSpec } from "../core/model";
import { findCardsOnPage, isCard, writePattern } from "./storage";

// The haptic card on the canvas: a plain frame the plugin redraws from its pattern.
// Designers move it around; its locked layers are the plugin's to change. A designer may still turn
// a card into a component, so both count as cards.

export type CardNode = FrameNode | ComponentNode;

const REGULAR: FontName = { family: "Inter", style: "Regular" };
const SEMI_BOLD: FontName = { family: "Inter", style: "Semi Bold" };
const RIGHT_EDGE = CARD.width - 14;
// Marks the layers the plugin draws inside a card.
const PART_KEY = "part";
const CARD_GAP = 16;
const HEADER_TEXT_WIDTH = RIGHT_EDGE - 30;

export async function renderCard(frame: CardNode, p: PatternSpec): Promise<void> {
  await Promise.all([figma.loadFontAsync(REGULAR), figma.loadFontAsync(SEMI_BOLD)]);
  // Only the plugin's own layers are redrawn; anything a designer put inside the card stays. Cards from
  // builds before the marker have unmarked but locked layers, which are the plugin's too.
  const isPart = (child: SceneNode) => child.getSharedPluginData(NAMESPACE, PART_KEY) !== "";
  const unmarked = !frame.children.some(isPart);
  const ours = (child: SceneNode) => isPart(child) || (unmarked && child.locked);
  // A designer's layers above the drawing go back on top after it; ones sent behind it, such as a custom
  // background, stay behind.
  const children = [...frame.children];
  const lowestPart = children.findIndex(ours);
  const onTop = children.filter((child, index) => !ours(child) && index > lowestPart);
  for (const child of children) {
    if (ours(child)) child.remove();
  }

  frame.name = `Haptic / ${p.name}`;
  frame.layoutMode = "NONE";
  frame.resize(CARD.width, CARD.height);
  frame.fills = [solid("#FFFFFF")];
  frame.strokes = [solid("#ECECEC")];
  frame.strokeWeight = 1;
  frame.cornerRadius = CARD.radius;
  frame.clipsContent = true;

  const tone = TONE_HEX[p.tone];
  const timeline = toTimeline(p);
  const windowMs = displayWindowMs(timeline.durationMs);
  const box = CARD.timeline;

  place(frame, ellipse(8, tone), 14, 20);
  place(frame, headerText(p.name, SEMI_BOLD, 13, "#18181B"), 30, 14);
  if (p.description) place(frame, headerText(p.description, REGULAR, 11, "#A1A1AA"), 30, 32);

  place(frame, rect(box.width, box.height, "#FAFAFA"), box.x, box.y);
  place(frame, rect(box.width, 1, "#E4E4E7"), box.x, box.y + box.height - 1);
  for (const bar of layoutBars(timeline, box, windowMs)) {
    if (bar.kind === "continuous") {
      const buzz = rect(bar.width, bar.height, tone);
      buzz.topLeftRadius = 2;
      buzz.topRightRadius = 2;
      place(frame, buzz, bar.x, bar.y);
    } else {
      place(frame, rect(CARD.tapLineWidth, bar.height, tone), bar.x, bar.y);
      const dotX = bar.x + CARD.tapLineWidth / 2 - CARD.tapDot / 2;
      place(frame, ellipse(CARD.tapDot, tone), dotX, bar.y - CARD.tapDot / 2);
    }
  }

  place(frame, text("jindong", SEMI_BOLD, 11, "#52525B"), 14, 152);
  // One length on the card: the haptic's own, including the silence after it. The chart's scale stays unlabelled.
  placeRight(frame, text(`${timeline.durationMs} ms total`, REGULAR, 11, "#A1A1AA"), 152);
  for (const child of onTop) frame.appendChild(child);

  frame.setRelaunchData({ edit: "Edit this haptic in Jindong" });
}

export async function createCard(p: PatternSpec): Promise<FrameNode> {
  const frame = figma.createFrame();
  try {
    await renderCard(frame, p);
  } catch (error) {
    frame.remove();
    throw error;
  }
  writePattern(frame, p);
  const center = figma.viewport.center;
  const y = Math.round(center.y - CARD.height / 2);
  // Step right past earlier cards on this page, including ones inside sections and instances of cards that
  // became components (an instance reads its component's plugin data), so a new card doesn't land on one.
  const page = figma.currentPage;
  const instances = page.children.filter((node) => node.type === "INSTANCE" && isCard(node));
  const taken = [...findCardsOnPage(page), ...instances].flatMap((card) =>
    card.id !== frame.id && card.absoluteBoundingBox ? [card.absoluteBoundingBox] : [],
  );
  let x = Math.round(center.x - CARD.width / 2);
  while (taken.some((box) => overlaps(box, x, y))) x += CARD.width + CARD_GAP;
  frame.x = x;
  frame.y = y;
  figma.currentPage.appendChild(frame);
  figma.currentPage.selection = [frame];
  return frame;
}

export async function updateCard(nodeId: string, p: PatternSpec): Promise<CardNode> {
  const node = await figma.getNodeByIdAsync(nodeId);
  if (!node || (node.type !== "FRAME" && node.type !== "COMPONENT") || !isCard(node)) {
    throw new Error("That card no longer exists.");
  }
  if (node.type === "COMPONENT" && node.remote) {
    throw new Error("This card comes from a library. Edit it in the library's file.");
  }
  await renderCard(node, p);
  writePattern(node, p);
  return node;
}

function overlaps(box: Rect, x: number, y: number): boolean {
  return box.x < x + CARD.width && x < box.x + box.width && box.y < y + CARD.height && y < box.y + box.height;
}

function place(frame: CardNode, node: SceneNode, x: number, y: number): void {
  frame.appendChild(node);
  node.x = x;
  node.y = y;
  node.locked = true;
  node.setSharedPluginData(NAMESPACE, PART_KEY, "1");
}

function placeRight(frame: CardNode, node: TextNode, y: number): void {
  place(frame, node, RIGHT_EDGE - node.width, y);
}

function rect(width: number, height: number, color: string): RectangleNode {
  const node = figma.createRectangle();
  node.resize(width, height);
  node.fills = [solid(color)];
  return node;
}

function ellipse(size: number, color: string): EllipseNode {
  const node = figma.createEllipse();
  node.resize(size, size);
  node.fills = [solid(color)];
  return node;
}

function text(characters: string, font: FontName, size: number, color: string): TextNode {
  const node = figma.createText();
  node.fontName = font;
  node.fontSize = size;
  node.characters = characters;
  node.fills = [solid(color)];
  node.textAutoResize = "WIDTH_AND_HEIGHT";
  return node;
}

// Long names and descriptions end in an ellipsis instead of running off the card.
function headerText(characters: string, font: FontName, size: number, color: string): TextNode {
  const node = text(characters, font, size, color);
  node.textAutoResize = "HEIGHT";
  node.resize(HEADER_TEXT_WIDTH, node.height);
  node.textTruncation = "ENDING";
  node.maxLines = 1;
  return node;
}

function solid(hex: string): SolidPaint {
  const value = parseInt(hex.slice(1), 16);
  return {
    type: "SOLID",
    color: { r: ((value >> 16) & 0xff) / 255, g: ((value >> 8) & 0xff) / 255, b: (value & 0xff) / 255 },
  };
}
