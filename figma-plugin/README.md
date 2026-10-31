# Jindong for Figma

A Figma plugin for designing haptic patterns and handing developers the Jindong code.

## Features

- **Presets** - Start from the same patterns as the sample app
- **Editor** - Shape a haptic as Taps and Buzzes, each with a pause, length, strength and sharpness
- **Preview rhythm** - Hear the pattern and watch each event light up as it plays
- **Haptic cards** - Each haptic lives on the canvas as a card, and edits apply as you make them
- **Links** - Link a layer to a card and choose when it plays: on tap, on screen enter or on state change
- **Dev Mode** - Pick **Jindong (Compose)** in the code panel to get the pattern, the composable call and the setup

## Usage

1. With nothing selected, open the plugin and pick a preset.
2. Adjust the events, then press **Create card**.
3. Select a layer, pick a card and when it plays, then press **Link haptic**.
4. In Dev Mode, select the layer and pick **Jindong (Compose)** in the code panel. Without Dev Mode, copy the code from the plugin's **Code** tab.

> **Note**: Dev Mode is read-only for plugins. Running Jindong there shows the selected haptic, its preview and its code. Edit haptics in Design Mode.

## Try it in Figma

1. Run `yarn install && yarn build` in `figma-plugin/`.
2. In the Figma desktop app, choose **Plugins → Development → Import plugin from manifest…** and pick `figma-plugin/manifest.json`.
3. Run **Jindong** from the plugins menu.

## Development

Uses yarn 1, like `documentation/`.

```bash
yarn build       # Build dist/main.js and dist/index.html
yarn test        # Unit, UI and golden tests
yarn typecheck   # Type-check, including the Figma API calls
yarn golden      # Rewrite golden/ from the code generator
yarn preview:ui  # Open the plugin window in a browser with a pretend Figma
```

The preview accepts `?state=none|card|layer|linked|multiple`, `&page=<name>` and `&dev=1` (read-only, as in Dev Mode).

### Golden tests

`golden/` holds the Kotlin the generator writes for every preset and some edge cases. `jindong-core` and `jindong-compose` compile it in `commonTest` and check that it plays the card's timeline.

- After changing the generator, run `yarn golden` and commit the result. `yarn test` fails while `golden/` is stale.
- When the Jindong version changes, update `src/core/version.ts`.

### Data

Cards and links are stored as shared plugin data under the `jindong` namespace: `pattern` on a card and `link` on a linked layer, both JSON with a `schema` version. The Figma REST API can read them too.

## Publishing

1. Create the plugin in Figma to get its ID, and replace `jindong-local-dev` in `manifest.json`.
2. Prepare a 128×128 icon and a 1920×960 cover image.
3. With a fresh build, check in Figma desktop: create and edit a card, link a layer and check its Dev Mode note, check the code in Dev Mode and in the Code tab, and press **Preview rhythm** to hear it.
4. Publish from the Figma desktop app.
