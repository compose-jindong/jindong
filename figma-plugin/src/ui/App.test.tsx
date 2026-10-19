// @vitest-environment jsdom
import { act, cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, test } from "vitest";
import { presetNamed } from "../core/presets";
import type { MainToUi, UiToMain } from "../shared/messages";
import { App } from "./App";
import type { Bridge } from "./bridge";

afterEach(cleanup);

function setup() {
  const sent: UiToMain[] = [];
  let handler: ((message: MainToUi) => void) | null = null;
  const bridge: Bridge = {
    send: (message) => sent.push(message),
    subscribe: (h) => {
      handler = h;
      return () => {
        handler = null;
      };
    },
  };
  render(<App bridge={bridge} />);
  const emit = (message: MainToUi) => act(() => handler?.(message));
  return { sent, emit };
}

describe("App", () => {
  test("announces it is ready", () => {
    const { sent } = setup();
    expect(sent[0]).toEqual({ type: "ready" });
  });

  test("picking Success from the gallery and creating sends the Success pattern", () => {
    const { sent, emit } = setup();
    emit({ type: "selection", state: { kind: "none" } });
    fireEvent.click(screen.getByRole("button", { name: /^Success/ }));
    fireEvent.click(screen.getByRole("button", { name: "Create card" }));
    expect(sent).toContainEqual({ type: "create-card", pattern: presetNamed("Success") });
  });

  test("editing a selected card sends update-card", () => {
    const { sent, emit } = setup();
    emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: presetNamed("Success") } });
    fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Paid" } });
    fireEvent.click(screen.getByRole("button", { name: "Update card" }));
    expect(sent).toContainEqual({
      type: "update-card",
      nodeId: "1:2",
      pattern: { ...presetNamed("Success"), name: "Paid" },
    });
  });

  test("an invalid draft disables the button and says why", () => {
    const { emit } = setup();
    emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: presetNamed("Success") } });
    fireEvent.change(screen.getByLabelText("Name"), { target: { value: "  " } });
    expect(screen.getByRole("button", { name: "Update card" })).toHaveProperty("disabled", true);
    expect(screen.getByText("Give the haptic a name.")).toBeTruthy();
  });

  test("an unreadable card shows the newer-version message", () => {
    const { emit } = setup();
    emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: null } });
    expect(screen.getByText("This card can't be read. It may come from a newer version of the plugin.")).toBeTruthy();
  });

  test("multiple selection shows the hint", () => {
    const { emit } = setup();
    emit({ type: "selection", state: { kind: "multiple" } });
    expect(
      screen.getByText("Select one layer to link a haptic, or clear the selection to make a new one."),
    ).toBeTruthy();
  });

  test("status messages from the main thread are shown", () => {
    const { emit } = setup();
    emit({ type: "selection", state: { kind: "none" } });
    emit({ type: "status", tone: "success", message: "Card created." });
    expect(screen.getByRole("status").textContent).toBe("Card created.");
  });
});
