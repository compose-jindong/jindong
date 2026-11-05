import type { MainToUi, UiToMain } from "../shared/messages";

export interface Bridge {
  send(message: UiToMain): void;
  subscribe(handler: (message: MainToUi) => void): () => void;
}

/** Talks to the plugin's main thread through Figma's iframe messaging. */
export const figmaBridge: Bridge = {
  send(message) {
    parent.postMessage({ pluginMessage: message }, "*");
  },
  subscribe(handler) {
    const listener = (event: MessageEvent) => {
      const message = event.data?.pluginMessage as MainToUi | undefined;
      if (message) handler(message);
    };
    window.addEventListener("message", listener);
    return () => window.removeEventListener("message", listener);
  },
};
