# Cortex

An Android app that teaches essential algorithms and wealth-thinking frameworks through evidence-based learning: retrieval practice, elaborative encoding, interleaved spaced practice.

**Two tracks:** Algorithms (two pointers, reservoir sampling, Boyer-Moore majority, HyperLogLog, Bloom filters, consistent hashing, CRDTs, etc.) and Wealth (leverage, compound math, Kelly criterion, pricing power, Munger-style mental models).

## Stack

- **Platform:** Android native, Kotlin 2.0, Jetpack Compose
- **DI:** Koin 4.0
- **DB:** Room + SQLite (progress only; content is bundled Kotlin)
- **Navigation:** Compose Navigation 2.8 (type-safe routes)
- **No backend. No network. Offline-first.**

## Architecture

```
com.cortex.app/
├── core/         di, database, navigation, ui/theme+components
├── data/         local/{entity,dao,content/lessons}, repository
├── domain/       model, repository, usecase, scheduler
└── feature/      home, lesson, library, review, progress
```

MVVM throughout. ViewModels expose `StateFlow<UiState>`, screens collect via `collectAsStateWithLifecycle`. Domain layer has zero Android dependencies.

## Lesson structure (v1)

Each lesson has 5 stages:
1. **Hook** — real-world problem + stakes
2. **Intuition** — narration + visual placeholder (Canvas in M5)
3. **Worked example** — annotated trace
4. **Faded practice** — 3 problems with decreasing scaffolding
5. **Transfer problem** — apply the pattern without being told what it is

Lessons are authored as type-safe Kotlin DSL objects in `data/local/content/lessons/`.

## Milestone status

| Milestone | Status | Scope |
|-----------|--------|-------|
| M0 | ✅ Done | Skeleton, theme, nav, Home screen |
| M1 | ✅ Done | Domain models, lesson DSL, Two Pointers end-to-end, LessonScreen |
| M2 | ✅ Done | Room schema, ProgressRepository, lesson resume |
| M3 | ⬜ Next | FSRS-4.5 scheduler port, Review screen |
| M4 | ⬜ | Daily session builder + interleaving |
| M5 | ⬜ | Compose Canvas visualizations |
| M6 | ⬜ | Library + Progress screens |
| M7 | ⬜ | Author 15 algorithm + 10 wealth lessons |
| M8 | ⬜ | Polish, accessibility, Play Store prep |

## Running the project

Requires Android Studio (Ladybug or later) with JDK 17.

```bash
# Build and install debug APK
./gradlew installDebug

# Run unit tests
./gradlew testDebugUnitTest
```

> **Note:** `gradlew` must be generated if missing: open the project in Android Studio and it will scaffold it, or run `gradle wrapper` with Gradle 8.9 on your PATH.

## Design system

Dark-only editorial aesthetic. No gamification. No confetti. No XP bars.

- **Background:** `#0E0E10` (Paper)
- **Text:** `#EDEAE3` (Ink)
- **Accent:** `#D4FF4F` (acid green)
- Typography placeholders: Fraunces / Plus Jakarta Sans / IBM Plex Mono (post-MVP swap)
