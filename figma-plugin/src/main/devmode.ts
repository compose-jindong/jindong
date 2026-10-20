import { codeSections } from "../core/codegen";
import { findCard, readLink, readPattern } from "./storage";

/** Serves the "Jindong (Compose)" language in Dev Mode's code panel for the selected layer. */
export function registerCodegen(): void {
  figma.codegen.on("generate", async ({ node }) => {
    const card = await findCard(node);
    const pattern = card ? readPattern(card) : null;
    const link = card ? null : readLink(node);
    const linkedPattern = link ? readPattern(await figma.getNodeByIdAsync(link.cardId)) : null;
    const unreadableCard = card !== null && pattern === null;
    return codeSections({ pattern, link, linkedPattern, unreadableCard, layerName: node.name }).map(({ title, language, code }) => ({
      title,
      language,
      code,
    }));
  });
}
