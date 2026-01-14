import { docs, meta } from "@/.source";
import { resolveFiles } from "fumadocs-mdx";
import { loader } from "fumadocs-core/source";
import { i18n } from "./i18n";

export const source = loader({
  baseUrl: "/docs",
  i18n,
  source: {
    files: resolveFiles({ docs, meta }),
  },
});
