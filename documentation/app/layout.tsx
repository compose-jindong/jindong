import "./main.css";
import { RootProvider } from "fumadocs-ui/provider";
import type { Metadata } from "next";
import { Geist, JetBrains_Mono } from "next/font/google";
import type { ReactNode } from "react";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const jetbrainsMono = JetBrains_Mono({
  variable: "--font-jetbrains-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: {
    template: "%s | Jindong",
    default: "Jindong - Declarative Haptic Feedback for KMP",
  },
  description:
    "A declarative haptic feedback library for Kotlin Multiplatform using Compose Runtime",
  icons: {
    icon: "/logo.png",
    shortcut: "/logo.png",
    apple: "/logo.png",
  },
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className={`${geistSans.variable} ${jetbrainsMono.variable} antialiased`}>
        <RootProvider>{children}</RootProvider>
      </body>
    </html>
  );
}
