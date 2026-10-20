import { afterEach, describe, expect, test, vi } from "vitest";
import { presetNamed } from "../core/presets";
import { readSelection } from "./selection";
import { writeLink, writePattern } from "./storage";

type FakeNode = { id: string; type: string; name: string; parent: FakeNode | null } & Record<string, unknown>;

function fakeNode(id: string, type: string, parent: FakeNode | null = null): FakeNode {
  const data = new Map<string, string>();
  return {
    id,
    type,
    name: `${type} ${id}`,
    parent,
    getSharedPluginData: (namespace: string, key: string) => data.get(`${namespace}/${key}`) ?? "",
    setSharedPluginData: (namespace: string, key: string, value: string) => data.set(`${namespace}/${key}`, value),
    // Every Figma instance has a main component; these tests' instances point at an ordinary one.
    ...(type === "INSTANCE" ? { getMainComponentAsync: async () => fakeNode(`main-of-${id}`, "COMPONENT") } : {}),
  };
}

function select(...nodes: FakeNode[]) {
  vi.stubGlobal("figma", {
    currentPage: { selection: nodes },
    getNodeByIdAsync: async () => null,
  });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("readSelection", () => {
  const page = fakeNode("0:1", "PAGE");

  test("a layer inside a card opens the card", async () => {
    const card = fakeNode("7:1", "FRAME", page);
    writePattern(card as unknown as SceneNode, presetNamed("Success"));
    const bar = fakeNode("7:2", "RECTANGLE", card);
    select(bar);
    expect(await readSelection()).toEqual({ kind: "card", nodeId: "7:1", pattern: presetNamed("Success") });
  });

  test("an instance of a card that became a component opens that card", async () => {
    const component = fakeNode("7:3", "COMPONENT", page);
    writePattern(component as unknown as SceneNode, presetNamed("Heartbeat"));
    const instance = fakeNode("8:1", "INSTANCE", page);
    instance.getMainComponentAsync = async () => component;
    select(instance);
    expect(await readSelection()).toEqual({ kind: "card", nodeId: "7:3", pattern: presetNamed("Heartbeat") });
  });

  test("an ordinary layer stays a layer", async () => {
    const frame = fakeNode("5:1", "FRAME", page);
    const button = fakeNode("5:2", "INSTANCE", frame);
    select(button);
    expect(await readSelection()).toMatchObject({ kind: "layer", nodeId: "5:2", link: null, warning: null });
  });

  test("a linked layer keeps its link even when its parent is linked too", async () => {
    const frame = fakeNode("5:1", "FRAME", page);
    writeLink(frame as unknown as SceneNode, { schema: 1, cardId: "9:9", trigger: "enter" });
    const button = fakeNode("5:2", "INSTANCE", frame);
    select(button);
    expect(await readSelection()).toMatchObject({ kind: "layer", nodeId: "5:2", link: null });
  });
});
