import { DocsLayout } from "fumadocs-ui/layouts/docs";
import type { ReactNode } from "react";
import { baseOptions } from "@/app/layout.config";
import { source } from "@/lib/source";
import { i18n } from "@/lib/i18n";
import { LanguageSwitcher } from "@/components/language-switcher";

export default function Layout({ children }: { children: ReactNode }) {
  return (
    <DocsLayout
      tree={source.pageTree[i18n.defaultLanguage]}
      {...baseOptions}
      sidebar={{ footer: <LanguageSwitcher /> }}
    >
      <div className="relative isolate min-h-screen">
        <div className="absolute inset-0 -z-10 h-full w-full bg-[linear-gradient(to_right,#80808012_1px,transparent_1px),linear-gradient(to_bottom,#80808012_1px,transparent_1px)] bg-[size:24px_24px] [mask-image:radial-gradient(ellipse_60%_50%_at_50%_0%,#000_70%,transparent_100%)] dark:bg-[linear-gradient(to_right,#ffffff08_1px,transparent_1px),linear-gradient(to_bottom,#ffffff08_1px,transparent_1px)] opacity-50 pointer-events-none fixed" />
        {children}
      </div>
    </DocsLayout>
  );
}
