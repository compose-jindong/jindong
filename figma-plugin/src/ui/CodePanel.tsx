import { useEffect, useState } from "react";
import type { CodeSection } from "../core/codegen";
import { copyText } from "./copy";

const SECTION_HINT: Record<CodeSection["title"], string> = {
  Haptic: "Where the layer is built",
  Pattern: "The haptic itself",
  Setup: "Once per project",
  Jindong: "",
};

export function CodePanel({ sections, notice }: { sections: CodeSection[]; notice?: string }) {
  return (
    <div className="code-panel">
      {notice && <p className="callout">{notice}</p>}
      <p className="code-intro">
        Send this to your developer. With Dev Mode, they can also select the layer and pick Jindong (Compose) in
        the code panel.
      </p>
      {sections.map((section) => (
        <CodeBlock key={section.title} section={section} />
      ))}
    </div>
  );
}

function CodeBlock({ section }: { section: CodeSection }) {
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!copied) return;
    const timer = setTimeout(() => setCopied(false), 1500);
    return () => clearTimeout(timer);
  }, [copied]);

  return (
    <section className="code-block">
      <header className="code-block-head">
        <h3>{section.title}</h3>
        {SECTION_HINT[section.title] && <span className="code-hint">{SECTION_HINT[section.title]}</span>}
        <button type="button" className="secondary" onClick={async () => setCopied(await copyText(section.code))}>
          {copied ? "Copied" : "Copy"}
        </button>
      </header>
      <pre>
        <code>{section.code}</code>
      </pre>
    </section>
  );
}
