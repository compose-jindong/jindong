import { codeSections } from "../core/codegen";
import { findCard, isCard, readLink, readPattern } from "./storage";

/** Serves the "Jindong (Compose)" language in Dev Mode's code panel for the selected layer. */
export function registerCodegen(): void {
  figma.codegen.on("generate", async ({ node }) => {
    const card = await findCard(node);
    const pattern = card ? readPattern(card) : null;
    const link = card ? null : readLink(node);
    const linkedCard = link ? await figma.getNodeByIdAsync(link.cardId) : null;
    const linkedPattern = readPattern(linkedCard);
    // A card that exists but can't be read isn't deleted; it likely comes from a newer plugin version.
    const unreadableCard = card ? pattern === null : linkedCard !== null && isCard(linkedCard) && linkedPattern === null;
    return codeSections({ pattern, link, linkedPattern, unreadableCard, layerName: node.name }).map(({ title, language, code }) => ({
      title,
      language,
      code,
    }));
  });
}
