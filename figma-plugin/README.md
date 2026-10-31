# Jindong for Figma

A Figma plugin that lets designers shape haptic patterns and hand developers working Jindong code.

Haptics usually start as a designer's idea and reach the app much later, if at all. With this plugin a designer
picks a pattern, tunes it, drops it on the canvas as a card, and links it to the button or screen it belongs to.
A developer who selects that layer in Dev Mode gets the Kotlin for it, including how to install Jindong. Teams
without Dev Mode can copy the same code from the plugin's Code tab.

## How designers use it

1. Open the plugin with nothing selected and pick a preset (the same presets as the sample app).
2. Adjust it: each event is a Tap (a short impact) or a Buzz (a vibration with a length), with a pause before it,
   a strength and a sharpness. Press **Preview rhythm** to watch and hear it: a computer can't vibrate, so taps
   play as short knocks and buzzes as a hum, louder when stronger and higher when sharper.
3. Press **Create card**. The card appears on the canvas, drawn by the plugin, and the plugin goes back to the
   presets. Select the card later to edit it again.
4. Select a layer, pick a card and when it plays (on tap, on screen enter, on state change), then press
   **Link haptic**. The layer gets a Dev Mode note that names the haptic and Jindong.

A few things the plugin handles for you:

- Cards keep distinct Kotlin names. A new card whose name is taken becomes "Success 2", and renaming a card to a
  taken name shows a warning. A card duplicated in Figma (⌘D, copy and paste) keeps its name, so rename one of
  the copies.
- Tap counters in the generated code are named after the linked layer and its haptic ("payButtonSuccessTaps"),
  so several tappable layers can be pasted into one composable. Two layers with the same name and the same haptic
  still share a name; rename one of the layers.
- Selecting a layer inside a card, or an instance of a card that was turned into a component, opens that card.
- Edits to a card reach the canvas as you make them, like Figma's own panels. Each pause is one undo step, and
  selecting something else applies the last edit. A new haptic that isn't created yet is dropped when you select
  something else.
- Layers you add inside a card stay on top of it when the plugin redraws it.
- A new card lands beside the cards already on the page, including cards inside sections.
- Groups, sections and a few other layer types can't hold Dev Mode notes. Linking one still works for code
  generation; the plugin says so, and a frame or instance shows the note.
- A link on a main component reaches every instance, and only the main component can remove it.
- Each plugin action is its own undo step.
- Dev Mode is read-only for plugins, so there Jindong shows the selected card or linked layer, its preview and its
  code, and says to switch to Design Mode to change it.

Version 1 keeps patterns simple: events play one after another with fixed strength and sharpness. Curves,
overlapping events and feeling a pattern on a phone come later.

## Develop

Uses yarn 1 like `documentation/`. Run these in `figma-plugin/`:

| Command | What it does |
|---|---|
| `yarn install` | Installs dependencies |
| `yarn build` | Builds `dist/main.js` (the Figma main thread) and `dist/index.html` (the plugin window) |
| `yarn test` | Runs unit, UI and golden tests |
| `yarn typecheck` | Type-checks everything, including the Figma API calls |
| `yarn golden` | Rewrites `golden/` from the code generator |
| `yarn preview:ui` | Opens the plugin window in a browser with a pretend Figma, for quick UI work |

The preview accepts `?state=none|card|layer|linked|multiple` to start from a given selection, `&page=<name>` to
name the page the cards are on and `&dev=1` to open the window read-only, as Dev Mode does. Its side panel can also
rename the Success card the way a teammate would.

## Try it in Figma

1. Run `yarn build`.
2. In the Figma desktop app, choose **Plugins → Development → Import plugin from manifest…** and pick
   `figma-plugin/manifest.json`.
3. In Design Mode, run **Jindong** from the plugins menu.
4. In Dev Mode, select a card or a linked layer and pick **Jindong (Compose)** as the code panel language, or run
   **Jindong** from the Plugins tab to see the haptic with its preview and code.

## Generated code is checked by Gradle

`golden/` holds the Kotlin that the generator writes for every preset and a few edge cases, next to the same
timeline written out as a literal `HapticPattern`. `jindong-core` and `jindong-compose` add these files to their
`commonTest` sources, so `./gradlew allTests` compiles them and checks that the generated code plays the card's
timeline.

- After changing the generator, run `yarn golden` and commit the result. `yarn test` fails while `golden/` is
  stale.
- If a Jindong API change breaks these tests, update the generator in the same pull request.
- When the Jindong version changes, update `src/core/version.ts`. A test compares it with
  `jindong-core/build.gradle.kts`.

## Where the data lives

Cards and links are stored as shared plugin data under the `jindong` namespace: `pattern` on a card frame and
`link` on a linked layer, both JSON with a `schema` version. The Figma REST API can read them too.

## Manual check before publishing

Run these in the Figma desktop app with a fresh build:

1. Create a card from a preset with nothing selected.
2. Select the card, change an event and its name, and check that the card redraws as you go. Click the canvas,
   press ⌘Z, and check that the plugin shows the undone values.
3. Link a button instance to the card and check its Dev Mode note.
4. Link the same layer again, then remove the link. The layer's other notes must stay, and there must never be
   two Jindong notes.
5. In Dev Mode, select the card, the linked layer and a layer without a haptic, with **Jindong (Compose)** as
   the language.
6. Delete the card and check that Dev Mode says the link is broken.
7. Copy each section from the plugin's Code tab and paste it somewhere.
8. In Dev Mode, run Jindong from the Plugins tab (or press **Edit haptic** on a card) and check that it shows the
   selected haptic and its code without editing.
9. Press **Preview rhythm** and check that you hear it.

If Figma refuses the Dev Mode note (the plugin says "Linked, but Figma did not accept the Dev Mode note."),
the link still works but developers without the plugin won't see it. Decide on a fallback before publishing.

## Publishing

1. In Figma, create the plugin to get its ID, and replace `jindong-local-dev` in `manifest.json`.
2. Prepare a 128×128 icon and a 1920×960 cover image.
3. Run `yarn build`, then publish from the Figma desktop app with the listing below.

### Community listing (draft)

**Name:** Jindong — Haptics for Compose

**Tagline:** Design haptic feedback in Figma and hand developers the code.

**Description:** Pick a haptic pattern, tune its rhythm and strength, and place it on your canvas as a card.
Link the card to the button or screen it belongs to. Developers select the layer in Dev Mode and get ready-to-use
Kotlin for Jindong, the haptics library for Compose Multiplatform on Android and iOS. No Dev Mode? Copy the
same code from the plugin.

**Tags:** haptics, vibration, compose, kotlin, android, ios, handoff, codegen
