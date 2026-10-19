import { describe, expect, test } from "vitest";
import { annotationMarkdown, isJindongAnnotation, mergeJindongAnnotation } from "./annotation";

test("the Dev Mode note names the haptic, the moment and Jindong", () => {
  expect(annotationMarkdown("Success", "tap")).toBe(
    "**Jindong haptic:** Success · On tap\nMade with [Jindong](https://compose-jindong.github.io/jindong)",
  );
  expect(annotationMarkdown("Paid", "state")).toContain("Paid · On state change");
});

describe("mergeJindongAnnotation", () => {
  const other = { label: "Spacing note" };
  const old = { labelMarkdown: "**Jindong haptic:** Old · On tap" };
  const next = { labelMarkdown: annotationMarkdown("New", "enter") };

  test("linking keeps other notes and replaces ours", () => {
    expect(mergeJindongAnnotation([other, old], next)).toEqual([other, next]);
  });

  test("relinking never duplicates our note", () => {
    const twice = mergeJindongAnnotation(mergeJindongAnnotation([other], next), next);
    expect(twice.filter(isJindongAnnotation)).toHaveLength(1);
    expect(twice).toHaveLength(2);
  });

  test("unlinking removes only our note", () => {
    expect(mergeJindongAnnotation([old, other], null)).toEqual([other]);
  });

  test("notes are written back in a form Figma accepts", () => {
    expect(
      mergeJindongAnnotation(
        [
          { label: "A", labelMarkdown: "**A**", categoryId: "c1" },
          { properties: [{ type: "width" }] },
        ],
        null,
      ),
    ).toEqual([{ labelMarkdown: "**A**", categoryId: "c1" }, { properties: [{ type: "width" }] }]);
  });

  test("a plain-label note that mentions Jindong haptic counts as ours", () => {
    expect(isJindongAnnotation({ label: "Jindong haptic: Old · On tap" })).toBe(true);
    expect(isJindongAnnotation({ label: "Haptics TBD" })).toBe(false);
  });
});
