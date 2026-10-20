import { LIMITS, type HapticEvent, type HapticLink, type PatternSpec, type Trigger } from "./model";
import { DOCS_URL, JINDONG_VERSION } from "./version";

// Kotlin for the Jindong 2.0 API. Generated code must compile and replay the card's timeline;
// figma-plugin/golden holds the Gradle-checked copies.

export type CodeLanguage = "KOTLIN" | "PLAINTEXT";

export interface CodeSection {
  title: "Haptic" | "Pattern" | "Setup" | "Jindong";
  language: CodeLanguage;
  code: string;
}

export const NO_HAPTIC_NOTICE = `No Jindong haptic on this layer.
Designers can add one with the Jindong Figma plugin.
${DOCS_URL}`;

export const BROKEN_LINK_NOTICE = `This layer was linked to a Jindong haptic card that no longer exists.
Ask your designer to link it again with the Jindong Figma plugin.`;

export const UNREADABLE_CARD_NOTICE =
  "This haptic card can't be read. It may come from a newer version of the Jindong Figma plugin.";

const INDENT = "    ";
const NAMED_INTENSITY: Record<number, string> = { 0.25: "LIGHT", 0.75: "STRONG", 1: "HIGH" };

/** A Kotlin float literal for a strength or sharpness with at most two decimals. */
export function formatUnit(x: number): string {
  return `${Number(x.toFixed(2))}f`;
}

/** PascalCase of the card name plus `Haptic`; Kotlin identifiers allow only letters and decimal digits here. */
export function patternVariableName(cardName: string): string {
  const pascal = cardName
    .split(/[^\p{L}\p{Nd}]+/u)
    .filter((word) => word.length > 0)
    .map((word) => word[0].toUpperCase() + word.slice(1))
    .join("");
  if (pascal.length === 0) return "CustomHaptic";
  return /^\p{Nd}/u.test(pascal) ? `Haptic${pascal}` : `${pascal}Haptic`;
}

/** True when [name] generates the same Kotlin value as one of [otherNames], so pasting both won't compile. */
export function clashesWith(name: string, otherNames: readonly string[]): boolean {
  const variable = patternVariableName(name);
  return otherNames.some((other) => patternVariableName(other) === variable);
}

/** [name], or "name 2", "name 3"… so that every card in a file generates its own Kotlin value. */
export function uniqueCardName(name: string, takenNames: readonly string[]): string {
  if (!clashesWith(name, takenNames)) return name;
  for (let n = 2; ; n++) {
    const suffix = ` ${n}`;
    const candidate = `${name.slice(0, LIMITS.maxNameLength - suffix.length).trimEnd()}${suffix}`;
    if (!clashesWith(candidate, takenNames)) return candidate;
  }
}

export function patternCode(p: PatternSpec): string {
  const body: string[] = [];
  for (const event of p.events) {
    if (event.gapBeforeMs > 0) body.push(`delay(${event.gapBeforeMs}.ms)`);
    body.push(eventCall(event));
  }
  if (p.trailingMs > 0) body.push(`delay(${p.trailingMs}.ms)`);

  const text = body.join("\n");
  const imports = ["io.github.compose.jindong.core.dsl.buildHapticPattern", "io.github.compose.jindong.core.model.HapticPattern"];
  if (text.includes("HapticIntensity.")) imports.push("io.github.compose.jindong.core.model.HapticIntensity");
  if (text.includes(".ms")) imports.push("io.github.compose.jindong.core.ms");

  return [
    importLines(imports),
    "",
    `val ${patternVariableName(p.name)}: HapticPattern = buildHapticPattern {`,
    ...body.map((line) => INDENT + line),
    "}",
  ].join("\n");
}

const CALL_IMPORTS = ["io.github.compose.jindong.Jindong", "io.github.compose.jindong.dsl.Clip"];
const TAP_IMPORTS = [
  "androidx.compose.runtime.getValue",
  "androidx.compose.runtime.mutableIntStateOf",
  "androidx.compose.runtime.remember",
  "androidx.compose.runtime.setValue",
];

export function hapticCodeParts(variableName: string, trigger: Trigger): { imports: string[]; body: string } {
  const clip = `${INDENT}Clip(${variableName})`;
  switch (trigger) {
    case "tap": {
      // Named after the haptic, so two tappable elements in one composable get separate counters.
      const base = variableName.replace(/Haptic$/, "");
      const counter = `${base[0].toLowerCase()}${base.slice(1)}Taps`;
      return {
        imports: [...TAP_IMPORTS, ...CALL_IMPORTS],
        body: [
          "// Call inside JindongProvider { ... }",
          `var ${counter} by remember { mutableIntStateOf(0) }`,
          `Jindong(${counter}, playOnInitialComposition = false) {`,
          clip,
          "}",
          `// In the element's onClick: ${counter}++`,
        ].join("\n"),
      };
    }
    case "enter":
      return {
        imports: [...CALL_IMPORTS],
        body: [
          "// Call inside JindongProvider { ... }",
          "// Plays when this screen enters the composition",
          "Jindong(Unit) {",
          clip,
          "}",
        ].join("\n"),
      };
    case "state":
      return {
        imports: [...CALL_IMPORTS],
        body: [
          "// Call inside JindongProvider { ... }",
          "// Replace `state` with the value whose change should play this haptic",
          "Jindong(state, playOnInitialComposition = false) {",
          clip,
          "}",
        ].join("\n"),
      };
  }
}

export function hapticCode(variableName: string, trigger: Trigger): string {
  const { imports, body } = hapticCodeParts(variableName, trigger);
  return `${importLines(imports)}\n\n${body}`;
}

export function setupCode(): string {
  return [
    "// build.gradle.kts (commonMain dependencies)",
    `implementation("io.github.compose-jindong:jindong-core:${JINDONG_VERSION}")`,
    `implementation("io.github.compose-jindong:jindong-compose:${JINDONG_VERSION}")`,
    "",
    "// AndroidManifest.xml",
    '// <uses-permission android:name="android.permission.VIBRATE" />',
    "",
    `// Docs: ${DOCS_URL}`,
  ].join("\n");
}

/** What Dev Mode and the plugin's Code tab show for one selected layer. */
export function codeSections(input: {
  pattern: PatternSpec | null;
  link: HapticLink | null;
  linkedPattern: PatternSpec | null;
  /** The layer is a card whose data this plugin version can't read. */
  unreadableCard?: boolean;
}): CodeSection[] {
  if (input.unreadableCard) return [{ title: "Jindong", language: "PLAINTEXT", code: UNREADABLE_CARD_NOTICE }];
  const setup: CodeSection = { title: "Setup", language: "KOTLIN", code: setupCode() };
  if (input.pattern) {
    return [{ title: "Pattern", language: "KOTLIN", code: patternCode(input.pattern) }, setup];
  }
  if (input.link && input.linkedPattern) {
    const name = patternVariableName(input.linkedPattern.name);
    return [
      { title: "Haptic", language: "KOTLIN", code: hapticCode(name, input.link.trigger) },
      { title: "Pattern", language: "KOTLIN", code: patternCode(input.linkedPattern) },
      setup,
    ];
  }
  if (input.link) return [{ title: "Jindong", language: "PLAINTEXT", code: BROKEN_LINK_NOTICE }];
  return [{ title: "Jindong", language: "PLAINTEXT", code: NO_HAPTIC_NOTICE }];
}

function eventCall(event: HapticEvent): string {
  const args: string[] = [];
  if (event.type === "continuous") args.push(`${event.durationMs}.ms`);
  if (event.intensity !== 0.5) {
    const named = NAMED_INTENSITY[event.intensity];
    args.push(named ? `HapticIntensity.${named}` : `HapticIntensity.Custom(${formatUnit(event.intensity)})`);
  }
  if (event.sharpness !== 0.5) args.push(`sharpness = ${formatUnit(event.sharpness)}`);
  return `${event.type}(${args.join(", ")})`;
}

function importLines(imports: string[]): string {
  return [...imports]
    .sort()
    .map((name) => `import ${name}`)
    .join("\n");
}
