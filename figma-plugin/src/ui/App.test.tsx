// @vitest-environment jsdom
import { act, cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, test, vi } from "vitest";
import { presetNamed } from "../core/presets";
import type { MainToUi, SelectionState, UiToMain } from "../shared/messages";
import { App } from "./App";
import type { Bridge } from "./bridge";
import { FakeAudioContext } from "./testing";

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

describe("in Dev Mode, which can't change the file", () => {
  const READ_ONLY = "Dev Mode is read-only. Switch to Design Mode to change haptics.";

  function devSetup() {
    const result = setup();
    result.emit({ type: "editor", readOnly: true });
    return result;
  }

  test("a card shows its haptic, a preview and its code, without editing", () => {
    const { emit } = devSetup();
    emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: presetNamed("Success") } });
    expect(screen.getByRole("heading", { name: "Success" })).toBeTruthy();
    expect(screen.getByText(READ_ONLY)).toBeTruthy();
    expect(screen.getByRole("img", { name: /Success rhythm/ })).toBeTruthy();
    expect(screen.getByRole("button", { name: /Preview rhythm/ })).toBeTruthy();
    expect(screen.getAllByRole("heading", { level: 3 }).map((h) => h.textContent)).toEqual(["Pattern", "Setup"]);
    expect(screen.queryByLabelText("Name")).toBeNull();
    expect(screen.queryByRole("button", { name: "Update card" })).toBeNull();
  });

  test("a linked layer shows what plays and the code for it, without relinking", () => {
    const { emit } = devSetup();
    emit({
      type: "selection",
      state: layer({ link: { schema: 1, cardId: "9:1", trigger: "tap" }, linkedPattern: presetNamed("Success") }),
    });
    expect(screen.getByRole("heading", { name: "Pay button" })).toBeTruthy();
    expect(screen.getByText("Plays Success · On tap")).toBeTruthy();
    expect(screen.getAllByRole("heading", { level: 3 }).map((h) => h.textContent)).toEqual(["Haptic", "Pattern", "Setup"]);
    expect(screen.queryByRole("button", { name: "Link haptic" })).toBeNull();
    expect(screen.queryByRole("button", { name: "Remove link" })).toBeNull();
  });

  test("a broken link explains itself the way the code panel does", () => {
    const { emit } = devSetup();
    emit({ type: "selection", state: layer({ link: { schema: 1, cardId: "9:9", trigger: "tap", cardName: "Success" } }) });
    expect(screen.getByText(/linked to the Jindong haptic card “Success”, which was moved or deleted/)).toBeTruthy();
  });

  test("other selections say what to select", () => {
    const { emit } = devSetup();
    emit({ type: "selection", state: { kind: "none" } });
    expect(screen.getByText("Select a haptic card, or a layer linked to one, to see its haptic and code.")).toBeTruthy();
    expect(screen.queryByRole("button", { name: /^Success/ })).toBeNull();
    emit({ type: "selection", state: layer() });
    expect(screen.getByText("No haptic is linked to this layer.")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Link haptic" })).toBeNull();
  });
});

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

  test("tweaks to a new haptic survive selecting a layer and coming back", () => {
    const { emit } = setup();
    emit({ type: "selection", state: { kind: "none" } });
    fireEvent.click(screen.getByRole("button", { name: /^Success/ }));
    fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Mine" } });
    emit({ type: "selection", state: layer() });
    emit({ type: "selection", state: { kind: "none" } });
    expect((screen.getByLabelText("Name") as HTMLInputElement).value).toBe("Mine");
  });

  test("once the card is created, clearing the selection shows the presets again", () => {
    const { emit } = setup();
    emit({ type: "selection", state: { kind: "none" } });
    fireEvent.click(screen.getByRole("button", { name: /^Success/ }));
    fireEvent.click(screen.getByRole("button", { name: "Create card" }));
    emit({ type: "selection", state: { kind: "card", nodeId: "8:1", pattern: presetNamed("Success") } });
    emit({ type: "status", tone: "success", message: "Card created." });
    emit({ type: "selection", state: { kind: "none" } });
    expect(screen.getByRole("heading", { name: "Jindong" })).toBeTruthy();
  });

  test("a double click creates one card, and the button comes back after the reply", () => {
    const { sent, emit } = setup();
    emit({ type: "selection", state: { kind: "none" } });
    fireEvent.click(screen.getByRole("button", { name: /^Success/ }));
    const create = screen.getByRole("button", { name: "Create card" });
    fireEvent.click(create);
    fireEvent.click(create);
    expect(sent.filter((m) => m.type === "create-card")).toHaveLength(1);
    expect(create).toHaveProperty("disabled", true);
    emit({ type: "status", tone: "error", message: "Font missing." });
    expect(screen.getByRole("button", { name: "Create card" })).toHaveProperty("disabled", false);
  });

  test("a slow request says it's still working instead of re-enabling the button", () => {
    vi.useFakeTimers();
    try {
      const { sent, emit } = setup();
      emit({ type: "selection", state: { kind: "none" } });
      fireEvent.click(screen.getByRole("button", { name: /^Success/ }));
      fireEvent.click(screen.getByRole("button", { name: "Create card" }));
      act(() => vi.advanceTimersByTime(12_000));
      expect(screen.getByRole("status").textContent).toBe("Still working… Large files can take a moment.");
      fireEvent.click(screen.getByRole("button", { name: "Create card" }));
      expect(sent.filter((m) => m.type === "create-card")).toHaveLength(1);
      act(() => vi.advanceTimersByTime(60_000));
      expect(screen.getByRole("button", { name: "Create card" })).toHaveProperty("disabled", false);
      expect(screen.getByRole("status").textContent).toBe("Figma didn't answer. Check the canvas before trying again.");
    } finally {
      vi.useRealTimers();
    }
  });

  test("the still-working message stays until the reply replaces it", () => {
    vi.useFakeTimers();
    try {
      const { emit } = setup();
      emit({ type: "selection", state: { kind: "none" } });
      fireEvent.click(screen.getByRole("button", { name: /^Success/ }));
      fireEvent.click(screen.getByRole("button", { name: "Create card" }));
      // One step at a time, so each timer's effects run before the next one is due.
      for (let second = 0; second < 25; second++) act(() => vi.advanceTimersByTime(1000));
      expect(screen.getByRole("status").textContent).toBe("Still working… Large files can take a moment.");
      emit({ type: "status", tone: "success", message: "Card created." });
      expect(screen.getByRole("status").textContent).toBe("Card created.");
    } finally {
      vi.useRealTimers();
    }
  });

  test("a double click links once", () => {
    const { sent, emit } = setup();
    emit({ type: "selection", state: layer() });
    emit({ type: "cards", cards: [{ id: "9:1", name: "Success", tone: "green", pageName: "Haptics" }] });
    fireEvent.click(screen.getByRole("radio", { name: /Success/ }));
    const link = screen.getByRole("button", { name: "Link haptic" });
    fireEvent.click(link);
    fireEvent.click(link);
    expect(sent.filter((m) => m.type === "link")).toHaveLength(1);
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

  test("events can be stepped through without clicking the timeline", () => {
    const { emit } = setup();
    emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: presetNamed("Success") } });
    expect(screen.getByText("Event 1 of 3")).toBeTruthy();
    expect(screen.getByRole("button", { name: "Previous event" })).toHaveProperty("disabled", true);
    fireEvent.click(screen.getByRole("button", { name: "Next event" }));
    fireEvent.click(screen.getByRole("button", { name: "Next event" }));
    expect(screen.getByText("Event 3 of 3")).toBeTruthy();
    expect(screen.getByRole("button", { name: "Next event" })).toHaveProperty("disabled", true);
    fireEvent.click(screen.getByRole("button", { name: "Previous event" }));
    expect(screen.getByText("Event 2 of 3")).toBeTruthy();
  });

  test("a number box can be emptied and retyped", () => {
    const { sent, emit } = setup();
    emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: presetNamed("Success") } });
    const length = screen.getByLabelText("Length (ms)") as HTMLInputElement;
    fireEvent.change(length, { target: { value: "" } });
    expect(length.value).toBe("");
    fireEvent.change(length, { target: { value: "250" } });
    expect(length.value).toBe("250");
    fireEvent.click(screen.getByRole("button", { name: "Update card" }));
    const update = sent.find((m) => m.type === "update-card");
    expect(update?.type === "update-card" && update.pattern.events[0]).toMatchObject({ durationMs: 250 });
  });

  test("leaving an emptied number box puts the value back", () => {
    const { emit } = setup();
    emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: presetNamed("Success") } });
    const length = screen.getByLabelText("Length (ms)") as HTMLInputElement;
    fireEvent.change(length, { target: { value: "" } });
    fireEvent.blur(length);
    expect(length.value).toBe("50");
  });

  test("a card changed elsewhere (undo, a teammate) shows the new values when nothing is unsaved", () => {
    const { emit } = setup();
    emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: presetNamed("Success") } });
    emit({
      type: "selection",
      state: { kind: "card", nodeId: "1:2", pattern: { ...presetNamed("Success"), name: "Paid" } },
    });
    expect((screen.getByLabelText("Name") as HTMLInputElement).value).toBe("Paid");
    expect(screen.getByRole("button", { name: "Update card" })).toHaveProperty("disabled", true);
  });

  test("unsaved edits are kept when the card changes elsewhere", () => {
    const { emit } = setup();
    emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: presetNamed("Success") } });
    fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Mine" } });
    emit({
      type: "selection",
      state: { kind: "card", nodeId: "1:2", pattern: { ...presetNamed("Success"), name: "Paid" } },
    });
    expect((screen.getByLabelText("Name") as HTMLInputElement).value).toBe("Mine");
  });

  test("a card that shares its name with another says so", () => {
    const { emit } = setup();
    emit({
      type: "selection",
      state: { kind: "card", nodeId: "1:2", pattern: presetNamed("Success"), nameClash: true },
    });
    expect(
      screen.getByText("Another card on this page is also called “Success”. Rename one so their code doesn't clash."),
    ).toBeTruthy();
  });

  test("unsaved edits to a card survive clicking away and back", () => {
    const { emit } = setup();
    const card = { kind: "card", nodeId: "1:2", pattern: presetNamed("Success") } as const;
    emit({ type: "selection", state: card });
    fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Payment done" } });
    emit({ type: "selection", state: { kind: "none" } });
    emit({ type: "selection", state: card });
    expect((screen.getByLabelText("Name") as HTMLInputElement).value).toBe("Payment done");
    expect(screen.getByRole("button", { name: "Update card" })).toHaveProperty("disabled", false);
  });

  test("a saved card starts clean the next time it's opened", () => {
    const { emit } = setup();
    emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: presetNamed("Success") } });
    fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Payment done" } });
    fireEvent.click(screen.getByRole("button", { name: "Update card" }));
    const saved = { ...presetNamed("Success"), name: "Payment done" };
    emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: saved } });
    emit({ type: "status", tone: "success", message: "Card updated." });
    emit({ type: "selection", state: { kind: "none" } });
    emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: { ...saved, name: "Renamed later" } } });
    expect((screen.getByLabelText("Name") as HTMLInputElement).value).toBe("Renamed later");
  });

  test("a change made elsewhere while you edit is announced, and your edits can be dropped", () => {
    const { emit } = setup();
    emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: presetNamed("Success") } });
    fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Mine" } });
    emit({
      type: "selection",
      state: { kind: "card", nodeId: "1:2", pattern: { ...presetNamed("Success"), name: "Teammate" } },
    });
    expect(
      screen.getByText("This card changed in Figma while you were editing. Update card replaces that change with yours."),
    ).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Discard my edits" }));
    expect((screen.getByLabelText("Name") as HTMLInputElement).value).toBe("Teammate");
    expect(screen.queryByRole("button", { name: "Discard my edits" })).toBeNull();
  });

  test("typing on while Update card runs doesn't look like someone else changed the card", () => {
    const { emit } = setup();
    emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: presetNamed("Success") } });
    fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Mine" } });
    fireEvent.click(screen.getByRole("button", { name: "Update card" }));
    fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Mine 2" } });
    emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: { ...presetNamed("Success"), name: "Mine" } } });
    emit({ type: "status", tone: "success", message: "Card updated." });
    expect(screen.queryByText(/This card changed in Figma/)).toBeNull();
    expect((screen.getByLabelText("Name") as HTMLInputElement).value).toBe("Mine 2");
  });

  test("a save that lands while another layer is selected doesn't look like someone else's change", () => {
    const { emit } = setup();
    emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: presetNamed("Success") } });
    fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Mine" } });
    fireEvent.click(screen.getByRole("button", { name: "Update card" }));
    emit({ type: "selection", state: layer() });
    emit({ type: "status", tone: "success", message: "Card updated." });
    emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: { ...presetNamed("Success"), name: "Mine" } } });
    fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Mine again" } });
    expect(screen.queryByText(/This card changed in Figma/)).toBeNull();
  });

  test("Update card waits for a change", () => {
    const { emit } = setup();
    emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: presetNamed("Success") } });
    expect(screen.getByRole("button", { name: "Update card" })).toHaveProperty("disabled", true);
    fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Paid" } });
    expect(screen.getByRole("button", { name: "Update card" })).toHaveProperty("disabled", false);
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

  test("linking keeps the card list instead of searching the file again", () => {
    const { sent, emit } = setup();
    emit({ type: "selection", state: layer() });
    emit({ type: "cards", cards: [{ id: "9:1", name: "Success", tone: "green", pageName: "Haptics" }] });
    emit({
      type: "selection",
      state: layer({ link: { schema: 1, cardId: "9:1", trigger: "tap" }, linkedPattern: presetNamed("Success") }),
    });
    expect(screen.queryByText("Finding cards…")).toBeNull();
    expect(sent.filter((m) => m.type === "list-cards")).toHaveLength(1);
  });

  test("selecting another layer asks for the card list again", () => {
    const { sent, emit } = setup();
    emit({ type: "selection", state: layer() });
    emit({ type: "cards", cards: [] });
    emit({ type: "selection", state: layer({ nodeId: "5:9" }) });
    expect(sent.filter((m) => m.type === "list-cards")).toHaveLength(2);
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

  test("a link inherited from the main component can't be removed here, and the panel says where", () => {
    const { emit } = setup();
    emit({
      type: "selection",
      state: layer({
        link: { schema: 1, cardId: "9:1", trigger: "tap" },
        linkedPattern: presetNamed("Success"),
        linkInherited: true,
      }),
    });
    expect(screen.getByText("Linked to Success · On tap through its main component.")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Remove link" })).toBeNull();
  });

  test("a deleted linked card shows a notice and Remove link unlinks", () => {
    const { sent, emit } = setup();
    emit({ type: "selection", state: layer({ link: { schema: 1, cardId: "9:9", trigger: "tap" } }) });
    expect(screen.getByText("The linked card was deleted. Link another card or remove the link.")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Remove link" }));
    expect(sent).toContainEqual({ type: "unlink", nodeId: "5:1" });
  });

  test("a link to a card from a newer plugin version says so", () => {
    const { emit } = setup();
    emit({
      type: "selection",
      state: layer({ link: { schema: 1, cardId: "9:9", trigger: "tap" }, linkedCardUnreadable: true }),
    });
    expect(screen.getByText("The linked card can't be read. It may come from a newer version of the plugin.")).toBeTruthy();
    expect(screen.getByRole("button", { name: "Remove link" })).toBeTruthy();
  });

  test("a link broken by moving its card offers the card with the same name", () => {
    const { sent, emit } = setup();
    emit({
      type: "selection",
      state: layer({ link: { schema: 1, cardId: "9:9", trigger: "enter", cardName: "Success" } }),
    });
    emit({ type: "cards", cards: [{ id: "9:1", name: "Success", tone: "green", pageName: "Haptics" }] });
    expect(
      screen.getByText("The linked card was moved or deleted. “Success” on Haptics looks like it. Press Link haptic to link it again."),
    ).toBeTruthy();
    expect(screen.getByRole("radio", { name: /Success/ }).getAttribute("aria-checked")).toBe("true");
    fireEvent.click(screen.getByRole("button", { name: "Link haptic" }));
    expect(sent).toContainEqual({ type: "link", nodeId: "5:1", cardId: "9:1", trigger: "enter" });
  });

  test("a layer inside an instance shows guidance and no Link button", () => {
    const { emit } = setup();
    emit({ type: "selection", state: layer({ warning: "inside-instance" }) });
    expect(screen.getByText("This layer is inside an instance. Select the instance itself to link a haptic.")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Link haptic" })).toBeNull();
  });

  test("a layer inside an instance shows the link it gets from its main component", () => {
    const { emit } = setup();
    emit({
      type: "selection",
      state: layer({
        warning: "inside-instance",
        link: { schema: 1, cardId: "9:1", trigger: "tap" },
        linkedPattern: presetNamed("Success"),
      }),
    });
    expect(screen.getByText("Linked to Success · On tap through its main component.")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Link haptic" })).toBeNull();
    expect(screen.queryByRole("button", { name: "Remove link" })).toBeNull();
  });

  test("a layer inside a main component says every instance gets its link", () => {
    const { emit } = setup();
    emit({ type: "selection", state: layer({ warning: "inside-main-component" }) });
    expect(
      screen.getByText("This layer is inside a main component, so every instance of the component gets this link."),
    ).toBeTruthy();
  });

  test("a main component says every instance gets the link", () => {
    const { emit } = setup();
    emit({ type: "selection", state: layer({ warning: "main-component" }) });
    expect(
      screen.getByText(
        "This is a main component, so every instance of it gets this link. To give one button its own haptic, link that instance instead.",
      ),
    ).toBeTruthy();
    expect(screen.getByRole("button", { name: "Link haptic" })).toBeTruthy();
  });

  test("a component set explains that its instances won't get the link", () => {
    const { emit } = setup();
    emit({ type: "selection", state: layer({ warning: "component-set" }) });
    expect(
      screen.getByText(
        "This is a component set. Instances get links from their variant, not the set — link a variant or an instance instead.",
      ),
    ).toBeTruthy();
  });

  test("a card's Code tab shows the pattern and setup", () => {
    const { emit } = setup();
    emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: presetNamed("Success") } });
    fireEvent.click(screen.getByRole("tab", { name: "Code" }));
    expect(screen.getByText(/val SuccessHaptic: HapticPattern/)).toBeTruthy();
    expect(screen.getByText(/jindong-compose:2\.0\.0/)).toBeTruthy();
  });

  test("the Code tab says when the card has unsaved changes", () => {
    const unsaved = "This card has unsaved changes. Press Update card to include them in the code.";
    const { emit } = setup();
    emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: presetNamed("Success") } });
    fireEvent.click(screen.getByRole("tab", { name: "Code" }));
    expect(screen.queryByText(unsaved)).toBeNull();
    fireEvent.click(screen.getByRole("tab", { name: "Edit" }));
    fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Paid" } });
    fireEvent.click(screen.getByRole("tab", { name: "Code" }));
    expect(screen.getByText(unsaved)).toBeTruthy();
  });

  test("a failed copy says so instead of looking copied", async () => {
    Object.defineProperty(navigator, "clipboard", { value: undefined, configurable: true });
    document.execCommand = () => false;
    const { emit } = setup();
    emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: presetNamed("Success") } });
    fireEvent.click(screen.getByRole("tab", { name: "Code" }));
    await act(async () => {
      fireEvent.click(screen.getAllByRole("button", { name: "Copy" })[0]);
    });
    expect(screen.getByRole("button", { name: "Copy failed" })).toBeTruthy();
    expect(screen.getByText("The code is selected. Press ⌘C to copy it.")).toBeTruthy();
    expect(window.getSelection()?.toString()).toContain("val SuccessHaptic");
  });

  test("a linked layer's Code tab shows the Jindong call", () => {
    const { emit } = setup();
    emit({
      type: "selection",
      state: layer({ link: { schema: 1, cardId: "9:1", trigger: "tap" }, linkedPattern: presetNamed("Success") }),
    });
    fireEvent.click(screen.getByRole("tab", { name: "Code" }));
    expect(screen.getByText(/Clip\(SuccessHaptic\)/)).toBeTruthy();
    expect(screen.getByText(/var payButtonSuccessTaps by remember/)).toBeTruthy();
  });

  test("an unlinked layer has no Code tab", () => {
    const { emit } = setup();
    emit({ type: "selection", state: layer() });
    expect(screen.queryByRole("tab", { name: "Code" })).toBeNull();
  });

  test("Preview rhythm lights each event while it plays, with timers rather than animation (Reduce Motion)", () => {
    vi.useFakeTimers();
    try {
      const { emit } = setup();
      // Success: buzzes at 0, 150 and 300 ms, each lit for at least 160 ms.
      emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: presetNamed("Success") } });
      fireEvent.click(screen.getByRole("button", { name: /Preview rhythm/ }));
      const lit = () =>
        [...screen.getByRole("img", { name: /Success rhythm/ }).querySelectorAll("g.bar")].map((bar) =>
          bar.classList.contains("lit"),
        );
      act(() => vi.advanceTimersByTime(10));
      expect(lit()).toEqual([true, false, false]);
      act(() => vi.advanceTimersByTime(190));
      expect(lit()).toEqual([false, true, false]);
      act(() => vi.advanceTimersByTime(120));
      expect(lit()).toEqual([false, false, true]);
      act(() => vi.advanceTimersByTime(200));
      expect(lit()).toEqual([false, false, false]);
    } finally {
      vi.useRealTimers();
    }
  });

  test("the timeline shows the haptic's total length rather than the chart's scale", () => {
    const { emit } = setup();
    emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: presetNamed("Success") } });
    expect(screen.getByText("460 ms total")).toBeTruthy();
    expect(screen.queryByText("506 ms")).toBeNull();
  });

  test("Preview rhythm plays the haptic as sound, since a computer can't vibrate", () => {
    vi.stubGlobal("AudioContext", FakeAudioContext);
    try {
      const { emit } = setup();
      emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: presetNamed("Success") } });
      fireEvent.click(screen.getByRole("button", { name: /Preview rhythm/ }));
      expect(FakeAudioContext.created.at(-1)?.oscillators).toHaveLength(3);
    } finally {
      vi.unstubAllGlobals();
    }
  });

  test("Preview rhythm ends on time even when no animation runs (Reduce Motion)", () => {
    vi.useFakeTimers();
    try {
      const { emit } = setup();
      emit({ type: "selection", state: { kind: "card", nodeId: "1:2", pattern: presetNamed("Success") } });
      fireEvent.click(screen.getByRole("button", { name: /Preview rhythm/ }));
      const svg = () => screen.getByRole("img", { name: /Success rhythm/ });
      expect(svg().getAttribute("class")).toContain("playing");
      act(() => vi.advanceTimersByTime(460 + 300));
      expect(svg().getAttribute("class")).not.toContain("playing");
    } finally {
      vi.useRealTimers();
    }
  });

  test("warnings stay long enough to read; confirmations clear quickly", () => {
    vi.useFakeTimers();
    try {
      const { emit } = setup();
      emit({ type: "selection", state: { kind: "none" } });
      emit({ type: "status", tone: "success", message: "Card created." });
      act(() => vi.advanceTimersByTime(4500));
      expect(screen.getByRole("status").textContent).toBe("");
      emit({ type: "status", tone: "warning", message: "Linked, but Figma did not accept the Dev Mode note." });
      act(() => vi.advanceTimersByTime(6000));
      expect(screen.getByRole("status").textContent).toBe("Linked, but Figma did not accept the Dev Mode note.");
      act(() => vi.advanceTimersByTime(4000));
      expect(screen.getByRole("status").textContent).toBe("");
    } finally {
      vi.useRealTimers();
    }
  });

  test("status messages from the main thread are shown", () => {
    const { emit } = setup();
    emit({ type: "selection", state: { kind: "none" } });
    emit({ type: "status", tone: "success", message: "Card created." });
    expect(screen.getByRole("status").textContent).toBe("Card created.");
  });
});
