import { afterEach, expect, test, vi } from "vitest";

afterEach(() => {
  vi.unstubAllGlobals();
  vi.resetModules();
});

test("a failed card search still answers with a list, so the Link panel doesn't wait forever", async () => {
  const posted: unknown[] = [];
  const ui: { postMessage: (message: unknown) => void; onmessage: ((message: unknown) => void) | null } = {
    postMessage: (message) => posted.push(message),
    onmessage: null,
  };
  vi.stubGlobal("__html__", "<html></html>");
  vi.stubGlobal("figma", {
    editorType: "figma",
    mode: "default",
    showUI: () => {},
    on: () => {},
    ui,
    currentPage: { selection: [], on: () => {}, off: () => {} },
    loadAllPagesAsync: async () => {
      throw new Error("Couldn't load every page.");
    },
  });

  await import("./main");
  ui.onmessage?.({ type: "list-cards" });
  await new Promise((resolve) => setTimeout(resolve, 0));

  expect(posted).toEqual([
    { type: "cards", cards: [] },
    { type: "status", tone: "error", message: "Couldn't load every page." },
  ]);
});

test("a slow read of an earlier selection never overwrites the current one", async () => {
  const handlers = new Map<string, () => void>();
  const posted: { type: string; state?: { nodeId?: string } }[] = [];
  const data = (link: string) => (namespace: string, key: string) => (key === "link" ? link : "");
  // A is linked, so reading it waits on the card lookup; B is a plain layer and reads at once.
  const a = { id: "1:1", type: "FRAME", name: "A", parent: null, getSharedPluginData: data('{"schema":1,"cardId":"9:9","trigger":"tap"}') };
  const b = { id: "1:2", type: "FRAME", name: "B", parent: null, getSharedPluginData: data("") };
  const page = { selection: [a] as unknown[], on: () => {}, off: () => {} };
  let releaseCard: (node: null) => void = () => {};
  vi.stubGlobal("__html__", "<html></html>");
  vi.stubGlobal("figma", {
    editorType: "figma",
    mode: "default",
    showUI: () => {},
    on: (event: string, handler: () => void) => handlers.set(event, handler),
    ui: { postMessage: (message: { type: string }) => posted.push(message), onmessage: null },
    currentPage: page,
    getNodeByIdAsync: () => new Promise((resolve) => (releaseCard = resolve)),
  });

  await import("./main");
  handlers.get("selectionchange")?.();
  page.selection = [b];
  handlers.get("selectionchange")?.();
  await new Promise((resolve) => setTimeout(resolve, 0));
  releaseCard(null);
  await new Promise((resolve) => setTimeout(resolve, 0));

  const selections = posted.filter((m) => m.type === "selection").map((m) => m.state?.nodeId);
  expect(selections[selections.length - 1]).toBe("1:2");
  expect(selections).not.toContain("1:1");
});

test("a change to the selected card (undo, a teammate) re-sends the selection; other changes don't", async () => {
  const posted: { type: string }[] = [];
  const pageHandlers = new Map<string, (event: unknown) => void>();
  const ui: { postMessage: (message: { type: string }) => void; onmessage: ((message: unknown) => void) | null } = {
    postMessage: (message) => posted.push(message),
    onmessage: null,
  };
  const card = { id: "7:1", type: "FRAME", name: "Haptic / Success", parent: null, getSharedPluginData: () => "" };
  vi.stubGlobal("__html__", "<html></html>");
  vi.stubGlobal("figma", {
    editorType: "figma",
    mode: "default",
    showUI: () => {},
    on: () => {},
    ui,
    currentPage: {
      selection: [card],
      on: (event: string, handler: (event: unknown) => void) => pageHandlers.set(event, handler),
      off: () => {},
    },
  });

  await import("./main");
  ui.onmessage?.({ type: "ready" });
  await new Promise((resolve) => setTimeout(resolve, 0));
  pageHandlers.get("nodechange")?.({ nodeChanges: [{ type: "PROPERTY_CHANGE", id: "9:9", properties: ["x"] }] });
  await new Promise((resolve) => setTimeout(resolve, 0));
  expect(posted.filter((m) => m.type === "selection")).toHaveLength(1);
  pageHandlers.get("nodechange")?.({ nodeChanges: [{ type: "PROPERTY_CHANGE", id: "7:1", properties: ["pluginData"] }] });
  await new Promise((resolve) => setTimeout(resolve, 0));
  expect(posted.filter((m) => m.type === "selection")).toHaveLength(2);
});

test("requests are handled one at a time, in order", async () => {
  const cleared: string[] = [];
  const pending = new Map<string, (node: unknown) => void>();
  const ui: { postMessage: () => void; onmessage: ((message: unknown) => void) | null } = {
    postMessage: () => {},
    onmessage: null,
  };
  const layer = (id: string) => ({
    id,
    type: "GROUP",
    parent: null,
    getSharedPluginData: () => "",
    setSharedPluginData: () => cleared.push(id),
  });
  vi.stubGlobal("__html__", "<html></html>");
  vi.stubGlobal("figma", {
    editorType: "figma",
    mode: "default",
    showUI: () => {},
    on: () => {},
    ui,
    currentPage: { selection: [], on: () => {}, off: () => {} },
    getNodeByIdAsync: (id: string) => new Promise((resolve) => pending.set(id, resolve)),
    commitUndo: () => {},
  });

  await import("./main");
  ui.onmessage?.({ type: "unlink", nodeId: "5:1" });
  ui.onmessage?.({ type: "unlink", nodeId: "5:2" });
  await new Promise((resolve) => setTimeout(resolve, 0));
  // The second request must not start while the first is still waiting.
  expect([...pending.keys()]).toEqual(["5:1"]);
  pending.get("5:1")?.(layer("5:1"));
  await new Promise((resolve) => setTimeout(resolve, 0));
  pending.get("5:2")?.(layer("5:2"));
  await new Promise((resolve) => setTimeout(resolve, 0));
  expect(cleared).toEqual(["5:1", "5:2"]);
});

test("each finished action is its own undo step", async () => {
  const posted: { type: string }[] = [];
  const commits: number[] = [];
  const ui: { postMessage: (message: { type: string }) => void; onmessage: ((message: unknown) => void) | null } = {
    postMessage: (message) => posted.push(message),
    onmessage: null,
  };
  const layer = { id: "5:1", type: "GROUP", parent: null, getSharedPluginData: () => "", setSharedPluginData: () => {} };
  vi.stubGlobal("__html__", "<html></html>");
  vi.stubGlobal("figma", {
    editorType: "figma",
    mode: "default",
    showUI: () => {},
    on: () => {},
    ui,
    currentPage: { selection: [], on: () => {}, off: () => {} },
    getNodeByIdAsync: async () => layer,
    commitUndo: () => commits.push(posted.length),
  });

  await import("./main");
  ui.onmessage?.({ type: "unlink", nodeId: "5:1" });
  await new Promise((resolve) => setTimeout(resolve, 0));

  expect(posted.map((m) => m.type)).toEqual(["selection", "status"]);
  expect(commits).toEqual([2]);
});

test("a failing selection read is reported to the window instead of escaping", async () => {
  const handlers = new Map<string, () => void>();
  const posted: unknown[] = [];
  vi.stubGlobal("__html__", "<html></html>");
  vi.stubGlobal("figma", {
    editorType: "figma",
    mode: "default",
    showUI: () => {},
    on: (event: string, handler: () => void) => handlers.set(event, handler),
    ui: { postMessage: (message: unknown) => posted.push(message), onmessage: null },
    currentPage: {
      get selection(): never {
        throw new Error("The page isn't loaded yet.");
      },
      on: () => {},
      off: () => {},
    },
  });

  await import("./main");
  handlers.get("selectionchange")?.();
  handlers.get("currentpagechange")?.();
  await new Promise((resolve) => setTimeout(resolve, 0));

  expect(posted).toEqual([
    { type: "status", tone: "error", message: "The page isn't loaded yet." },
    { type: "status", tone: "error", message: "The page isn't loaded yet." },
  ]);
});
