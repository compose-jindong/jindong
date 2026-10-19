// @vitest-environment jsdom
import { afterEach, expect, test, vi } from "vitest";
import { copyText } from "./copy";

function setClipboard(value: unknown) {
  Object.defineProperty(navigator, "clipboard", { value, configurable: true });
}

afterEach(() => {
  setClipboard(undefined);
});

test("uses the Clipboard API when it works", async () => {
  const writeText = vi.fn(async () => {});
  setClipboard({ writeText });
  expect(await copyText("hello")).toBe(true);
  expect(writeText).toHaveBeenCalledWith("hello");
});

test("falls back to execCommand when the Clipboard API is missing or blocked", async () => {
  const exec = vi.fn(() => true);
  document.execCommand = exec;
  setClipboard({ writeText: vi.fn(async () => Promise.reject(new Error("blocked by permissions policy"))) });
  expect(await copyText("x")).toBe(true);
  expect(exec).toHaveBeenCalledWith("copy");
  expect(document.querySelector("textarea")).toBeNull();

  setClipboard(undefined);
  expect(await copyText("y")).toBe(true);
  expect(exec).toHaveBeenCalledTimes(2);
});

test("reports failure when neither way copies", async () => {
  document.execCommand = vi.fn(() => false);
  expect(await copyText("x")).toBe(false);
});
