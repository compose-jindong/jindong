import { useEffect, useRef, useState } from "react";
import type { CodeSection } from "../core/codegen";
import { copyText } from "./copy";

const SECTION_HINT: Record<CodeSection["title"], string> = {
  Haptic: "Where the layer is built",
  Pattern: "The haptic itself",
  Setup: "Once per project",
  Jindong: "",
};

export function CodePanel({ sections }: { sections: CodeSection[] }) {
  return (
    <div className="code-panel">
      {sections.map((section) => (
        <CodeBlock key={section.title} section={section} />
      ))}
    </div>
  );
}

function CodeBlock({ section }: { section: CodeSection }) {
  const [copy, setCopy] = useState<"idle" | "copied" | "failed">("idle");
  const code = useRef<HTMLElement>(null);

  useEffect(() => {
    if (copy !== "copied") return;
    const timer = setTimeout(() => setCopy("idle"), 1500);
    return () => clearTimeout(timer);
  }, [copy]);

  async function onCopy() {
    if (await copyText(section.code)) {
      setCopy("copied");
      return;
    }
    // Leave the code selected so ⌘C still works when the plugin isn't allowed to copy.
    if (code.current) window.getSelection()?.selectAllChildren(code.current);
    setCopy("failed");
  }

  return (
    <section className="code-block">
      <header className="code-block-head">
        <h3>{section.title}</h3>
        {SECTION_HINT[section.title] && <span className="code-hint">{SECTION_HINT[section.title]}</span>}
        <button type="button" className="secondary" onClick={onCopy}>
          {copy === "copied" ? "Copied" : copy === "failed" ? "Copy failed" : "Copy"}
        </button>
      </header>
      {copy === "failed" && <p className="form-error">The code is selected. Press ⌘C to copy it.</p>}
      <pre>
        <code ref={code}>{section.code}</code>
      </pre>
    </section>
  );
}
