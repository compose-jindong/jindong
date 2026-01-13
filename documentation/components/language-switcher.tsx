"use client";

import { useState, useRef, useEffect } from "react";
import { useParams, usePathname } from "next/navigation";
import { i18n, languageNames } from "@/lib/i18n";
import { Globe } from "lucide-react";

export function LanguageSwitcher() {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const params = useParams();
  const pathname = usePathname();
  const currentLang = (params.lang as string) || i18n.defaultLanguage;

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const getLocalePath = (locale: string) => {
    // pathname includes basePath, so we need to handle it
    const basePath = "/jindong";
    let path = pathname;

    // Remove basePath prefix if present
    if (path.startsWith(basePath)) {
      path = path.slice(basePath.length) || "/";
    }

    // Remove current locale prefix if present
    for (const lang of i18n.languages) {
      if (path.startsWith(`/${lang}/`) || path === `/${lang}`) {
        path = path.slice(lang.length + 1) || "/";
        break;
      }
    }

    // Add new locale prefix (or none for default)
    if (locale === i18n.defaultLanguage) {
      return `${basePath}${path}`;
    }
    return `${basePath}/${locale}${path}`;
  };

  // Hide switcher if only one language
  if (i18n.languages.length <= 1) return null;

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-1.5 px-2 py-1.5 rounded-md hover:bg-fd-accent text-fd-muted-foreground hover:text-fd-foreground transition-colors"
      >
        <Globe className="size-4" />
        <span className="text-sm">{languageNames[currentLang] || currentLang}</span>
      </button>
      {isOpen && (
        <div className="absolute left-0 bottom-full mb-1 bg-fd-popover border rounded-md shadow-lg min-w-[120px] z-50">
          {i18n.languages.map((lang) => (
            <a
              key={lang}
              href={getLocalePath(lang)}
              className={`block px-3 py-2 text-sm hover:bg-fd-accent transition-colors ${
                lang === currentLang ? "font-medium text-fd-primary" : "text-fd-muted-foreground"
              }`}
              onClick={() => setIsOpen(false)}
            >
              {languageNames[lang] || lang}
            </a>
          ))}
        </div>
      )}
    </div>
  );
}
