import "./globals.css";
import { RootProvider } from "fumadocs-ui/provider";
import type { Metadata } from "next";
import type { ReactNode } from "react";

export const metadata: Metadata = {
  title: {
    template: "%s | Jindong",
    default: "Jindong - Declarative Haptic Feedback for KMP",
  },
  description:
    "A declarative haptic feedback library for Kotlin Multiplatform using Compose Runtime",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body>
        <RootProvider>{children}</RootProvider>
      </body>
    </html>
  );
}
