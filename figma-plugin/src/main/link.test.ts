import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { isJindongAnnotation, type AnnotationLike } from "../core/annotation";
import { presetNamed } from "../core/presets";
import { linkLayer } from "./link";
import { readLink, writePattern } from "./storage";

// A layer whose annotations behave like Figma's when it refuses markdown links in a note.
function fakeLayer(id: string, rejectLinks: boolean) {
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
      if (rejectLinks && next.some((a) => a.labelMarkdown?.includes("]("))) throw new Error("Invalid markdown");
      annotations = next;
    },
  };
}

describe("linkLayer", () => {
  const nodes = new Map<string, unknown>();

  beforeEach(() => {
    nodes.clear();
    const card = fakeLayer("9:1", false);
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
    const layer = fakeLayer("5:1", false);
    nodes.set("5:1", layer);
    expect(await linkLayer("5:1", "9:1", "tap")).toEqual({ annotation: true });
    expect(readLink(layer as unknown as SceneNode)).toEqual({ schema: 1, cardId: "9:1", trigger: "tap" });
    expect(layer.annotations[0]).toEqual({ label: "Spacing note" });
    expect(layer.annotations[1]).toMatchObject({ categoryId: "category:haptics" });
    expect(layer.annotations[1].labelMarkdown).toContain("[Jindong](");
  });

  test("falls back to a plain note that still names Jindong when Figma refuses links", async () => {
    const layer = fakeLayer("5:1", true);
    nodes.set("5:1", layer);
    expect(await linkLayer("5:1", "9:1", "enter")).toEqual({ annotation: true });
    const ours = layer.annotations.filter(isJindongAnnotation);
    expect(ours).toEqual([
      { label: "Jindong haptic: Success · On screen enter — made with Jindong", categoryId: "category:haptics" },
    ]);
    expect(layer.annotations[0]).toEqual({ label: "Spacing note" });
  });
});
