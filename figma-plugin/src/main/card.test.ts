import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { toTimeline } from "../core/model";
import { presetNamed } from "../core/presets";
import { createCard, updateCard } from "./card";
import { findCards, readPattern, writePattern } from "./storage";

// Just enough of Figma's scene graph for drawing a card: nodes keep children, size, position and plugin data.

type FakeNode = Record<string, any>;

function fakeNode(type: string, id = `${type}:${Math.random()}`): FakeNode {
  const data = new Map<string, string>();
  const node: FakeNode = {
    id,
    type,
    parent: null,
    children: [],
    width: 0,
    height: 0,
    layoutMode: "NONE",
    resize(width: number, height: number) {
      node.width = width;
      node.height = height;
    },
    appendChild(child: FakeNode) {
      child.parent?.children.splice(child.parent.children.indexOf(child), 1);
      node.children.push(child);
      child.parent = node;
    },
    remove() {
      node.parent?.children.splice(node.parent.children.indexOf(node), 1);
      node.parent = null;
    },
    setRelaunchData(relaunch: Record<string, string>) {
      node.relaunch = relaunch;
    },
    getSharedPluginData: (namespace: string, key: string) => data.get(`${namespace}/${key}`) ?? "",
    setSharedPluginData: (namespace: string, key: string, value: string) => data.set(`${namespace}/${key}`, value),
    // Page coordinates: its own position plus its parents', up to the page.
    get absoluteBoundingBox() {
      let x = node.x ?? 0;
      let y = node.y ?? 0;
      for (let p = node.parent; p && p.type !== "PAGE" && p.type !== "DOCUMENT"; p = p.parent) {
        x += p.x ?? 0;
        y += p.y ?? 0;
      }
      return { x, y, width: node.width, height: node.height };
    },
  };
  return node;
}

// Mirrors Figma's documented matching at any depth: one of `types`, and shared plugin data under the keys.
function findAllWithCriteria(under: FakeNode) {
  return ({ types, sharedPluginData }: { types: string[]; sharedPluginData: { namespace: string; keys: string[] } }) => {
    const found: FakeNode[] = [];
    const visit = (node: FakeNode) => {
      for (const child of node.children) {
        const tagged = sharedPluginData.keys.some((key) => child.getSharedPluginData(sharedPluginData.namespace, key) !== "");
        if (types.includes(child.type) && tagged) found.push(child);
        visit(child);
      }
    };
    visit(under);
    return found;
  };
}

function overlapping(a: FakeNode, b: { x: number; y: number; width: number; height: number }): boolean {
  return a.x < b.x + b.width && b.x < a.x + a.width && a.y < b.y + b.height && b.y < a.y + a.height;
}

function fakeText(): FakeNode {
  const node = fakeNode("TEXT");
  Object.defineProperty(node, "characters", {
    get: () => node.text ?? "",
    set: (value: string) => {
      node.text = value;
      node.width = value.length * 6;
      node.height = 14;
    },
  });
  return node;
}

describe("cards a designer has reshaped", () => {
  const nodes = new Map<string, FakeNode>();
  const root = fakeNode("DOCUMENT", "0:0");

  beforeEach(() => {
    nodes.clear();
    root.children = [];
    vi.stubGlobal("figma", {
      loadFontAsync: async () => {},
      loadAllPagesAsync: async () => {},
      createRectangle: () => fakeNode("RECTANGLE"),
      createEllipse: () => fakeNode("ELLIPSE"),
      createText: () => fakeText(),
      getNodeByIdAsync: async (id: string) => nodes.get(id) ?? null,
      root: {
        // Mirrors Figma's documented matching: one of `types`, and shared plugin data under the keys.
        findAllWithCriteria: ({ types, sharedPluginData }: { types: string[]; sharedPluginData: { namespace: string; keys: string[] } }) =>
          root.children.filter(
            (n: FakeNode) =>
              types.includes(n.type) &&
              sharedPluginData.keys.some((key) => n.getSharedPluginData(sharedPluginData.namespace, key) !== ""),
          ),
      },
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function card(type: string, id: string): FakeNode {
    const node = fakeNode(type, id);
    writePattern(node as unknown as SceneNode, presetNamed("Success"));
    nodes.set(id, node);
    root.appendChild(node);
    return node;
  }

  test("a card turned into a component can still be updated", async () => {
    const node = card("COMPONENT", "7:1");
    await updateCard("7:1", { ...presetNamed("Success"), name: "Paid" });
    expect(node.name).toBe("Haptic / Paid");
    expect(readPattern(node as unknown as SceneNode)?.name).toBe("Paid");
  });

  test("a card that's a variant in a component set keeps its variant name", async () => {
    const set = fakeNode("COMPONENT_SET", "7:20");
    root.appendChild(set);
    const variant = card("COMPONENT", "7:21");
    set.appendChild(variant);
    variant.name = "Type=Success";
    await updateCard("7:21", presetNamed("Heartbeat"));
    expect(variant.name).toBe("Type=Success");
  });

  test("a card given auto layout is redrawn with absolute positions", async () => {
    const node = card("FRAME", "7:2");
    node.layoutMode = "VERTICAL";
    await updateCard("7:2", presetNamed("Success"));
    expect(node.layoutMode).toBe("NONE");
    expect(node.children.length).toBeGreaterThan(0);
  });

  test("the Link panel lists cards that became components", async () => {
    card("FRAME", "7:3");
    card("COMPONENT", "7:4");
    fakeNode("FRAME", "7:5");
    expect((await findCards()).map((n) => n.id)).toEqual(["7:3", "7:4"]);
  });

  test("redrawing keeps layers a designer added inside the card", async () => {
    const node = card("FRAME", "7:7");
    await updateCard("7:7", presetNamed("Success"));
    const drawn = node.children.length;
    const note = fakeText();
    note.characters = "Ask Jin about this one";
    node.appendChild(note);
    await updateCard("7:7", presetNamed("Heartbeat"));
    expect(node.children).toContain(note);
    expect(node.children.length).toBe(drawn + 2);
  });

  test("the card shows the haptic's length once, as its total", async () => {
    const node = card("FRAME", "7:13");
    const rampUp = presetNamed("Ramp Up");
    await updateCard("7:13", rampUp);
    const texts = node.children.filter((child: FakeNode) => child.type === "TEXT").map((child: FakeNode) => child.characters);
    const lengths = texts.filter((text: string) => /\d ms/.test(text) || text === "0");
    expect(lengths).toEqual([`${toTimeline(rampUp).durationMs} ms total`]);
  });

  test("a designer's layer sent behind the card's drawing stays behind it", async () => {
    const node = card("FRAME", "7:12");
    await updateCard("7:12", presetNamed("Success"));
    const backdrop = fakeNode("RECTANGLE");
    node.appendChild(backdrop);
    node.children.unshift(node.children.pop());
    const note = fakeText();
    node.appendChild(note);
    await updateCard("7:12", presetNamed("Heartbeat"));
    expect(node.children[0]).toBe(backdrop);
    expect(node.children.at(-1)).toBe(note);
  });

  test("a designer's layers stay above the redrawn card, in their order", async () => {
    const node = card("FRAME", "7:11");
    await updateCard("7:11", presetNamed("Success"));
    const sticker = fakeNode("RECTANGLE");
    const note = fakeText();
    node.appendChild(sticker);
    node.appendChild(note);
    await updateCard("7:11", presetNamed("Heartbeat"));
    expect(node.children.slice(-2)).toEqual([sticker, note]);
  });

  test("a card drawn by an earlier build is redrawn once, not on top of itself", async () => {
    const fresh = card("FRAME", "7:9");
    await updateCard("7:9", presetNamed("Success"));
    const drawn = fresh.children.length;

    // Earlier builds locked their layers but didn't mark them.
    const legacy = card("FRAME", "7:10");
    for (let i = 0; i < drawn; i++) {
      const layer = fakeNode("RECTANGLE");
      layer.locked = true;
      legacy.appendChild(layer);
    }
    await updateCard("7:10", presetNamed("Success"));
    expect(legacy.children.length).toBe(drawn);
  });

  function stubPage(): FakeNode {
    const page = fakeNode("PAGE", "0:1");
    page.selection = [];
    page.findAllWithCriteria = findAllWithCriteria(page);
    vi.stubGlobal("figma", {
      ...(globalThis as unknown as { figma: object }).figma,
      createFrame: () => fakeNode("FRAME"),
      viewport: { center: { x: 500, y: 300 } },
      currentPage: page,
    });
    return page;
  }

  test("a new card is placed beside an earlier one instead of on top of it", async () => {
    const page = stubPage();
    const first = await createCard(presetNamed("Success"));
    const second = await createCard(presetNamed("Error"));
    expect(overlapping(second, first)).toBe(false);
    expect(page.children).toEqual([first, second]);
  });

  test("creating a card leaves the selection alone, so the plugin goes back to the presets", async () => {
    const page = stubPage();
    await createCard(presetNamed("Success"));
    expect(page.selection).toEqual([]);
  });

  test("a new card steps past an instance of a card component", async () => {
    const page = stubPage();
    // An instance reads its main component's plugin data, so it looks like a card.
    const instance = fakeNode("INSTANCE");
    writePattern(instance as unknown as SceneNode, presetNamed("Success"));
    instance.resize(280, 178);
    instance.x = 360;
    instance.y = 211;
    page.appendChild(instance);
    const created = await createCard(presetNamed("Error"));
    expect(overlapping(created, instance.absoluteBoundingBox)).toBe(false);
  });

  test("a new card steps past cards inside sections too", async () => {
    const page = stubPage();
    const section = fakeNode("SECTION");
    section.x = 300;
    section.y = 100;
    section.resize(1000, 600);
    page.appendChild(section);
    const inside = fakeNode("FRAME");
    writePattern(inside as unknown as SceneNode, presetNamed("Success"));
    inside.resize(280, 178);
    // At (360, 211) on the page: right where a card centred on the viewport would go.
    inside.x = 60;
    inside.y = 111;
    section.appendChild(inside);
    const created = await createCard(presetNamed("Error"));
    expect(overlapping(created, inside.absoluteBoundingBox)).toBe(false);
  });

  test("a card from a library file says where to edit it", async () => {
    const node = card("COMPONENT", "7:8");
    node.remote = true;
    await expect(updateCard("7:8", presetNamed("Success"))).rejects.toThrow(
      "This card comes from a library. Edit it in the library's file.",
    );
  });

  test("updating something that is not a card still says the card is gone", async () => {
    nodes.set("7:6", fakeNode("RECTANGLE", "7:6"));
    await expect(updateCard("7:6", presetNamed("Success"))).rejects.toThrow("That card no longer exists.");
  });
});
