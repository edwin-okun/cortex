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

- ✅ **M0:** project skeleton, `CortexTheme`, Koin bootstrap, `HomeScreen` designed, `CortexNavHost`, placeholder screens, test stack verified
- ✅ **M1:** domain models (`Lesson`, `LessonStage`, `Track`, `Tier`, `VisualSpec`), Kotlin DSL (`LessonDsl.kt`), `ContentRepositoryImpl`, `GetLessonUseCase`, `LessonScreen` + `LessonViewModel`, Two Pointers authored end-to-end
- ✅ **M2:** Room v1 schema (`LessonProgressEntity`, `PracticeAttemptEntity`), `ProgressRepositoryImpl`, lesson-resume from saved stage, `PracticeAttempt` tracking
- ✅ **M3:** FSRS-6 scheduler (`Fsrs.kt`, 21-param `FsrsParameters`), Room v2 migration (`ReviewCardEntity`), `SchedulerRepositoryImpl`, Review screen (`ReviewViewModel` + `ReviewScreen`), HomeViewModel wired to due count
- **M4 (NEXT):** Daily session builder + interleaving
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

### M0 — Skeleton
Gradle wiring, version catalog, Compose BOM 2024.10. `CortexTheme` + full token set (`CortexColors`, `CortexSpacing`, `CortexTypography`). Koin bootstrapped in `CortexApplication`. `MainActivity` hosts a single `CortexNavHost`. `HomeScreen` fully designed (editorial dark theme, TODAY card, MetaTiles, footer). `Library / Review / Progress` are `ComingSoonScreen` placeholders. `HomeViewModelTest` with Turbine verifies the test stack.

### M1 — Lesson engine
Domain models: `Lesson`, `LessonStage` (sealed: Hook, Intuition, WorkedExample, FadedPractice, TransferProblem), `Track`, `Tier`, `VisualSpec` (sealed, text fallback only — Canvas is M5), `LessonReviewCard`. Kotlin DSL (`LessonDsl.kt`) with builder pattern. `ContentRepositoryImpl` backed by `LessonCatalog` (in-memory). `GetLessonUseCase`. `LessonScreen` + `LessonViewModel` (stage paging, `SavedStateHandle`). **Two Pointers** authored end-to-end as the reference lesson.

### M2 — Progress persistence
Room v1: `LessonProgressEntity` + `LessonProgressDao`, `PracticeAttemptEntity` + `PracticeAttemptDao`. `ProgressRepositoryImpl` with upsert-on-advance semantics: `startedAt` set on first advance, `masteredAt` set when `newStage == totalStages` and never overwritten. Lesson resume reads `currentStage` from DB on init. `ProgressRepositoryImplTest` (6 tests, Robolectric + Turbine).

### M3 — FSRS scheduler + Review screen
**Scheduler:** FSRS-6 (21 params, py-fsrs reference vectors validated). Pure Kotlin, zero Android deps. `FsrsParameters.Default` uses empirically-optimised weights (100M+ reviews). `Fsrs.schedule()` covers Learning → Review → Relearning state machine. 8 unit tests including 2 exact reference vector checks. **Persistence:** Room v2 migration adds `review_cards` table (`ReviewCardEntity`). `SchedulerRepositoryImpl` injectable `Clock` for test determinism; `seedCardsForLesson` is idempotent. **Review screen:** `ReviewViewModel` loads queue once on init (in-memory, no mid-session flicker), shows FSRS preview intervals on grade buttons. `ReviewScreen` with progress bar, tap-to-reveal, 4 grade buttons (Again/Hard/Good/Easy). `HomeViewModel` wired to `observeDueCount()`. 7 integration tests (`SchedulerRepositoryImplTest`).

## Lesson authoring DSL

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

Canvas visualizations are M5. `VisualSpec` is a sealed class; `LessonScreen` renders a text-only fallback until then.

## Architectural decisions log

- **FSRS-6 over 4.5:** 21 params vs 17; matches current py-fsrs/Anki standard; verified test vectors available. Trainable decay `w[20]=0.1542` instead of fixed `-0.5`.
- **`LessonReviewCard` vs `ReviewCard`:** Authored DSL type renamed to `LessonReviewCard` to avoid collision with the runtime `ReviewCard` domain model (FSRS state + scheduling fields).
- **`step = -1` sentinel in `ReviewCardEntity`:** Room can't store nullable `Int` without a wrapper; `-1` means "graduated to Review state" in the entity layer; mappers convert to `Int?` at the domain boundary.
- **In-memory review queue in `ReviewViewModel`:** Loads `observeDueCards().first()` once on init. Prevents the current card from disappearing if DB updates mid-session (e.g., background clock tick).
- **Injectable `Clock` in `SchedulerRepositoryImpl`:** `kotlinx.datetime.Clock` injected (defaults to `Clock.System`) so tests can pass a fixed `Instant` without mocking system time.
- **`stopKoin()` in Robolectric `@After`:** Robolectric spins up `CortexApplication` which calls `startKoin()`; without `stopKoin()` in teardown, successive test methods throw `KoinApplicationAlreadyStartedException`.
- **`observeDueCount()` is a point-in-time snapshot:** The query passes `clock.now()` at subscription time, not a live clock. Cards become "newly due" only when the user re-opens the app and a new subscription is created. Acceptable for M3; revisit in M4.

## Known issues / tech debt

- `observeDueCount()` / `observeDueCards()` snapshot `now` at subscription time — doesn't react as cards cross their `dueAt` threshold while the app is open. Fix in M4 when building the daily session builder (pass a periodic ticker or use `conflate + delay`).
- `VisualSpec` subtypes (`ArrayPointerVisual`, etc.) are defined but all render the same text fallback in `LessonScreen`. Proper Canvas rendering is M5.
- `Library` and `Progress` screens are `ComingSoonScreen` stubs. M6 work.
- No migration test — Room migration correctness is asserted only at runtime. Consider adding a `MigrationTest` before v2→v3.
- FSRS weights are hardcoded defaults. User-specific weight training (from review history) is not implemented; deferred to post-MVP.

## File map

```
app/src/main/java/com/cortex/app/
│
├── CortexApplication.kt          Koin init
├── MainActivity.kt               single-Activity host
│
├── core/
│   ├── database/
│   │   └── CortexDatabase.kt     Room DB, v2, MIGRATION_1_2
│   ├── di/
│   │   └── AppModule.kt          Koin module (DB, DAOs, repos, use cases, ViewModels)
│   ├── navigation/
│   │   ├── CortexRoute.kt        @Serializable route sealed class
│   │   └── CortexNavHost.kt      NavHost + screen wiring
│   └── ui/
│       ├── components/
│       │   ├── ComingSoonScreen.kt
│       │   └── CortexTopBar.kt
│       └── theme/
│           ├── Color.kt          CortexColors palette
│           ├── Spacing.kt        CortexSpacing tokens
│           ├── Theme.kt          CortexTheme MaterialTheme wrapper
│           └── Typography.kt     type scale
│
├── data/
│   ├── local/
│   │   ├── content/
│   │   │   ├── LessonDsl.kt      Kotlin DSL builders (lesson {}, reviewCards {}, etc.)
│   │   │   └── lessons/
│   │   │       └── TwoPointers.kt  reference lesson, fully authored
│   │   ├── dao/
│   │   │   ├── LessonProgressDao.kt
│   │   │   ├── PracticeAttemptDao.kt
│   │   │   └── ReviewCardDao.kt
│   │   └── entity/
│   │       ├── LessonProgressEntity.kt
│   │       ├── PracticeAttemptEntity.kt
│   │       └── ReviewCardEntity.kt  step=-1 sentinel for Review state
│   └── repository/
│       ├── ContentRepositoryImpl.kt  in-memory LessonCatalog
│       ├── ProgressRepositoryImpl.kt upsert-on-advance, masteredAt guard
│       └── SchedulerRepositoryImpl.kt  FSRS grading, idempotent seed, injectable Clock
│
├── domain/
│   ├── model/
│   │   ├── Lesson.kt             Lesson + LessonStage sealed class + LessonReviewCard
│   │   ├── LessonProgress.kt
│   │   ├── PracticeAttempt.kt
│   │   ├── ReviewCard.kt         runtime card with full FSRS state
│   │   ├── Tier.kt
│   │   ├── Track.kt
│   │   └── VisualSpec.kt         sealed (text fallback until M5)
│   ├── repository/
│   │   ├── ContentRepository.kt
│   │   ├── ProgressRepository.kt
│   │   └── SchedulerRepository.kt
│   ├── scheduler/
│   │   ├── CardState.kt          Learning | Review | Relearning
│   │   ├── Fsrs.kt               FSRS-6 object (schedule, nextDue, retrievability)
│   │   ├── FsrsCard.kt           pure Kotlin card state
│   │   ├── FsrsParameters.kt     21 weights, Default companion
│   │   └── Rating.kt             Again | Hard | Good | Easy
│   └── usecase/
│       └── GetLessonUseCase.kt
│
└── feature/
    ├── home/
    │   ├── HomeScreen.kt         TODAY card, MetaTiles, due-count badge
    │   └── HomeViewModel.kt      combines progress + due count
    ├── lesson/
    │   ├── LessonScreen.kt       stage pager (Hook→Transfer)
    │   └── LessonViewModel.kt    stage advance, seeds FSRS cards on completion
    ├── library/
    │   ├── LibraryScreen.kt      ComingSoonScreen (M6)
    │   └── LibraryViewModel.kt
    ├── progress/
    │   ├── ProgressScreen.kt     ComingSoonScreen (M6)
    │   └── ProgressViewModel.kt
    └── review/
        ├── ReviewScreen.kt       progress bar, tap-to-reveal, 4 grade buttons
        └── ReviewViewModel.kt    in-memory queue, FSRS preview intervals

app/src/test/java/com/cortex/app/
├── domain/scheduler/
│   └── FsrsTest.kt              8 tests incl. 2 py-fsrs reference vectors
├── data/repository/
│   ├── ProgressRepositoryImplTest.kt  6 Robolectric integration tests
│   └── SchedulerRepositoryImplTest.kt 7 Robolectric integration tests
└── feature/
    ├── home/
    │   └── HomeViewModelTest.kt
    └── lesson/
        └── LessonViewModelTest.kt
```

## User context

Primary user is the developer themselves (Okun, Senior Python Backend Engineer at tappi Inc., Nairobi). React/TypeScript/Python background; Android is the stretch. Keep Kotlin idiomatic and annotate non-obvious idioms briefly in comments. Don't over-explain things they'd know from any stack (HTTP, SQL, etc.).

## When in doubt

Prefer shipping 1 excellent lesson over 5 mediocre ones. Content volume is the #1 project risk. Architecture should pay for itself after ~5 lessons authored with the DSL.
