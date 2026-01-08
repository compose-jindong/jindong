import Image from "next/image";
import Link from "next/link";

const features = [
  {
    title: "Declarative API",
    description:
      "Define complex haptic patterns with intuitive DSL. Describe what you want, not how to do it.",
    icon: "✨",
  },
  {
    title: "Cross-Platform",
    description:
      "Single API for both Android and iOS. Write once, feel everywhere.",
    icon: "📱",
  },
  {
    title: "Compose Runtime",
    description:
      "Built on Compose Runtime. Natural integration with your existing Compose codebase.",
    icon: "🧩",
  },
  {
    title: "Trigger-based",
    description:
      "Works like LaunchedEffect. Haptics trigger automatically when keys change.",
    icon: "⚡",
  },
];

const codeExample = `// Define haptic patterns declaratively
var count by remember { mutableStateOf(0) }

Jindong(count) {
    Haptic(100.ms)
    Delay(50.ms)
    Haptic(50.ms, HapticIntensity.STRONG)
}

Button(onClick = { count++ }) {
    Text("Trigger Haptic")
}`;

const installCommand = `implementation("io.github.compose-jindong:jindong:<version>")`;

export default function HomePage() {
  return (
    <main className="min-h-screen bg-gradient-to-b from-teal-950 via-background to-background dark:from-teal-950 dark:via-background dark:to-background">
      {/* Header */}
      <header className="sticky top-0 z-50 border-b border-teal-800/30 bg-teal-950/80 backdrop-blur-md">
        <div className="container mx-auto flex h-14 items-center justify-between px-4">
          <Link href="/" className="flex items-center gap-2 text-lg font-bold text-white">
            <Image
              src="/logo.png"
              alt="Jindong"
              width={28}
              height={28}
              className="rounded-md"
            />
            <span>Jindong</span>
          </Link>
          <nav className="flex items-center gap-6">
            <Link
              href="/docs"
              className="text-sm text-teal-200/80 transition-colors hover:text-white"
            >
              Docs
            </Link>
            <Link
              href="https://github.com/compose-jindong/jindong"
              className="text-sm text-teal-200/80 transition-colors hover:text-white"
              target="_blank"
              rel="noopener noreferrer"
            >
              GitHub
            </Link>
          </nav>
        </div>
      </header>

      {/* Hero Section */}
      <section className="relative overflow-hidden border-b border-teal-800/30">
        {/* Background gradient */}
        <div className="hero-gradient absolute inset-0 -z-10" />
        <div className="absolute inset-0 -z-10 bg-[radial-gradient(ellipse_80%_50%_at_50%_-20%,_rgba(8,199,174,0.3),_transparent_60%)]" />

        <div className="container mx-auto px-4 py-24 md:py-32 lg:py-40">
          <div className="mx-auto max-w-3xl text-center">
            {/* Badge */}
            <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-teal-500/30 bg-teal-900/50 px-4 py-1.5 text-sm backdrop-blur-sm">
              <span className="h-2 w-2 rounded-full bg-gradient-to-r from-teal-400 to-mint-400 animate-pulse" />
              <span className="text-teal-200">
                Built with Compose Runtime
              </span>
            </div>

            <h1 className="mb-6 text-4xl font-bold tracking-tight text-white md:text-5xl lg:text-6xl">
              Declarative Haptics for{" "}
              <span className="gradient-text">Kotlin Multiplatform</span>
            </h1>
            <p className="mb-8 text-lg text-teal-100/70 md:text-xl">
              Vibe with Jindong
              <br className="hidden sm:block" />
              Powered by Compose Runtime
            </p>

            {/* Install Command */}
            <div className="mb-8 inline-flex items-center gap-3 rounded-xl border border-teal-700/50 bg-teal-900/60 px-4 py-3 font-mono text-sm shadow-lg backdrop-blur-sm">
              <span className="text-mint-400">$</span>
              <code className="text-teal-100">{installCommand}</code>
              <button
                className="ml-2 rounded-md p-1.5 text-teal-300/60 transition-colors hover:bg-teal-800/50 hover:text-teal-100"
                aria-label="Copy to clipboard"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <rect width="14" height="14" x="8" y="8" rx="2" ry="2" />
                  <path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2" />
                </svg>
              </button>
            </div>

            {/* CTA Buttons */}
            <div className="flex flex-col items-center justify-center gap-4 sm:flex-row">
              <Link
                href="/docs/guide/quick-start"
                className="inline-flex h-12 items-center justify-center rounded-xl bg-gradient-to-r from-teal-500 via-mint-500 to-lime-500 px-8 font-medium text-white shadow-lg shadow-mint-500/30 transition-all hover:shadow-xl hover:shadow-mint-400/40"
              >
                Get Started
              </Link>
              <Link
                href="/docs/api/core/jindong"
                className="inline-flex h-12 items-center justify-center rounded-xl border border-border bg-card px-8 font-medium shadow-sm transition-colors hover:bg-accent"
              >
                API Reference
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* Features Grid */}
      <section className="border-b border-border/50 py-20 md:py-28">
        <div className="container mx-auto px-4">
          <div className="mb-12 text-center md:mb-16">
            <h2 className="mb-4 text-3xl font-bold md:text-4xl">
              Everything You Need
            </h2>
            <p className="text-lg text-muted-foreground">
              A complete solution for haptic feedback in Kotlin Multiplatform.
            </p>
          </div>
          <div className="mx-auto grid max-w-5xl gap-6 md:grid-cols-2">
            {features.map((feature) => (
              <div
                key={feature.title}
                className="group relative overflow-hidden rounded-2xl border border-border bg-card p-6 transition-all hover:border-mint-400 hover:shadow-lg hover:shadow-mint-500/10 dark:hover:border-teal-600"
              >
                <div className="feature-card-gradient absolute inset-0 opacity-0 transition-opacity group-hover:opacity-100" />
                <div className="relative">
                  <div className="mb-4 inline-flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br from-teal-100 to-mint-100 text-2xl dark:from-teal-900/50 dark:to-mint-900/50">
                    {feature.icon}
                  </div>
                  <h3 className="mb-2 text-lg font-semibold">{feature.title}</h3>
                  <p className="text-muted-foreground">{feature.description}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Code Example Section */}
      <section className="border-b border-border/50 py-20 md:py-28">
        <div className="container mx-auto px-4">
          <div className="mx-auto max-w-4xl">
            <div className="mb-12 text-center md:mb-16">
              <h2 className="mb-4 text-3xl font-bold md:text-4xl">
                Simple & Intuitive
              </h2>
              <p className="text-lg text-muted-foreground">
                Haptic patterns that read like the intent.
              </p>
            </div>
            <div className="code-window overflow-hidden rounded-2xl shadow-2xl">
              <div className="code-window-header flex items-center gap-2 px-4 py-3">
                <div className="h-3 w-3 rounded-full bg-teal-500/60" />
                <div className="h-3 w-3 rounded-full bg-mint-500/60" />
                <div className="h-3 w-3 rounded-full bg-lime-500/60" />
                <span className="ml-3 text-sm text-mint-400/80">
                  HapticButton.kt
                </span>
              </div>
              <pre className="overflow-x-auto p-6 text-sm leading-relaxed">
                <code className="text-mint-100">{codeExample}</code>
              </pre>
            </div>
          </div>
        </div>
      </section>

      {/* Quick Links Section */}
      <section className="py-20 md:py-28">
        <div className="container mx-auto px-4">
          <div className="mb-12 text-center md:mb-16">
            <h2 className="mb-4 text-3xl font-bold md:text-4xl">
              Start Building
            </h2>
            <p className="text-lg text-muted-foreground">
              Everything you need to add haptic feedback to your app.
            </p>
          </div>
          <div className="mx-auto grid max-w-4xl gap-6 md:grid-cols-3">
            <Link
              href="/docs/guide/thinking-declarative"
              className="group rounded-2xl border border-border bg-card p-6 transition-all hover:border-mint-300 hover:shadow-lg hover:shadow-mint-500/10 dark:hover:border-mint-600"
            >
              <div className="mb-3 text-2xl">💡</div>
              <h3 className="mb-2 font-semibold transition-colors group-hover:text-mint-600 dark:group-hover:text-mint-400">
                Thinking Declarative
              </h3>
              <p className="text-sm text-muted-foreground">
                Understand the paradigm behind Jindong.
              </p>
            </Link>
            <Link
              href="/docs/guide/quick-start"
              className="group rounded-2xl border border-border bg-card p-6 transition-all hover:border-lime-300 hover:shadow-lg hover:shadow-lime-500/10 dark:hover:border-lime-600"
            >
              <div className="mb-3 text-2xl">🚀</div>
              <h3 className="mb-2 font-semibold transition-colors group-hover:text-lime-600 dark:group-hover:text-lime-400">
                Quick Start
              </h3>
              <p className="text-sm text-muted-foreground">
                Get up and running in minutes.
              </p>
            </Link>
            <Link
              href="/docs/api/composable-dsl"
              className="group rounded-2xl border border-border bg-card p-6 transition-all hover:border-teal-300 hover:shadow-lg hover:shadow-teal-500/10 dark:hover:border-teal-600"
            >
              <div className="mb-3 text-2xl">🧱</div>
              <h3 className="mb-2 font-semibold transition-colors group-hover:text-teal-600 dark:group-hover:text-teal-400">
                Composable DSL
              </h3>
              <p className="text-sm text-muted-foreground">
                Explore the pattern building blocks.
              </p>
            </Link>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-border/50 bg-card/50 py-12">
        <div className="container mx-auto px-4">
          <div className="flex flex-col items-center justify-between gap-6 md:flex-row">
            <div className="flex items-center gap-2">
              <Image
                src="/logo.png"
                alt="Jindong"
                width={24}
                height={24}
                className="rounded-md"
              />
              <span className="font-medium">Jindong</span>
              <span className="text-muted-foreground">
                — Declarative Haptics
              </span>
            </div>
            <div className="flex items-center gap-6 text-sm text-muted-foreground">
              <Link
                href="/docs"
                className="transition-colors hover:text-foreground"
              >
                Documentation
              </Link>
              <Link
                href="https://github.com/user/jindong"
                className="transition-colors hover:text-foreground"
                target="_blank"
                rel="noopener noreferrer"
              >
                GitHub
              </Link>
            </div>
          </div>
          <div className="mt-8 text-center text-sm text-muted-foreground">
            <p>
              <span className="text-teal-600 dark:text-teal-400">진동</span>
              {" "}— Declarative haptic feedback for Kotlin Multiplatform.
            </p>
          </div>
        </div>
      </footer>
    </main>
  );
}
