"use client";

import Link from "next/link";
import Image from "next/image";
import { ThemeToggle } from "@/components/theme-toggle";
import { motion, useScroll, useTransform } from "framer-motion";
import { ArrowRight, Code2, Heart, Sparkles, Layers, Zap, Lightbulb, Rocket, Blocks, Activity, Waves } from "lucide-react";
import { cn } from "@/lib/utils";

// --- Components ---

function Hero() {
  const { scrollY } = useScroll();
  const y1 = useTransform(scrollY, [0, 500], [0, 200]);
  const y2 = useTransform(scrollY, [0, 500], [0, -150]);
  
  return (
    <div className="relative isolate overflow-hidden">
      {/* Background Effects */}
      <div className="absolute inset-0 -z-10 h-full w-full bg-[linear-gradient(to_right,#80808012_1px,transparent_1px),linear-gradient(to_bottom,#80808012_1px,transparent_1px)] bg-[size:24px_24px] [mask-image:radial-gradient(ellipse_60%_50%_at_50%_0%,#000_70%,transparent_100%)] dark:bg-[linear-gradient(to_right,#ffffff08_1px,transparent_1px),linear-gradient(to_bottom,#ffffff08_1px,transparent_1px)]" />
      
      <div className="absolute left-1/2 top-0 -z-10 -translate-x-1/2 blur-3xl xl:-top-6" aria-hidden="true">
        <div 
          className="aspect-[1155/678] w-[72.1875rem] bg-gradient-to-tr from-teal-500 to-lime-500 opacity-20" 
          style={{clipPath: "polygon(74.1% 44.1%, 100% 61.6%, 97.5% 26.9%, 85.5% 0.1%, 80.7% 2%, 72.5% 32.5%, 60.2% 62.4%, 52.4% 68.1%, 47.5% 58.3%, 45.2% 34.5%, 27.5% 76.7%, 0.1% 64.9%, 17.9% 100%, 27.6% 76.8%, 76.1% 97.7%, 74.1% 44.1%)"}}
        />
      </div>

      <div className="container relative mx-auto flex min-h-[90vh] flex-col items-center justify-center px-4 py-24 text-center sm:py-32">
        <motion.div 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="mb-8 flex items-center gap-2 rounded-full border border-teal-500/30 bg-teal-500/10 px-4 py-1.5 text-sm font-medium text-teal-600 backdrop-blur-md dark:border-teal-400/20 dark:bg-teal-900/20 dark:text-teal-300"
        >
          <span className="h-2 w-2 rounded-full bg-teal-500 animate-pulse" />
          <span>Built with Compose Runtime</span>
        </motion.div>

        <motion.h1 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.1 }}
          className="max-w-4xl bg-gradient-to-br from-teal-900 via-teal-800 to-teal-500 bg-clip-text text-5xl font-bold tracking-tight text-transparent pb-6 leading-normal dark:from-white dark:via-teal-100 dark:to-teal-400 sm:text-7xl"
        >
          Declarative Haptics for <br />
          <span className="text-gradient">Kotlin Multiplatform</span>
        </motion.h1>
        
        <motion.p 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.2 }}
          className="mt-6 max-w-2xl text-lg leading-relaxed text-muted-foreground sm:text-xl"
        >
          Vibe with Jindong<br />
          Powered by Compose Runtime
        </motion.p>
        
        <motion.div 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.3 }}
          className="mt-10 flex flex-col items-center gap-4 w-full"
        >
          {/* Install Command - Full Width on Mobile, Auto on Desktop */}
          <div className="group relative w-full max-w-2xl rounded-xl bg-gradient-to-b from-zinc-200 to-zinc-50 p-[1px] dark:from-zinc-700 dark:to-zinc-900">
             <div className="relative flex h-full w-full items-center justify-between rounded-xl bg-white px-6 py-4 dark:bg-zinc-950">
                <code className="font-mono text-sm text-zinc-600 dark:text-zinc-400 whitespace-nowrap overflow-x-auto no-scrollbar">
                  <span className="text-purple-600 dark:text-purple-400">implementation</span>(
                  <span className="text-lime-600 dark:text-lime-400">"io.github.compose-jindong:jindong:&lt;version&gt;"</span>
                  )
                </code>
                <button 
                  className="ml-3 shrink-0 rounded p-2 text-zinc-500 hover:bg-zinc-100 dark:hover:bg-zinc-800"
                  onClick={() => navigator.clipboard.writeText('implementation("io.github.compose-jindong:jindong:<version>")')}
                >
                  <CopyIcon />
                </button>
             </div>
          </div>

          <div className="flex gap-4 mt-4">
            <Link
              href="/docs/guide/quick-start"
              prefetch={true}
              className="group relative inline-flex h-12 items-center justify-center overflow-hidden rounded-xl bg-teal-600 px-8 font-medium text-white transition-all hover:bg-teal-500 hover:shadow-[0_0_40px_-10px_rgba(13,148,136,0.6)] dark:bg-teal-500 dark:hover:bg-teal-400"
            >
              <span className="mr-2">Get Started</span>
              <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
              <div className="absolute inset-0 -z-10 bg-gradient-to-r from-transparent via-white/20 to-transparent opacity-0 transition-opacity group-hover:animate-shine" />
            </Link>
            
            <Link
              href="/docs/api/core/jindong"
               className="inline-flex h-12 items-center justify-center rounded-xl border border-zinc-200 bg-white px-8 font-medium text-zinc-900 transition-all hover:bg-zinc-50 dark:border-zinc-800 dark:bg-zinc-950 dark:text-zinc-100 dark:hover:bg-zinc-900"
            >
              API Reference
            </Link>
          </div>
        </motion.div>

        {/* Floating Elements (Parallax) */}
        <motion.div style={{ y: y1 }} className="absolute left-[5%] top-[20%] hidden xl:block">
           <div className="backdrop-blur-xl bg-white/5 dark:bg-black/5 border border-white/10 dark:border-white/5 rotate-[-6deg] rounded-2xl p-4 shadow-2xl">
              <Activity className="h-8 w-8 text-teal-400" />
           </div>
        </motion.div>
        
        <motion.div style={{ y: y2 }} className="absolute right-[5%] top-[30%] hidden xl:block">
           <div className="backdrop-blur-xl bg-white/5 dark:bg-black/5 border border-white/10 dark:border-white/5 rotate-[12deg] rounded-2xl p-4 shadow-2xl">
              <Waves className="h-8 w-8 text-lime-400" />
           </div>
        </motion.div>
      </div>
    </div>
  );
}

function FeatureCard({ title, description, icon: Icon, delay }: { title: string, description: string, icon: any, delay: number }) {
  return (
    <motion.div 
      initial={{ opacity: 0, y: 20 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      transition={{ delay, duration: 0.5 }}
      whileHover={{ y: -5 }}
      className="group relative overflow-hidden rounded-3xl border border-zinc-200 bg-white p-8 dark:border-zinc-800 dark:bg-zinc-950/50"
    >
      <div className="mb-4 inline-flex rounded-xl bg-teal-50 p-3 text-teal-600 dark:bg-teal-900/20 dark:text-teal-400">
        <Icon className="h-6 w-6" />
      </div>
      <h3 className="mb-2 text-xl font-bold text-zinc-900 dark:text-zinc-100">{title}</h3>
      <p className="text-zinc-600 dark:text-zinc-400">{description}</p>
      
      {/* Hover Gradient */}
      <div className="pointer-events-none absolute -inset-px opacity-0 transition-opacity duration-300 group-hover:opacity-100">
        <div className="absolute inset-0 bg-gradient-to-br from-teal-500/10 via-transparent to-transparent" />
      </div>
    </motion.div>
  );
}

function CodePreview() {
  return (
    <div className="relative mx-auto max-w-4xl px-4 py-24">
       <div className="mb-12 text-center md:mb-16">
          <h2 className="mb-4 text-3xl font-bold md:text-4xl">
            Simple & Intuitive
          </h2>
          <p className="text-lg text-muted-foreground">
            Haptic patterns that read like the intent.
          </p>
        </div>
       <div className="relative overflow-hidden rounded-2xl border border-zinc-800 bg-[#0A0A0A] shadow-2xl">
          <div className="flex items-center gap-2 border-b border-white/5 bg-white/5 px-4 py-3">
            <div className="flex gap-1.5">
              <div className="h-3 w-3 rounded-full bg-red-500/20" />
              <div className="h-3 w-3 rounded-full bg-yellow-500/20" />
              <div className="h-3 w-3 rounded-full bg-green-500/20" />
            </div>
            <div className="ml-4 text-xs font-medium text-zinc-500">HapticExample.kt</div>
          </div>
          <div className="p-6 font-mono text-sm leading-relaxed text-zinc-300 overflow-x-auto">
             {/* Syntax Highlighted Code */}
             <div className="space-y-1">
                <div className="text-zinc-500">// Define haptic patterns declaratively</div>
                <div>
                   <span className="text-purple-400">var</span> count <span className="text-purple-400">by</span> <span className="text-blue-400">remember</span> {'{'} <span className="text-blue-400">mutableStateOf</span>(<span className="text-orange-400">0</span>) {'}'}
                </div>
                <br />
                <div>
                   <span className="text-teal-400">Jindong</span>(count) {'{'}
                </div>
                <div className="pl-4">
                   <span className="text-teal-400">Haptic</span>(<span className="text-orange-400">100</span>.ms)
                </div>
                <div className="pl-4">
                   <span className="text-teal-400">Delay</span>(<span className="text-orange-400">50</span>.ms)
                </div>
                <div className="pl-4">
                   <span className="text-teal-400">Haptic</span>(<span className="text-orange-400">50</span>.ms, HapticIntensity.STRONG)
                </div>
                <div>{'}'}</div>
                <br />
                <div>
                   <span className="text-teal-400">Button</span>(onClick = {'{'} count++ {'}'}) {'{'}
                </div>
                <div className="pl-4">
                   <span className="text-teal-400">Text</span>(<span className="text-lime-400">"Trigger Haptic"</span>)
                </div>
                <div>{'}'}</div>
             </div>
          </div>
       </div>
    </div>
  );
}

// --- Icons ---
function CopyIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect width="14" height="14" x="8" y="8" rx="2" ry="2" />
      <path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2" />
    </svg>
  );
}

// --- Main Page ---

export function HomePage() {
  return (
    <main className="min-h-screen bg-white text-zinc-950 dark:bg-black dark:text-zinc-50">
      <header className="fixed top-0 z-50 w-full border-b border-zinc-200 bg-white/50 backdrop-blur-xl dark:border-white/5 dark:bg-black/50">
        <div className="container mx-auto flex h-16 items-center justify-between px-4">
          <Link href="/" className="flex items-center gap-2 font-bold text-xl tracking-tight">
            <Image
              src="/logo.png"
              alt="Jindong"
              width={32}
              height={32}
              className="rounded-lg"
            />
            <span>Jindong</span>
          </Link>
          <nav className="flex items-center gap-6">
            <Link href="/docs" className="text-sm font-medium text-zinc-600 transition-colors hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-100">Docs</Link>
            <Link href="https://github.com/compose-jindong/jindong" className="text-sm font-medium text-zinc-600 transition-colors hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-100">GitHub</Link>
            <ThemeToggle />
          </nav>
        </div>
      </header>

      <Hero />
      
      <section className="relative z-10 py-24">
        <div className="container mx-auto px-4">
          <div className="mb-16 text-center">
            <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">Why Jindong?</h2>
            <p className="mt-4 text-lg text-zinc-600 dark:text-zinc-400">Built for the modern mobile development era.</p>
          </div>
          
          <div className="grid gap-8 md:grid-cols-3">
            <FeatureCard 
              title="Declarative DSL" 
              description="Describe haptic patterns with a readable, composable Kotlin DSL."
              icon={Code2}
              delay={0}
            />
            <FeatureCard 
              title="Cross-Platform" 
              description="Single codebase for both Android and iOS haptics."
              icon={Layers}
              delay={0.1}
            />
             <FeatureCard 
              title="Compose Native" 
              description="Built directly on the Compose Runtime for seamless integration."
              icon={Sparkles}
              delay={0.2}
            />
          </div>
        </div>
      </section>

      <CodePreview />
      
      {/* Quick Links Section (Restored Footer/CTA) */}
      <section className="py-20 md:py-28 border-t border-zinc-200 dark:border-white/5 bg-zinc-50/50 dark:bg-zinc-950/30">
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
              className="group rounded-2xl border border-zinc-200 bg-white p-6 transition-all hover:border-teal-400 hover:shadow-lg hover:shadow-teal-500/10 dark:border-zinc-800 dark:bg-zinc-900/50 dark:hover:border-teal-500/50"
            >
              <div className="mb-4 inline-flex rounded-lg bg-teal-50 p-3 text-teal-600 dark:bg-teal-900/20 dark:text-teal-400">
                <Lightbulb className="h-6 w-6" />
              </div>
              <h3 className="mb-2 font-semibold transition-colors group-hover:text-teal-600 dark:group-hover:text-teal-400">
                Thinking Declarative
              </h3>
              <p className="text-sm text-muted-foreground">
                Understand the paradigm behind Jindong.
              </p>
            </Link>
            <Link
              href="/docs/guide/quick-start"
              className="group rounded-2xl border border-zinc-200 bg-white p-6 transition-all hover:border-teal-400 hover:shadow-lg hover:shadow-teal-500/10 dark:border-zinc-800 dark:bg-zinc-900/50 dark:hover:border-teal-500/50"
            >
              <div className="mb-4 inline-flex rounded-lg bg-lime-50 p-3 text-lime-600 dark:bg-lime-900/20 dark:text-lime-400">
                <Rocket className="h-6 w-6" />
              </div>
              <h3 className="mb-2 font-semibold transition-colors group-hover:text-teal-600 dark:group-hover:text-teal-400">
                Quick Start
              </h3>
              <p className="text-sm text-muted-foreground">
                Get up and running in minutes.
              </p>
            </Link>
            <Link
              href="/docs/api/composable-dsl/haptic"
              className="group rounded-2xl border border-zinc-200 bg-white p-6 transition-all hover:border-teal-400 hover:shadow-lg hover:shadow-teal-500/10 dark:border-zinc-800 dark:bg-zinc-900/50 dark:hover:border-teal-500/50"
            >
              <div className="mb-4 inline-flex rounded-lg bg-indigo-50 p-3 text-indigo-600 dark:bg-indigo-900/20 dark:text-indigo-400">
                <Blocks className="h-6 w-6" />
              </div>
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
      
      <footer className="border-t border-zinc-200 py-12 dark:border-white/5">
        <div className="container mx-auto flex flex-col items-center justify-between gap-6 px-4 md:flex-row">
            <div className="flex items-center gap-2">
              <Image
                src="/logo.png"
                alt="Jindong"
                width={24}
                height={24}
                className="rounded-md"
              />
              <span className="font-medium text-zinc-900 dark:text-zinc-100">Jindong (진동)</span>
              <span className="text-muted-foreground">
                — Declarative Haptics
              </span>
            </div>
            <div className="flex gap-6">
              <Link href="https://github.com/compose-jindong/jindong" className="text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-100">
                 <span className="sr-only">GitHub</span>
                 <svg viewBox="0 0 24 24" className="h-5 w-5" fill="currentColor"><path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/></svg>
              </Link>
            </div>
        </div>
      </footer>
    </main>
  );
}
