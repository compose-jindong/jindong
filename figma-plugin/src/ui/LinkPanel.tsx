import { useEffect, useState } from "react";
import { TRIGGER_LABEL } from "../core/annotation";
import { TONE_HEX, type Trigger } from "../core/model";
import type { CardSummary, SelectionState } from "../shared/messages";
import { Segmented } from "./EventInspector";

type LayerState = Extract<SelectionState, { kind: "layer" }>;

interface LinkPanelProps {
  state: LayerState;
  cards: CardSummary[] | null;
  onRequestCards: () => void;
  onLink: (cardId: string, trigger: Trigger) => void;
  onUnlink: () => void;
  /** An earlier request is still being handled. */
  busy?: boolean;
}

const TRIGGERS: Trigger[] = ["tap", "enter", "state"];

export function LinkPanel({ state, cards, onRequestCards, onLink, onUnlink, busy = false }: LinkPanelProps) {
  const { link, linkedPattern, warning } = state;
  const [cardId, setCardId] = useState<string | null>(link && linkedPattern ? link.cardId : null);
  const [trigger, setTrigger] = useState<Trigger>(link?.trigger ?? "tap");

  // Ask again whenever the list was reset, e.g. after the selection changed.
  useEffect(() => {
    if (cards === null) onRequestCards();
  }, [cards, onRequestCards]);

  // A cut and paste gives a card a new id; a card with the remembered name is probably the same one.
  const suggestion =
    link && !linkedPattern && link.cardName ? cards?.find((card) => card.name === link.cardName) : undefined;
  useEffect(() => {
    if (suggestion) setCardId((current) => current ?? suggestion.id);
  }, [suggestion]);

  const chosen = cards?.find((card) => card.id === cardId);

  return (
    <div className="link-panel">
      <header className="link-header">
        <span className="link-label">Selected layer</span>
        <h2>{state.nodeName}</h2>
      </header>

      {warning === "inside-instance" ? (
        <p className="callout">This layer is inside an instance. Select the instance itself to link a haptic.</p>
      ) : (
        <>
          {warning === "main-component" && (
            <p className="callout">
              This is a main component, so every instance of it gets this link. To give one button its own haptic, link that instance instead.
            </p>
          )}

          {link && (
            <div className={linkedPattern ? "current-link" : "current-link current-link-broken"}>
              <p>
                {linkedPattern
                  ? `Linked to ${linkedPattern.name} · ${TRIGGER_LABEL[link.trigger]}`
                  : suggestion
                    ? `The linked card was moved or deleted. “${suggestion.name}” on ${suggestion.pageName} looks like it. Press Link haptic to link it again.`
                    : "The linked card was deleted. Link another card or remove the link."}
              </p>
              <button type="button" className="secondary" onClick={onUnlink} disabled={busy}>
                Remove link
              </button>
            </div>
          )}

          <section className="link-section">
            <h3>Haptic</h3>
            <CardList cards={cards} selectedId={cardId} onSelect={setCardId} />
          </section>

          <section className="link-section">
            <h3>Plays</h3>
            <Segmented
              label="When it plays"
              value={trigger}
              options={TRIGGERS.map((value) => ({ value, label: TRIGGER_LABEL[value] }))}
              onChange={setTrigger}
            />
          </section>

          <div className="editor-footer">
            <button
              type="button"
              className="primary"
              disabled={!chosen || busy}
              onClick={() => chosen && onLink(chosen.id, trigger)}
            >
              Link haptic
            </button>
          </div>
        </>
      )}
    </div>
  );
}

function CardList({
  cards,
  selectedId,
  onSelect,
}: {
  cards: CardSummary[] | null;
  selectedId: string | null;
  onSelect: (id: string) => void;
}) {
  if (cards === null) return <p className="muted">Finding cards…</p>;
  if (cards.length === 0) {
    return <p className="muted">No haptic cards in this file yet. Clear the selection to make one.</p>;
  }
  return (
    <div className="card-list" role="radiogroup" aria-label="Haptic card">
      {cards.map((card) => (
        <button
          key={card.id}
          type="button"
          role="radio"
          aria-checked={card.id === selectedId}
          className="card-option"
          onClick={() => onSelect(card.id)}
        >
          <span className="card-dot" style={{ background: TONE_HEX[card.tone] }} />
          <span className="card-option-name">{card.name}</span>
          <span className="card-option-page">{card.pageName}</span>
        </button>
      ))}
    </div>
  );
}
