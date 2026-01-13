import type { ReactNode } from "react";
import { RootProvider } from "fumadocs-ui/provider";
import { i18n } from "@/lib/i18n";
import { notFound } from "next/navigation";

export default async function LangLayout({
  params,
  children,
}: {
  params: Promise<{ lang: string }>;
  children: ReactNode;
}) {
  const { lang } = await params;

  if (!i18n.languages.includes(lang)) {
    notFound();
  }

  return (
    <RootProvider i18n={{ locale: lang }}>{children}</RootProvider>
  );
}

export function generateStaticParams(): { lang: string }[] {
  // Generate params for all non-default languages
  return i18n.languages
    .filter((lang: string) => lang !== i18n.defaultLanguage)
    .map((lang: string) => ({ lang }));
}
