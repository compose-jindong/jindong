import { readFileSync } from "node:fs";
import { expect, test } from "vitest";
import { JINDONG_VERSION } from "./version";

function atLeast(version: string, minimum: string): boolean {
  const [a, b] = [version.split(".").map(Number), minimum.split(".").map(Number)];
  for (let i = 0; i < Math.max(a.length, b.length); i++) {
    if ((a[i] ?? 0) !== (b[i] ?? 0)) return (a[i] ?? 0) > (b[i] ?? 0);
  }
  return true;
}

// The golden tests check that the generated code compiles against this repository's API.
test("the repository builds at least the Jindong version the setup snippet installs", () => {
  const gradle = readFileSync(new URL("../../../jindong-core/build.gradle.kts", import.meta.url), "utf8");
  const version = gradle.match(/^version = "(.+)"$/m)?.[1]?.replace(/-SNAPSHOT$/, "");
  expect(version).toBeDefined();
  expect(atLeast(version!, JINDONG_VERSION)).toBe(true);
});

test("versions compare by number, part by part", () => {
  expect(atLeast("2.1.0", "2.0.0")).toBe(true);
  expect(atLeast("2.0.0", "2.0.0")).toBe(true);
  expect(atLeast("1.10.0", "1.9.0")).toBe(true);
  expect(atLeast("1.1.1", "2.0.0")).toBe(false);
});
