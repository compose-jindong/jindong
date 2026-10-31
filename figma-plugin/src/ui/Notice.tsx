/** A short explanation in place of a screen, such as what to select. */
export function Notice({ children }: { children: string }) {
  return <p className="notice">{children}</p>;
}
