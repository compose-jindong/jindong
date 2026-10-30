import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { BROKEN_LINK_NOTICE, NO_HAPTIC_NOTICE, UNREADABLE_CARD_NOTICE } from "../core/codegen";
import { presetNamed } from "../core/presets";
import { registerCodegen } from "./devmode";
import { writeLink, writePattern } from "./storage";

// A minimal stand-in for the Figma API: nodes keep shared plugin data, and codegen keeps its handler.

type Generate = (event: { node: SceneNode; language: string }) => Promise<CodegenResult[]>;

function fakeNode(id: string): SceneNode {
  const data = new Map<string, string>();
  return {
    id,
    getSharedPluginData: (namespace: string, key: string) => data.get(`${namespace}/${key}`) ?? "",
    setSharedPluginData: (namespace: string, key: string, value: string) => data.set(`${namespace}/${key}`, value),
  } as unknown as SceneNode;
}

describe("Dev Mode codegen", () => {
  const nodes = new Map<string, SceneNode>();
  let generate: Generate;

  beforeEach(() => {
    nodes.clear();
    vi.stubGlobal("figma", {
      codegen: { on: (_: string, handler: Generate) => (generate = handler) },
      getNodeByIdAsync: async (id: string) => nodes.get(id) ?? null,
    });
    registerCodegen();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function card(id: string): SceneNode {
    const node = fakeNode(id);
    writePattern(node, presetNamed("Success"));
    nodes.set(id, node);
    return node;
  }

  test("a card shows its pattern and setup", async () => {
    const results = await generate({ node: card("9:1"), language: "jindong" });
    expect(results.map((r) => [r.title, r.language])).toEqual([
      ["Pattern", "KOTLIN"],
      ["Setup", "KOTLIN"],
    ]);
    expect(results[0].code).toContain("val SuccessHaptic: HapticPattern");
  });

  test("a linked layer shows the Jindong call for its card", async () => {
    card("9:1");
    const button = fakeNode("5:1");
    writeLink(button, { schema: 1, cardId: "9:1", trigger: "enter" });
    const results = await generate({ node: button, language: "jindong" });
    expect(results.map((r) => r.title)).toEqual(["Haptic", "Pattern", "Setup"]);
    expect(results[0].code).toContain("Jindong(Unit) {\n    Clip(SuccessHaptic)\n}");
  });

  test("the tap counter is named after the linked layer", async () => {
    card("9:1");
    const button = fakeNode("5:7");
    (button as unknown as { name: string }).name = "Pay button";
    writeLink(button, { schema: 1, cardId: "9:1", trigger: "tap" });
    const [haptic] = await generate({ node: button, language: "jindong" });
    expect(haptic.code).toContain("var payButtonSuccessTaps by remember");
  });

  test("a link to a deleted card says the link is broken", async () => {
    const button = fakeNode("5:1");
    writeLink(button, { schema: 1, cardId: "9:404", trigger: "tap" });
    expect(await generate({ node: button, language: "jindong" })).toEqual([
      { title: "Jindong", language: "PLAINTEXT", code: BROKEN_LINK_NOTICE },
    ]);
  });

  test("a layer inside a card shows the card's code", async () => {
    const parent = card("9:1");
    const bar = fakeNode("9:2");
    (bar as unknown as { parent: SceneNode }).parent = parent;
    const results = await generate({ node: bar, language: "jindong" });
    expect(results.map((r) => r.title)).toEqual(["Pattern", "Setup"]);
  });

  test("an instance of a card component shows the card's code", async () => {
    const component = card("9:4");
    const instance = fakeNode("9:5") as unknown as Record<string, unknown>;
    instance.type = "INSTANCE";
    instance.getMainComponentAsync = async () => component;
    const results = await generate({ node: instance as unknown as SceneNode, language: "jindong" });
    expect(results.map((r) => r.title)).toEqual(["Pattern", "Setup"]);
  });

  test("a card this plugin version can't read says so instead of claiming there's no haptic", async () => {
    const node = fakeNode("9:3");
    node.setSharedPluginData("jindong", "pattern", JSON.stringify({ ...presetNamed("Success"), schema: 2 }));
    expect(await generate({ node, language: "jindong" })).toEqual([
      { title: "Jindong", language: "PLAINTEXT", code: UNREADABLE_CARD_NOTICE },
    ]);
  });

  test("a plain layer gets the hint", async () => {
    expect(await generate({ node: fakeNode("5:2"), language: "jindong" })).toEqual([
      { title: "Jindong", language: "PLAINTEXT", code: NO_HAPTIC_NOTICE },
    ]);
  });
});
