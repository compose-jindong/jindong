import { codeSections } from "../core/codegen";
import { readLink, readPattern } from "./storage";

/** Serves the "Jindong (Compose)" language in Dev Mode's code panel for the selected layer. */
export function registerCodegen(): void {
  figma.codegen.on("generate", async ({ node }) => {
    const pattern = readPattern(node);
    const link = pattern ? null : readLink(node);
    const linkedPattern = link ? readPattern(await figma.getNodeByIdAsync(link.cardId)) : null;
    return codeSections({ pattern, link, linkedPattern }).map(({ title, language, code }) => ({ title, language, code }));
  });
}
