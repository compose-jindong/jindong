import { mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { expect, test } from "vitest";
import { goldenFiles } from "./goldenFiles";

// `yarn golden` rewrites figma-plugin/golden; `yarn test` fails when the committed files drift
// from the generator, so Gradle always compiles what the plugin really emits.

const GOLDEN_DIR = fileURLToPath(new URL("../golden", import.meta.url));

test("golden Kotlin matches the code generator", () => {
  const expected = goldenFiles();

  if (process.env.UPDATE_GOLDEN) {
    rmSync(GOLDEN_DIR, { recursive: true, force: true });
    for (const [path, content] of expected) {
      mkdirSync(dirname(join(GOLDEN_DIR, path)), { recursive: true });
      writeFileSync(join(GOLDEN_DIR, path), content);
    }
    return;
  }

  expect(listFiles(GOLDEN_DIR), "Golden files are missing or extra. Run yarn golden").toEqual([...expected.keys()].sort());
  for (const [path, content] of expected) {
    expect(readFileSync(join(GOLDEN_DIR, path), "utf8"), `${path} is stale. Run yarn golden`).toBe(content);
  }
});

test("files the OS drops into a folder, like .DS_Store, don't count as golden files", () => {
  const dir = mkdtempSync(join(tmpdir(), "golden-"));
  mkdirSync(join(dir, "core"));
  writeFileSync(join(dir, "core", "Golden01.kt"), "");
  writeFileSync(join(dir, ".DS_Store"), "");
  writeFileSync(join(dir, "core", ".DS_Store"), "");
  expect(listFiles(dir)).toEqual(["core/Golden01.kt"]);
  rmSync(dir, { recursive: true, force: true });
});

function listFiles(dir: string, prefix = ""): string[] {
  let entries;
  try {
    entries = readdirSync(dir, { withFileTypes: true });
  } catch {
    return [];
  }
  return entries
    .filter((entry) => !entry.name.startsWith("."))
    .flatMap((entry) =>
      entry.isDirectory() ? listFiles(join(dir, entry.name), `${prefix}${entry.name}/`) : [`${prefix}${entry.name}`],
    )
    .sort();
}
