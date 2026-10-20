import { afterEach, expect, test, vi } from "vitest";

afterEach(() => {
  vi.unstubAllGlobals();
  vi.resetModules();
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
    get currentPage(): never {
      throw new Error("The page isn't loaded yet.");
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
