import { describe, expect, test } from "vitest";
import {
  BROKEN_LINK_NOTICE,
  NO_HAPTIC_NOTICE,
  UNREADABLE_CARD_NOTICE,
  clashesWith,
  codeSections,
  hapticCode,
  patternCode,
  patternVariableName,
  setupCode,
  uniqueCardName,
} from "./codegen";
import type { HapticEvent, HapticLink } from "./model";
import { presetNamed } from "./presets";
import { buzz, tap } from "./testing";

const SUCCESS_CODE = `import io.github.compose.jindong.core.dsl.buildHapticPattern
import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.core.model.HapticPattern
import io.github.compose.jindong.core.ms

val SuccessHaptic: HapticPattern = buildHapticPattern {
    continuous(50.ms, HapticIntensity.Custom(0.3f))
    delay(100.ms)
    continuous(50.ms, HapticIntensity.Custom(0.55f))
    delay(100.ms)
    continuous(60.ms, HapticIntensity.Custom(0.8f))
    delay(100.ms)
}`;

const TAP_CODE = `import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.compose.jindong.Jindong
import io.github.compose.jindong.dsl.Clip

// Call inside JindongProvider { ... }
var successTaps by remember { mutableIntStateOf(0) }
Jindong(successTaps, playOnInitialComposition = false) {
    Clip(SuccessHaptic)
}
// In the element's onClick: successTaps++`;

describe("patternVariableName", () => {
  test.each([
    ["Success", "SuccessHaptic"],
    ["Double Tap", "DoubleTapHaptic"],
    ["my button – ok!", "MyButtonOkHaptic"],
    ["doubleTap", "DoubleTapHaptic"],
    ["성공 진동", "성공진동Haptic"],
    ["3 Taps!", "Haptic3Taps"],
    ["Level ²", "LevelHaptic"],
    ["ǰet", "ǰetHaptic"],
    ["İptal", "İptalHaptic"],
    ["🎉", "CustomHaptic"],
    ["", "CustomHaptic"],
  ])("%s → %s", (name, expected) => {
    expect(patternVariableName(name)).toBe(expected);
  });
});

describe("uniqueCardName", () => {
  test("keeps a name no other card's code uses", () => {
    expect(uniqueCardName("Success", ["Heartbeat"])).toBe("Success");
  });

  test("numbers a name whose Kotlin value is taken", () => {
    expect(uniqueCardName("Success", ["Success"])).toBe("Success 2");
    expect(uniqueCardName("Success", ["Success", "Success 2"])).toBe("Success 3");
    expect(uniqueCardName("Pay-ok", ["pay ok"])).toBe("Pay-ok 2");
  });

  test("stays within the name limit", () => {
    const long = "x".repeat(40);
    expect(uniqueCardName(long, [long])).toBe(`${"x".repeat(38)} 2`);
  });

  test("clash detection compares Kotlin values, not text", () => {
    expect(clashesWith("Pay-ok", ["pay ok"])).toBe(true);
    expect(clashesWith("Pay OK", ["pay ok"])).toBe(false);
  });
});

describe("patternCode", () => {
  const code = (events: HapticEvent[], trailingMs = 0) =>
    patternCode({ ...presetNamed("Success"), name: "X", events, trailingMs });

  test("Success matches the spec example", () => {
    expect(patternCode(presetNamed("Success"))).toBe(SUCCESS_CODE);
  });

  test("omits default arguments and names the standard strengths", () => {
    expect(code([tap(0)])).toContain("    transient()");
    expect(code([tap(0, 0.75, 0.9)])).toContain("transient(HapticIntensity.STRONG, sharpness = 0.9f)");
    expect(code([tap(0, 0.5, 0.9)])).toContain("transient(sharpness = 0.9f)");
    expect(code([buzz(0, 70)])).toContain("continuous(70.ms)");
    expect(code([buzz(0, 70, 0.5, 0.8)])).toContain("continuous(70.ms, sharpness = 0.8f)");
    expect(code([buzz(0, 20, 0.25), buzz(0, 20, 1), buzz(0, 20, 0)]).split("\n").slice(-4, -1)).toEqual([
      "    continuous(20.ms, HapticIntensity.LIGHT)",
      "    continuous(20.ms, HapticIntensity.HIGH)",
      "    continuous(20.ms, HapticIntensity.Custom(0f))",
    ]);
  });

  test("writes pauses and silence as delays", () => {
    expect(code([buzz(120, 40)], 30).split("\n").slice(-4)).toEqual([
      "    delay(120.ms)",
      "    continuous(40.ms)",
      "    delay(30.ms)",
      "}",
    ]);
    expect(code([buzz(0, 40)], 0)).not.toMatch(/delay/);
  });

  test("a zero-length pattern imports only what it uses", () => {
    const c = patternCode({ ...presetNamed("Success"), name: "One", events: [tap(0)], trailingMs: 0 });
    expect(c.split("\n").filter((l) => l.startsWith("import "))).toEqual([
      "import io.github.compose.jindong.core.dsl.buildHapticPattern",
      "import io.github.compose.jindong.core.model.HapticPattern",
    ]);
  });
});

const LINK: HapticLink = { schema: 1, cardId: "1:2", trigger: "tap" };

describe("hapticCode", () => {
  test("tap remembers a counter and waits for the first tap", () => {
    expect(hapticCode("SuccessHaptic", "tap")).toBe(TAP_CODE);
  });

  test("two tap haptics pasted into one composable don't declare the same counter", () => {
    const counter = (code: string) => code.match(/^var (\S+) by remember/m)?.[1];
    expect(counter(hapticCode("SuccessHaptic", "tap"))).toBe("successTaps");
    expect(counter(hapticCode("ErrorHaptic", "tap"))).toBe("errorTaps");
    expect(counter(hapticCode("Haptic3Taps", "tap"))).toBe("haptic3TapsTaps");
    expect(counter(hapticCode("성공진동Haptic", "tap"))).toBe("성공진동Taps");
    // Lowercasing "İ" adds a combining dot, which Kotlin names can't contain.
    expect(counter(hapticCode("İptalHaptic", "tap"))).toBe("İptalTaps");
  });

  test("buttons sharing one haptic get counters named after each button", () => {
    const counter = (code: string) => code.match(/^var (\S+) by remember/m)?.[1];
    expect(counter(hapticCode("SuccessHaptic", "tap", "Pay button"))).toBe("payButtonTaps");
    expect(counter(hapticCode("SuccessHaptic", "tap", "Confirm button"))).toBe("confirmButtonTaps");
    expect(counter(hapticCode("SuccessHaptic", "tap", "🎉"))).toBe("successTaps");
    expect(counter(hapticCode("SuccessHaptic", "tap", "3rd button"))).toBe("successTaps");
    expect(
      codeSections({ pattern: null, link: LINK, linkedPattern: presetNamed("Success"), layerName: "Pay button" })[0].code,
    ).toContain("Jindong(payButtonTaps, playOnInitialComposition = false)");
  });

  test("enter plays on entry", () => {
    const c = hapticCode("SuccessHaptic", "enter");
    expect(c).toContain("Jindong(Unit) {\n    Clip(SuccessHaptic)\n}");
    expect(c).not.toContain("playOnInitialComposition");
    expect(c).not.toContain("remember");
  });

  test("state waits for the state to change", () => {
    const c = hapticCode("SuccessHaptic", "state");
    expect(c).toContain("Jindong(state, playOnInitialComposition = false) {\n    Clip(SuccessHaptic)\n}");
    expect(c).toContain("// Replace `state` with the value whose change should play this haptic");
  });
});

test("setup names the artifacts, permission and docs", () => {
  const c = setupCode();
  expect(c).toContain('implementation("io.github.compose-jindong:jindong-core:2.0.0")');
  expect(c).toContain('implementation("io.github.compose-jindong:jindong-compose:2.0.0")');
  expect(c).toContain('// <uses-permission android:name="android.permission.VIBRATE" />');
  expect(c).toContain("// Docs: https://compose-jindong.github.io/jindong");
});

describe("codeSections", () => {
  const success = presetNamed("Success");
  const link: HapticLink = { schema: 1, cardId: "1:2", trigger: "tap" };

  test("a card shows its pattern and setup", () => {
    const sections = codeSections({ pattern: success, link: null, linkedPattern: null });
    expect(sections.map((s) => [s.title, s.language])).toEqual([
      ["Pattern", "KOTLIN"],
      ["Setup", "KOTLIN"],
    ]);
    expect(sections[0].code).toBe(SUCCESS_CODE);
  });

  test("a linked layer shows the call, the pattern and setup", () => {
    const sections = codeSections({ pattern: null, link, linkedPattern: success });
    expect(sections.map((s) => s.title)).toEqual(["Haptic", "Pattern", "Setup"]);
    expect(sections[0].code).toBe(TAP_CODE);
  });

  test("a link to a deleted card says so", () => {
    expect(codeSections({ pattern: null, link, linkedPattern: null })).toEqual([
      { title: "Jindong", language: "PLAINTEXT", code: BROKEN_LINK_NOTICE },
    ]);
  });

  test("a broken link names the card it remembers", () => {
    const [notice] = codeSections({
      pattern: null,
      link: { schema: 1, cardId: "9:9", trigger: "tap", cardName: "Success" },
      linkedPattern: null,
    });
    expect(notice).toEqual({
      title: "Jindong",
      language: "PLAINTEXT",
      code: "This layer was linked to the Jindong haptic card “Success”, which was moved or deleted.\nAsk your designer to link it again with the Jindong Figma plugin.",
    });
  });

  test("an unreadable card says it can't be read", () => {
    expect(codeSections({ pattern: null, link: null, linkedPattern: null, unreadableCard: true })).toEqual([
      { title: "Jindong", language: "PLAINTEXT", code: UNREADABLE_CARD_NOTICE },
    ]);
    expect(UNREADABLE_CARD_NOTICE).toBe(
      "This haptic card can't be read. It may come from a newer version of the Jindong Figma plugin.",
    );
  });

  test("any other layer gets a short hint", () => {
    expect(codeSections({ pattern: null, link: null, linkedPattern: null })).toEqual([
      { title: "Jindong", language: "PLAINTEXT", code: NO_HAPTIC_NOTICE },
    ]);
    expect(NO_HAPTIC_NOTICE).toBe(
      "No Jindong haptic on this layer.\nDesigners can add one with the Jindong Figma plugin.\nhttps://compose-jindong.github.io/jindong",
    );
  });
});
