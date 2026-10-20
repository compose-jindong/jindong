import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { isJindongAnnotation, type AnnotationLike } from "../core/annotation";
import { presetNamed } from "../core/presets";
import { linkLayer, linkNoteStatus, unlinkLayer } from "./link";
import { readLink, writeLink, writePattern } from "./storage";

// A layer whose annotations behave like Figma's when it refuses markdown links, or every note.
function fakeLayer(id: string, reject: "nothing" | "links" | "everything" = "nothing") {
  const data = new Map<string, string>();
  let annotations: AnnotationLike[] = [{ label: "Spacing note" }];
  return {
    id,
    type: "INSTANCE",
    parent: null,
    getSharedPluginData: (namespace: string, key: string) => data.get(`${namespace}/${key}`) ?? "",
    setSharedPluginData: (namespace: string, key: string, value: string) => data.set(`${namespace}/${key}`, value),
    get annotations() {
      return annotations;
    },
    set annotations(next: AnnotationLike[]) {
      if (reject === "everything") throw new Error("Annotations are not available");
      if (reject === "links" && next.some((a) => a.labelMarkdown?.includes("]("))) throw new Error("Invalid markdown");
      annotations = next;
    },
  };
}

// Groups and boolean operations have no annotations in Figma's API.
function fakeGroup(id: string) {
  const data = new Map<string, string>();
  return {
    id,
    type: "GROUP",
    parent: null,
    getSharedPluginData: (namespace: string, key: string) => data.get(`${namespace}/${key}`) ?? "",
    setSharedPluginData: (namespace: string, key: string, value: string) => data.set(`${namespace}/${key}`, value),
  };
}

describe("linkLayer", () => {
  const nodes = new Map<string, unknown>();

  beforeEach(() => {
    nodes.clear();
    const card = fakeLayer("9:1");
    writePattern(card as unknown as SceneNode, presetNamed("Success"));
    nodes.set("9:1", card);
    vi.stubGlobal("figma", {
      getNodeByIdAsync: async (id: string) => nodes.get(id) ?? null,
      annotations: {
        getAnnotationCategoriesAsync: async () => [],
        addAnnotationCategoryAsync: async () => ({ id: "category:haptics" }),
      },
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  test("writes the link and a Dev Mode note next to the layer's other notes", async () => {
    const layer = fakeLayer("5:1");
    nodes.set("5:1", layer);
    expect((await linkLayer("5:1", "9:1", "tap")).annotation).toBe("added");
    expect(readLink(layer as unknown as SceneNode)).toEqual({ schema: 1, cardId: "9:1", trigger: "tap", cardName: "Success" });
    expect(layer.annotations[0]).toEqual({ label: "Spacing note" });
    expect(layer.annotations[1]).toMatchObject({ categoryId: "category:haptics" });
    expect(layer.annotations[1].labelMarkdown).toContain("[Jindong](");
  });

  test("falls back to a plain note that still names Jindong when Figma refuses links", async () => {
    const layer = fakeLayer("5:1", "links");
    nodes.set("5:1", layer);
    expect((await linkLayer("5:1", "9:1", "enter")).annotation).toBe("added");
    const ours = layer.annotations.filter(isJindongAnnotation);
    expect(ours).toEqual([
      { label: "Jindong haptic: Success · On screen enter — made with Jindong", categoryId: "category:haptics" },
    ]);
    expect(layer.annotations[0]).toEqual({ label: "Spacing note" });
  });

  test("a group is linked and reported as unable to hold a note", async () => {
    const group = fakeGroup("5:2");
    nodes.set("5:2", group);
    expect((await linkLayer("5:2", "9:1", "tap")).annotation).toBe("unsupported");
    expect(readLink(group as unknown as SceneNode)).toEqual({ schema: 1, cardId: "9:1", trigger: "tap", cardName: "Success" });
  });

  test("a layer that refuses every note is reported as rejected", async () => {
    const layer = fakeLayer("5:3", "everything");
    nodes.set("5:3", layer);
    expect((await linkLayer("5:3", "9:1", "tap")).annotation).toBe("rejected");
    expect(layer.annotations).toEqual([{ label: "Spacing note" }]);
  });
});

describe("unlinkLayer", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  test("a link an instance inherits from its main component can't be removed on the instance", async () => {
    const main = fakeLayer("3:1");
    writeLink(main as unknown as SceneNode, { schema: 1, cardId: "9:1", trigger: "tap" });
    // Like Figma: the instance reads the main component's value unless it has its own.
    const own = new Map<string, string>();
    const instance = {
      ...fakeLayer("3:2"),
      getSharedPluginData: (namespace: string, key: string) =>
        own.get(`${namespace}/${key}`) || main.getSharedPluginData(namespace, key),
      setSharedPluginData: (namespace: string, key: string, value: string) => own.set(`${namespace}/${key}`, value),
    };
    vi.stubGlobal("figma", { getNodeByIdAsync: async (id: string) => (id === "3:2" ? instance : null) });
    await expect(unlinkLayer("3:2")).rejects.toThrow(
      "This link comes from the main component, so every instance has it. Remove it on the main component.",
    );
  });
});

describe("linkNoteStatus", () => {
  test("says plainly when the layer can't carry a note", () => {
    expect(linkNoteStatus("added", "INSTANCE")).toEqual({
      tone: "success",
      message: "Linked. Developers will see it in Dev Mode.",
    });
    expect(linkNoteStatus("unsupported", "BOOLEAN_OPERATION")).toEqual({
      tone: "warning",
      message:
        "Linked. Figma can't put a Dev Mode note on a boolean operation, so only developers using this plugin will see it. Link a frame or instance to show a note.",
    });
    expect(linkNoteStatus("rejected", "FRAME")).toEqual({
      tone: "warning",
      message: "Linked, but Figma did not accept the Dev Mode note.",
    });
  });
});
