import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";
import { viteSingleFile } from "vite-plugin-singlefile";

// Figma loads the plugin window from one HTML file, so scripts and styles are inlined.
// src/ui/preview.html is for the dev server only (`yarn preview:ui`).
export default defineConfig({
  root: "src/ui",
  plugins: [react(), viteSingleFile()],
  build: {
    outDir: "../../dist",
    emptyOutDir: false,
  },
});
