# UNES iOS (v2)

The redesigned, **fully native** iOS app built with [The Composable Architecture](https://github.com/pointfreeco/swift-composable-architecture)
and the **Swift 6 language mode with complete strict concurrency**. This replaces
the KMP-backed `apps/ios` — the data layer is reimplemented in Swift against
`apps/api` so every native integration (Widgets, Live Activities, Siri / App
Intents, notifications) can link plain Swift instead of a shared XCFramework.

## Architecture

```
apps/iosv2/
  UNES.xcodeproj        thin app shell (Swift 6 + complete concurrency, MainActor default isolation)
  UNES/UNESApp.swift    @main App — imports UNESKit and hosts RootView()
  UNESKit/              local Swift package — all feature code lives here
    Package.swift        swiftLanguageModes: [.v6], depends on swift-composable-architecture + GRDB
    Sources/UNESKit/
      Domain/            pure Swift — no networking
        Models/          domain models (Profile, …) — what reducers and views speak
        Repositories/    repository interfaces as TCA dependency clients (+ test/preview values)
      Data/              implements the Domain interfaces
        DTO/             Codable wire models (mirror packages/shared-types) + mapping to domain
        Network/         APIClient (URLSession data source) + APIError
        Database/        GRDB SQLite mirror — schema (AppDatabase), records, SemesterSnapshot + mappings, MirrorStore
        Repositories/    live repository values (liveValue) — HTTP + mirror + DTO→domain mapping
      Features/          UI — one folder per tab: <Feature>Feature.swift (@Reducer) + <Feature>View.swift
      App/               AppFeature (root @Reducer) + AppView (tab shell) + RootView (public entry)
      Components/        shared, design-agnostic views
      DesignSystem/      AppTheme today; real color/type/motion tokens drop in here with the first design
    Tests/UNESKitTests/  TestStore-based tests
```

## Layering

`Data → Domain ← Features`. It's a single target, so the boundaries are by
convention (folders), not the compiler:

- **Domain** — models + repository interfaces. A repository is a TCA _dependency
  client_: a `struct` of `async` closures (not a protocol/class), registered on
  `DependencyValues`. Its interface and `testValue`/`previewValue` live here.
- **Data** — the `liveValue` for each repository, the `APIClient` HTTP data
  source, DTOs, and DTO→domain mapping. Nothing here leaks upward.
- **Features (UI)** — reducers reach repositories via `@Dependency`, never the
  `APIClient` or DTOs directly. Repositories are called _directly from reducers_
  (the reducer is the interactor); a use-case layer is added only where logic
  spans multiple repositories.

The `Me` tab is the reference vertical slice: `MeFeature` → `ProfileRepository`
→ `APIClient` + `ProfileDTO` → `Profile`.

**Why a local package?** All feature code + tests live in `UNESKit` so the app
target never imports TCA directly — ComposableArchitecture is linked exactly once
(in the package). This also lets the package compile with plain (nonisolated)
default isolation, which keeps reducers idiomatic (`var body: some ReducerOf<Self>`)
and sidesteps a `@Reducer` + default-MainActor compiler issue. The app shell keeps
`MainActor` default isolation for its SwiftUI code. Both build under Swift 6 /
complete concurrency.

## Build & test

```sh
# Build the app for a simulator
xcodebuild build -scheme UNES -destination 'platform=iOS Simulator,name=iPhone 17 Pro'

# Run the feature tests (fast — runs natively on the macOS host)
cd UNESKit && swift test
```

## Status

Onboarding v2 is implemented end to end: `RootFeature` gates between the
onboarding flow and the tab shell based on the Keychain session, and
`OnboardingFeature` drives splash → welcome → intro carousel → login →
sync → ready on a native `NavigationStack` (value-routed `StackState`).

- **DesignSystem/** carries the first real tokens from the v2 design:
  `UNESColor` (adaptive neutrals + fixed brand palette), `UNESMotion`
  (the `cubic-bezier(.2,.9,.3,x)` entrance family), the animated `MeshView`
  gradient, entrance modifiers, and the filled `UNESButtonStyle`.
- **Auth data layer**: `AuthRepository` (SAGRES credentials + passkey via
  `ASAuthorizationController`), Keychain-backed `SessionStore`, and the
  envelope-aware `APIClient` with bearer injection.
- **Initial sync**: `SyncFeature` runs the six-step first sync against
  `api/sync/onboarding-status` (phase-1 terminal gate + applied-semesters
  gate), then snapshots the active semester for the Ready screen.
- **Home v2** (`Features/Home`): the "Hoje" tab from the v2 design — native
  collapsing large title (accent date eyebrow as the first content line,
  avatar in the toolbar), mesh hero with a live countdown to the next class,
  widget grid (coefficient sparkline, attendance ring, next exam, messages),
  the "Seu dia" list with a live now-line, and the Turmas carousel. Data is
  stale-while-revalidate over a local **SQLite mirror** (GRDB, `Data/Database`):
  on `.task` the reducer hydrates instantly from the mirror (no spinner when
  data exists) and kicks a network refresh — the active
  `api/sync/semesters/:id` payload transactionally replaces that semester's
  rows (upstream deletions disappear locally), the first `api/sync/messages`
  page is upserted by id, and both the wire and DB paths fold into the same
  `HomeOverview` mapping through `SemesterSnapshot`. The persisted
  `lastSyncedAt` drives the "Atualizado há X min" footer across relaunches,
  and an offline refresh (pull or hero rollover) recomputes hero/"Seu dia"
  from mirrored data so the screen still advances with time.

- **Turmas v2** (`Features/Disciplines`): the disciplines tab — semester
  scorecard hero (partial mean, last-grade delta, staggered equalizer bars,
  attention line), one card per discipline (grade ring, evaluation chips,
  absence bar, next-evaluation countdown), and the Histórico section with
  collapsible past semesters plus on-demand "Baixar" cards that pull
  `api/sync/semesters/:id` for semesters not mirrored yet. Reuses the Home
  mirror through `DisciplinesRepository` (same stale-while-revalidate shape);
  multi-group disciplines (theory + practice) merge into one card — grades
  dedup by upstream id and totalFaltas reads first-non-null per offer, since
  the backend replicates both onto every group row.

The remaining tabs (Horário, Mensagens, Eu) are still minimal placeholder
reducers; day rows and discipline cards value-route to a placeholder detail
until the discipline detail screen ships.
