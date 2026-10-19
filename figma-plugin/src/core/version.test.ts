import { readFileSync } from "node:fs";
import { expect, test } from "vitest";
import { JINDONG_VERSION } from "./version";

test("the setup snippet installs the Jindong version this repository builds", () => {
  const gradle = readFileSync(new URL("../../../jindong-core/build.gradle.kts", import.meta.url), "utf8");
  const version = gradle.match(/^version = "(.+)"$/m)?.[1];
  expect(version?.replace(/-SNAPSHOT$/, "")).toBe(JINDONG_VERSION);
});
