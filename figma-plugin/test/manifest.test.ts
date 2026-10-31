import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { expect, test } from "vitest";

// Figma decides from the manifest where the plugin may run, so these settings can't be checked any other way.

const manifest = JSON.parse(readFileSync(fileURLToPath(new URL("../manifest.json", import.meta.url)), "utf8"));
const card = readFileSync(fileURLToPath(new URL("../src/main/card.ts", import.meta.url)), "utf8");

test("Dev Mode can open the window from its Plugins tab and a card's Edit haptic button, besides generating code", () => {
  expect(manifest.editorType).toEqual(expect.arrayContaining(["figma", "dev"]));
  // Without "inspect", Figma won't run the plugin in Dev Mode's panel, so the button does nothing there.
  expect(manifest.capabilities).toEqual(expect.arrayContaining(["codegen", "inspect"]));
});

test("the Edit haptic button's command is the one cards are given", () => {
  const commands = manifest.relaunchButtons.map((button: { command: string }) => button.command);
  for (const [, command] of card.matchAll(/setRelaunchData\(\{\s*(\w+):/g)) expect(commands).toContain(command);
});
