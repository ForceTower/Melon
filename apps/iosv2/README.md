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
    Package.swift        swiftLanguageModes: [.v6], depends on swift-composable-architecture
    Sources/UNESKit/
      Domain/            pure Swift — no networking
        Models/          domain models (Profile, …) — what reducers and views speak
        Repositories/    repository interfaces as TCA dependency clients (+ test/preview values)
      Data/              implements the Domain interfaces
        DTO/             Codable wire models (mirror packages/shared-types) + mapping to domain
        Network/         APIClient (URLSession data source) + APIError
        Repositories/    live repository values (liveValue) — HTTP + DTO→domain mapping
      Features/          UI — one folder per tab: <Feature>Feature.swift (@Reducer) + <Feature>View.swift
      App/               AppFeature (root @Reducer) + AppView (tab shell) + RootView (public entry)
      Components/        shared, design-agnostic views
      DesignSystem/      AppTheme today; real color/type/motion tokens drop in here with the first design
    Tests/UNESKitTests/  TestStore-based tests
```

## Layering

`Data → Domain ← Features`. It's a single target, so the boundaries are by
convention (folders), not the compiler:

- **Domain** — models + repository interfaces. A repository is a TCA *dependency
  client*: a `struct` of `async` closures (not a protocol/class), registered on
  `DependencyValues`. Its interface and `testValue`/`previewValue` live here.
- **Data** — the `liveValue` for each repository, the `APIClient` HTTP data
  source, DTOs, and DTO→domain mapping. Nothing here leaks upward.
- **Features (UI)** — reducers reach repositories via `@Dependency`, never the
  `APIClient` or DTOs directly. Repositories are called *directly from reducers*
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

The connected tabs are still minimal placeholder reducers; per-feature data
wiring and the Home v2 redesign layer on top of this shell next.
