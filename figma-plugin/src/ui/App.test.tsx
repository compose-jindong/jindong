// @vitest-environment jsdom
import { act, cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, test } from "vitest";
import { presetNamed } from "../core/presets";
import type { MainToUi, SelectionState, UiToMain } from "../shared/messages";
import { App } from "./App";
import type { Bridge } from "./bridge";

afterEach(cleanup);

function layer(
  overrides: Partial<Extract<SelectionState, { kind: "layer" }>> = {},
): Extract<SelectionState, { kind: "layer" }> {
  return { kind: "layer", nodeId: "5:1", nodeName: "Pay button", link: null, linkedPattern: null, warning: null, ...overrides };
}

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

  test("a layer lists cards and links the chosen card with the chosen trigger", () => {
    const { sent, emit } = setup();
    emit({ type: "selection", state: layer() });
    expect(sent).toContainEqual({ type: "list-cards" });
    expect(screen.getByRole("button", { name: "Link haptic" })).toHaveProperty("disabled", true);
    emit({ type: "cards", cards: [{ id: "9:1", name: "Success", tone: "green", pageName: "Haptics" }] });
    fireEvent.click(screen.getByRole("radio", { name: /Success/ }));
    fireEvent.click(screen.getByRole("button", { name: "On screen enter" }));
    fireEvent.click(screen.getByRole("button", { name: "Link haptic" }));
    expect(sent).toContainEqual({ type: "link", nodeId: "5:1", cardId: "9:1", trigger: "enter" });
  });

  test("an empty file explains how to make a card", () => {
    const { emit } = setup();
    emit({ type: "selection", state: layer() });
    emit({ type: "cards", cards: [] });
    expect(screen.getByText("No haptic cards in this file yet. Clear the selection to make one.")).toBeTruthy();
  });

  test("a linked layer shows its link and preselects it", () => {
    const { emit } = setup();
    emit({
      type: "selection",
      state: layer({ link: { schema: 1, cardId: "9:1", trigger: "state" }, linkedPattern: presetNamed("Success") }),
    });
    emit({ type: "cards", cards: [{ id: "9:1", name: "Success", tone: "green", pageName: "Haptics" }] });
    expect(screen.getByText("Linked to Success · On state change")).toBeTruthy();
    expect(screen.getByRole("radio", { name: /Success/ }).getAttribute("aria-checked")).toBe("true");
    expect(screen.getByRole("button", { name: "On state change" }).getAttribute("aria-pressed")).toBe("true");
  });

  test("a deleted linked card shows a notice and Remove link unlinks", () => {
    const { sent, emit } = setup();
    emit({ type: "selection", state: layer({ link: { schema: 1, cardId: "9:9", trigger: "tap" } }) });
    expect(screen.getByText("The linked card was deleted. Link another card or remove the link.")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Remove link" }));
    expect(sent).toContainEqual({ type: "unlink", nodeId: "5:1" });
  });

  test("a layer inside an instance shows guidance and no Link button", () => {
    const { emit } = setup();
    emit({ type: "selection", state: layer({ warning: "inside-instance" }) });
    expect(screen.getByText("This layer is inside an instance. Select the instance itself to link a haptic.")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Link haptic" })).toBeNull();
  });

  test("a main component warns that instances won't follow", () => {
    const { emit } = setup();
    emit({ type: "selection", state: layer({ warning: "main-component" }) });
    expect(
      screen.getByText(
        "This is a main component. Its instances won't get this link — link the instance on your screen instead.",
      ),
    ).toBeTruthy();
    expect(screen.getByRole("button", { name: "Link haptic" })).toBeTruthy();
  });

  test("a card's Code tab shows the pattern and setup", () => {
    const { emit } = setup();
    emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: presetNamed("Success") } });
    fireEvent.click(screen.getByRole("tab", { name: "Code" }));
    expect(screen.getByText(/val SuccessHaptic: HapticPattern/)).toBeTruthy();
    expect(screen.getByText(/jindong-compose:2\.0\.0/)).toBeTruthy();
  });

  test("a linked layer's Code tab shows the Jindong call", () => {
    const { emit } = setup();
    emit({
      type: "selection",
      state: layer({ link: { schema: 1, cardId: "9:1", trigger: "tap" }, linkedPattern: presetNamed("Success") }),
    });
    fireEvent.click(screen.getByRole("tab", { name: "Code" }));
    expect(screen.getByText(/Clip\(SuccessHaptic\)/)).toBeTruthy();
  });

  test("an unlinked layer has no Code tab", () => {
    const { emit } = setup();
    emit({ type: "selection", state: layer() });
    expect(screen.queryByRole("tab", { name: "Code" })).toBeNull();
  });

  test("status messages from the main thread are shown", () => {
    const { emit } = setup();
    emit({ type: "selection", state: { kind: "none" } });
    emit({ type: "status", tone: "success", message: "Card created." });
    expect(screen.getByRole("status").textContent).toBe("Card created.");
  });
});
