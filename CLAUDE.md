# Cortex — Project Context for Claude Code

You are picking up an in-progress Android project. Read this fully before doing anything.

## What this app is

Cortex is an Android app that teaches essential algorithms and wealth-thinking frameworks through evidence-based learning: retrieval practice, elaborative encoding, interleaved spaced practice.

**Two tracks:** Algorithms (including lesser-known ones beyond the interview canon — reservoir sampling, Boyer-Moore majority, HyperLogLog, Bloom filters, consistent hashing, CRDTs, etc.) and Wealth (leverage, compound math, asymmetric bets, Kelly, pricing power, unit economics, Munger-style mental models).

**What this app is NOT:** not a get-rich-quick app, not a "secrets" app, not LeetCode-with-a-fresh-coat-of-paint. Push back on any request that drifts toward those framings.

## Architecture

- **Platform:** Android native, Kotlin 2.0, Jetpack Compose, minSdk 26, targetSdk 34, JVM 17
- **Pattern:** MVVM. ViewModel exposes `StateFlow<UiState>`, Screen collects via `collectAsStateWithLifecycle`.
- **DI:** Koin 4.0 (not Hilt — lighter for a solo project)
- **Local DB:** Room + SQLite (user progress only; content is bundled Kotlin)
- **Navigation:** Compose Navigation 2.8 with type-safe `@Serializable` routes
- **No backend.** No network. Offline-first.
- **Teach-back feature using Anthropic API is explicitly deferred to v2.**

## Module structure

```
com.cortex.app/
├── core/         di, database, navigation, ui/{theme,components}
├── data/         local/{entity,dao,content/lessons}, repository
├── domain/       model, repository, usecase, scheduler
└── feature/      home, lesson, library, review, progress
```

## Design system

Dark-only editorial aesthetic. See `core/ui/theme/`:
- **Palette:** Paper `#0E0E10`, Ink `#EDEAE3`, single accent `#D4FF4F` (acid green)
- **Type:** currently system Serif/SansSerif/Mono placeholders; swap to Fraunces/Plus Jakarta Sans/IBM Plex Mono post-MVP
- **Spacing:** 4dp baseline grid via `CortexSpacing` tokens
- **Shapes:** small radii (2–4dp). No rounded-everything.
- No gamification kitsch. No confetti. No XP bars.

## Milestone roadmap

- **M0 (DONE):** project skeleton, theme, nav, Home screen real-designed, placeholders for Library/Review/Progress, one HomeViewModel unit test
- **M1 (NEXT):** domain models, lesson-authoring Kotlin DSL, `LessonCatalog`, `ContentRepository`, `GetLessonUseCase`, generic `LessonScreen` + `LessonViewModel`, author **Two Pointers** end-to-end
- **M2:** Room schema, `ProgressRepository`, lesson-resume
- **M3:** FSRS-4.5 scheduler port, Review screen
- **M4:** Daily session builder + interleaving
- **M5:** Compose Canvas visualization DSL (ArrayPointer, Graph, Tree visuals)
- **M6:** Library + Progress screens
- **M7:** author 15 algorithm + 10 wealth lessons
- **M8:** polish, accessibility, Play Store prep

## Lesson structure (per lesson, v1 = 5 stages; teach-back = v2)

1. **Hook** — real-world problem, stakes
2. **Intuition** — narration + visual, no pseudocode yet
3. **Worked example** — annotated trace, scrubbable
4. **Faded practice** — 3 problems with decreasing scaffolding (Sweller)
5. **Transfer problem** — problem where the algorithm is the solution but isn't named

Each lesson spawns review cards that enter the FSRS queue. Mastery criteria:
- ≥80% on 3 faded-practice problems
- ≥3 spaced repetitions over ≥21 days
- 1 transfer problem solved

## Code conventions

- Domain layer must have zero Android dependencies. Pure Kotlin.
- Don't leak Room entities into domain models. Separate data classes, map at the repository.
- ViewModels never touch DAOs directly; go through Repository → UseCase.
- One ViewModel per screen. No shared ViewModels across screens.
- Koin: use `viewModelOf(::Foo)` for simple cases, `viewModel { (param) -> Foo(param, get()) }` for parameterized.
- All spacing uses `CortexSpacing.*`, never raw dp numbers except in small one-off local adjustments.
- All colors via `MaterialTheme.colorScheme.*` or `CortexColors.*`. No hex in screens.
- Prefer `collectAsStateWithLifecycle()` over `collectAsState()`.

## What's done

- Gradle wiring, version catalog, Compose BOM 2024.10
- `CortexTheme` + full token set
- Koin bootstrapped in `CortexApplication`
- `MainActivity` hosts a single `CortexNavHost`
- `HomeScreen` fully designed (editorial dark theme, TODAY card, MetaTiles, footer)
- `Library / Review / Progress` are `ComingSoonScreen` placeholders with honest milestone labels
- `HomeViewModelTest` with Turbine verifies the test stack works

## Open items for M1

User asked for type-safe lesson authoring. Build this as a Kotlin DSL, not JSON:

```kotlin
val TwoPointers = lesson("two-pointers") {
    track = Track.ALGORITHMS
    tier = Tier.FOUNDATIONS
    title = "Two Pointers"
    hook { problem = "..." ; stakes = "..." }
    intuition { narration("...", "...") ; visual = ArrayPointerVisual(...) }
    workedExample { step("...") ; step("...") }
    fadedPractice { problem(scaffold = 2) { ... } ; ... }
    transferProblem { prompt = "..." ; solution = "..." }
    reviewCards {
        card("invariant", "What's the loop invariant?", "...")
    }
}
```

Canvas visualizations are M5. In M1, leave `VisualSpec` as a sealed class but render a text-only fallback for now.

## User context

Primary user is the developer themselves (Okun, Senior Python Backend Engineer at tappi Inc., Nairobi). React/TypeScript/Python background; Android is the stretch. Keep Kotlin idiomatic and annotate non-obvious idioms briefly in comments. Don't over-explain things they'd know from any stack (HTTP, SQL, etc.).

## When in doubt

Prefer shipping 1 excellent lesson over 5 mediocre ones. Content volume is the #1 project risk. Architecture should pay for itself after ~5 lessons authored with the DSL.
