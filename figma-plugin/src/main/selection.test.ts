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

// Like Figma, an instance reads its main component's shared plugin data unless it has its own value.
function instanceOf(id: string, main: FakeNode, parent: FakeNode | null): FakeNode {
  const own = new Map<string, string>();
  const read = main.getSharedPluginData as (namespace: string, key: string) => string;
  return {
    id,
    type: "INSTANCE",
    name: `INSTANCE ${id}`,
    parent,
    getSharedPluginData: (namespace: string, key: string) => own.get(`${namespace}/${key}`) || read(namespace, key),
    setSharedPluginData: (namespace: string, key: string, value: string) => own.set(`${namespace}/${key}`, value),
    getMainComponentAsync: async () => main,
  };
}

function select(...nodes: FakeNode[]) {
  vi.stubGlobal("figma", {
    currentPage: { selection: nodes, findAllWithCriteria: () => [] },
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
    expect(await readSelection()).toEqual({
      kind: "card",
      nodeId: "7:1",
      pattern: presetNamed("Success"),
      nameClash: false,
      fromLibrary: false,
    });
  });

  test("an instance of a card that became a component opens that card, not the instance", async () => {
    const component = fakeNode("7:3", "COMPONENT", page);
    writePattern(component as unknown as SceneNode, presetNamed("Heartbeat"));
    const instance = instanceOf("8:1", component, page);
    select(instance);
    expect(await readSelection()).toEqual({
      kind: "card",
      nodeId: "7:3",
      pattern: presetNamed("Heartbeat"),
      nameClash: false,
      fromLibrary: false,
    });
  });

  test("a card from a team library is marked, since only the library's file can change it", async () => {
    const libraryCard = fakeNode("7:30", "COMPONENT");
    libraryCard.remote = true;
    writePattern(libraryCard as unknown as SceneNode, presetNamed("Success"));
    select(instanceOf("8:30", libraryCard, page));
    expect(await readSelection()).toMatchObject({ kind: "card", nodeId: "7:30", fromLibrary: true });
  });

  test("a card duplicated with the same name is flagged, so the code clash is caught early", async () => {
    const original = fakeNode("7:5", "FRAME", page);
    writePattern(original as unknown as SceneNode, presetNamed("Success"));
    const copy = fakeNode("7:6", "FRAME", page);
    writePattern(copy as unknown as SceneNode, presetNamed("Success"));
    select(copy);
    (figma.currentPage as unknown as { findAllWithCriteria: () => FakeNode[] }).findAllWithCriteria = () => [original, copy];
    expect(await readSelection()).toMatchObject({ kind: "card", nodeId: "7:6", nameClash: true });
  });

  test("a component set is told apart from a component, since its instances inherit from variants", async () => {
    select(fakeNode("6:1", "COMPONENT_SET", page));
    expect(await readSelection()).toMatchObject({ kind: "layer", warning: "component-set" });
    select(fakeNode("6:2", "COMPONENT", page));
    expect(await readSelection()).toMatchObject({ kind: "layer", warning: "main-component" });
  });

  test("an instance whose link comes from its main component says so", async () => {
    const main = fakeNode("6:3", "COMPONENT", page);
    writeLink(main as unknown as SceneNode, { schema: 1, cardId: "9:9", trigger: "tap" });
    select(instanceOf("6:4", main, page));
    expect(await readSelection()).toMatchObject({ kind: "layer", nodeId: "6:4", linkInherited: true });

    const own = instanceOf("6:5", main, page);
    writeLink(own as unknown as SceneNode, { schema: 1, cardId: "9:8", trigger: "enter" });
    select(own);
    expect(await readSelection()).toMatchObject({ kind: "layer", nodeId: "6:5", linkInherited: false });
  });

  test("an instance's own copy of its main component's link is its own, not inherited", async () => {
    const main = fakeNode("6:6", "COMPONENT", page);
    writeLink(main as unknown as SceneNode, { schema: 1, cardId: "9:9", trigger: "tap", layerId: "6:6" });
    const copy = instanceOf("6:7", main, page);
    writeLink(copy as unknown as SceneNode, { schema: 1, cardId: "9:9", trigger: "tap", layerId: "6:7" });
    select(copy);
    expect(await readSelection()).toMatchObject({ kind: "layer", nodeId: "6:7", linkInherited: false });
  });

  test("a link to a card from a newer plugin version is told apart from a deleted card", async () => {
    const newer = fakeNode("9:7", "FRAME", page);
    (newer as unknown as SceneNode).setSharedPluginData("jindong", "pattern", JSON.stringify({ ...presetNamed("Success"), schema: 2 }));
    const button = fakeNode("6:10", "FRAME", page);
    writeLink(button as unknown as SceneNode, { schema: 1, cardId: "9:7", trigger: "tap" });
    select(button);
    (figma as unknown as { getNodeByIdAsync: (id: string) => Promise<FakeNode | null> }).getNodeByIdAsync = async (id) =>
      id === "9:7" ? newer : null;
    expect(await readSelection()).toMatchObject({ kind: "layer", linkedPattern: null, linkedCardUnreadable: true });

    writeLink(button as unknown as SceneNode, { schema: 1, cardId: "9:404", trigger: "tap" });
    expect(await readSelection()).toMatchObject({ kind: "layer", linkedPattern: null, linkedCardUnreadable: false });
  });

  test("a link that comes from a library component isn't looked up in this file, where its card id means nothing", async () => {
    const libraryButton = fakeNode("6:20", "COMPONENT");
    libraryButton.remote = true;
    writeLink(libraryButton as unknown as SceneNode, { schema: 1, cardId: "9:1", trigger: "tap", cardName: "Tap" });
    // This file happens to have its own node with that id.
    const unrelated = fakeNode("9:1", "FRAME", page);
    writePattern(unrelated as unknown as SceneNode, presetNamed("Error"));
    select(instanceOf("6:21", libraryButton, page));
    (figma as unknown as { getNodeByIdAsync: (id: string) => Promise<FakeNode | null> }).getNodeByIdAsync = async (id) =>
      id === "9:1" ? unrelated : null;
    expect(await readSelection()).toMatchObject({ kind: "layer", linkFromLibrary: true, linkedPattern: null });
  });

  test("an instance's own link stays local even when its component comes from a library", async () => {
    const libraryButton = fakeNode("6:22", "COMPONENT");
    libraryButton.remote = true;
    const own = instanceOf("6:23", libraryButton, page);
    writeLink(own as unknown as SceneNode, { schema: 1, cardId: "9:2", trigger: "tap" });
    const card = fakeNode("9:2", "FRAME", page);
    writePattern(card as unknown as SceneNode, presetNamed("Success"));
    select(own);
    (figma as unknown as { getNodeByIdAsync: (id: string) => Promise<FakeNode | null> }).getNodeByIdAsync = async (id) =>
      id === "9:2" ? card : null;
    expect(await readSelection()).toMatchObject({ kind: "layer", linkFromLibrary: false, linkedPattern: presetNamed("Success") });
  });

  test("a layer inside a main component warns that every instance gets its link", async () => {
    const component = fakeNode("6:8", "COMPONENT", page);
    select(fakeNode("6:9", "RECTANGLE", component));
    expect(await readSelection()).toMatchObject({ kind: "layer", warning: "inside-main-component" });
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
