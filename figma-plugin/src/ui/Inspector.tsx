import { TRIGGER_LABEL } from "../core/annotation";
import { UNREADABLE_CARD_NOTICE, codeSections, type CodeSection } from "../core/codegen";
import type { PatternSpec } from "../core/model";
import type { SelectionState } from "../shared/messages";
import { CodePanel } from "./CodePanel";
import { Notice } from "./Notice";
import { Timeline } from "./Timeline";

// The selected haptic, a preview and its code, for where the plugin can't change it: Dev Mode, or a card that
// comes from a team library.

const DEV_MODE_NOTE = "Dev Mode is read-only. Switch to Design Mode to change haptics.";

export function Inspector({ selection, note = DEV_MODE_NOTE }: { selection: SelectionState; note?: string }) {
  switch (selection.kind) {
    case "none":
      return <Notice>Select a haptic card, or a layer linked to one, to see its haptic and code.</Notice>;
    case "multiple":
      return <Notice>Select one layer to see its haptic and code.</Notice>;
    case "card": {
      const { pattern } = selection;
      if (!pattern) return <Notice>{UNREADABLE_CARD_NOTICE}</Notice>;
      const sections = codeSections({ pattern, link: null, linkedPattern: null });
      return (
        <View label="Haptic" title={pattern.name} summary={pattern.description} note={note} pattern={pattern} sections={sections} />
      );
    }
    case "layer": {
      const { link, linkedPattern, linkedCardUnreadable, linkFromLibrary, nodeName } = selection;
      if (!link) return <Notice>No haptic is linked to this layer.</Notice>;
      const sections = codeSections({
        pattern: null,
        link,
        linkedPattern,
        unreadableCard: linkedCardUnreadable,
        layerName: nodeName,
        linkFromLibrary,
      });
      // Without a readable card the code panel's own explanation is the whole answer.
      if (!linkedPattern) return <Notice>{sections[0].code}</Notice>;
      const summary = `Plays ${linkedPattern.name} · ${TRIGGER_LABEL[link.trigger]}`;
      return (
        <View label="Selected layer" title={nodeName} summary={summary} note={note} pattern={linkedPattern} sections={sections} />
      );
    }
  }
}

function View({
  label,
  title,
  summary,
  note,
  pattern,
  sections,
}: {
  label: string;
  title: string;
  summary: string;
  note: string;
  pattern: PatternSpec;
  sections: CodeSection[];
}) {
  return (
    <div className="dev-inspector">
      <header className="link-header">
        <span className="link-label">{label}</span>
        <h2>{title}</h2>
        {summary && <p className="inspector-summary">{summary}</p>}
      </header>
      <p className="callout">{note}</p>
      <Timeline pattern={pattern} width={320} height={96} detailed />
      <CodePanel sections={sections} />
    </div>
  );
}
