# Melon Agent Instructions

## Overall

- This repo holds the client side of UNES: iOS, Android, the marketing site, and the shared KMP module. The backend lives in a separate, private repo.
- Code readability matters most.
- ALWAYS read and understand relevant files before proposing edits. Do not speculate about code you have not inspected.

## Tooling

- `mise` is used for tool version management (see `.mise.toml`)
- `bun` is used for Node package management and as the script runner (we do NOT use `npm`, `yarn`, or `pnpm`)
- `bunx` must be used in-place of `npx`
- `oxlint` is used for linting, `oxfmt` is used for formatting (we do NOT use `prettier`, `eslint` or anything else)
- `gradle` is used for the JVM side (Android app + Kotlin Multiplatform shared package)
- Native iOS is a standard Xcode project (`apps/ios`)

## Development Workflow for TypeScript

All `bun` scripts should be run from the root of the repo.

- `bun install` to install dependencies
- `bun run check` to format-check and lint without applying fixes
- `bun run fix` to format and lint with applying fixes

**Important**: Run `bun run fix` frequently throughout your task to find issues, and always run it after your task completes.

- Do NOT fix formatting issues yourself, use `bun run fix`.
- For TypeScript typechecking, run `bun run --filter '@melon/*' typecheck` or `bun run tsc --noEmit` inside the specific workspace.
- If you need to run a command in a specific package, use `bun run --filter @melon/package-name ...` or cd into the workspace.

**Scope**: `bun run fix` / `bun run check` only operate on the TypeScript side (the bun workspace: `apps/landing`, configs, scripts). If your task touched only Kotlin, Swift, or other non-TS files, do NOT run them — they will do nothing useful and add noise. Run them when, and only when, you have modified files in the bun workspace.

## Development Workflow for Android

The Android side is a Gradle composite build rooted at the repo root. Run `./gradlew` from the repo root (not from `apps/android/`); `settings.gradle.kts` already wires in `build-logic/`, the Android modules, and every `packages/shared-kmp/*` module.

Android Gradle modules:

- `:apps:android:app` — the main app.
- `:apps:android:design-system` — Compose-based design tokens, theme, and shared UI primitives.
- `:apps:android:mvi` — shared MVI scaffolding for feature screens.

Useful commands (run from repo root):

- `./gradlew :apps:android:app:assembleDebug` — build the debug APK.
- `./gradlew :apps:android:app:installDebug` — build + install on a connected device/emulator.
- `./gradlew :apps:android:app:lintDebug` — run the Android Lint task.
- `./gradlew <module>:compileDebugKotlin` — quick compile check for a single module (faster than a full assemble when you just want to know it builds).

Guidance:

- Prefer using Android Studio for builds/runs when possible; reach for `./gradlew` from the CLI for targeted compile checks or CI-style runs.
- Don't run a full `./gradlew build` to validate a small Kotlin change — it's slow and pulls in unrelated modules. Compile the specific module instead.
- After Kotlin/Compose changes, you do NOT need to run `bun run fix` — that's TypeScript only. See the scope note above.
- If you touched `packages/shared-kmp/*`, a Gradle sync (or rerunning the relevant `compile*Kotlin` task) is enough on the Android side. On iOS you don't need to rebuild the XCFramework manually — the Xcode build script rebuilds it as part of the iOS app build.
- Honor the design-system rules below (no hardcoded colors/typography/motion, strings via `stringResource`). Lint won't always catch violations; the rules are enforced by review.

## Development Workflow for iOS

- The project builds with **Swift 6** and `SWIFT_STRICT_CONCURRENCY = complete`. Code that compiles can still crash at runtime when actor-isolation preconditions are violated — write isolation-safe code, don't just satisfy the compiler.
- If you make big changes to iOS or KMP and we're running on macOS try to compile the iOS app.

## Monorepo Structure

Melon (client side) is a polyglot monorepo. The TypeScript side is a bun workspace; the JVM side is a Gradle composite build.

- `apps/ios` — Native iOS app (Swift + SwiftUI), standard Xcode project.
- `apps/android` — Native Android app (Kotlin + Jetpack Compose), Gradle.
- `apps/landing` — Astro marketing site, deployed to Cloudflare Pages.
- `packages/shared-kmp` — Shared business logic via Kotlin Multiplatform, packaged as an XCFramework for iOS and a library for Android. Split into `core/` (database, network), `features/` (auth, dashboard, …), and `umbrella/` (combined build target).
- `build-logic/` — Gradle convention plugins shared across the JVM projects.

The backend (API, scrapers, shared TS types/utils, infra) lives in a separate private repo. Wire-facing shapes consumed by `shared-kmp` are mirrored here manually — if you change a contract on either side, keep both in sync.

## File Organization & Naming

- **TypeScript file naming**: `kebab-case.ts`
- **Export style**: Named exports over default exports for discoverability (enforced by `no-default-export`; config files and a few other patterns are allowed — see `.oxlintrc.json`)
- **TypeScript files**: Use `.ts` for code
- **Swift files**: Follow standard Swift conventions (`PascalCase.swift` for types)
- **Kotlin files**: Follow standard Kotlin conventions (`PascalCase.kt` matching the primary type, `camelCase.kt` for top-level declarations)

## TypeScript & Type Safety

- **Type safety**: Avoid `any` and type casting. Use `zod` for validating unknown data at boundaries.
- **Config files**: Use `.ts` extension for config files when possible.
- **Typing**: Avoid adding explicit types when TypeScript can infer them; only add typings when you're confident inference is insufficient.

### Coding

- When adding new behavior to existing code, don't just apply similar-looking code, you can and should strive to a semantically better looking code.
- Attempt to identify the "root" of some behavior and change the behavior in the source, don't postpone and add layers for fixing the behavior.
- Usually you should don't run tests yourself.

## Cross-Platform (KMP / Native) Notes

- `packages/shared-kmp` is the single source of truth for business logic shared between iOS and Android. If behavior genuinely belongs on both platforms, add it there instead of duplicating in each native app.
- API contracts are owned by the backend repo and mirrored here in `shared-kmp` for native consumers. Keep these in sync when touching wire-facing shapes; coordinate with the backend repo when contracts change.

## Android (Kotlin)

- **Visibility**: declare classes, functions, and top-level properties as `internal` by default. Only widen to `public` when the symbol is genuinely consumed from another Gradle module (e.g. `:app` consuming something from `:design-system`, or KMP `commonMain` API surface). Use `private` when the symbol stays inside a single file. This keeps module API surfaces small and refactor-friendly.

## Android Design System (`apps/android/design-system`)

**Hardcoded colors are not allowed in Android code.** Every color used in feature code, components, or screens must come from the theme — `MaterialTheme.colorScheme.*`, `MaterialTheme.melon.brand.*`, or `MaterialTheme.melon.surface.*`. Raw `Color(0x…)` literals, `Color.Red`/`Color.White`/etc., and direct references to backing constants (e.g. `InkLight`, `SurfaceDark`) are forbidden in feature code. If a color you need does not exist in the theme, add it to the design system first (extend `MelonColors`, `colorScheme`, or the appropriate token group), then consume it via the theme. The same rule applies to typography (`MaterialTheme.typography.*`, never `FontFamily.Default`) and motion (`MelonMotion.*`, never magic spring values).

When porting an iOS flow to Android, every color, typography role, and (eventually) animation must come through the design system. If iOS uses a token for it, Android has the equivalent token; if it doesn't exist yet, add it to the design system first.

The iOS source of truth is `apps/ios/UNES/DesignSystem/DesignTokens.swift` (`UNESColor`, `UNESFont`, `UNESMotion`). Read it before porting a screen so you know which tokens the iOS flow uses.

### Color access — three tiers, all routed through the theme

1. **`MaterialTheme.colorScheme.<slot>`** — iOS adaptive _neutrals_ + the adaptive accent. Use this for any standard Compose component (Button, Card, Surface, Text, etc.) — they read `colorScheme` automatically, so iOS parity comes for free. Do **not** read raw neutrals (`InkLight`, `SurfaceDark`, …) directly; they're `internal` backing constants for the scheme.

2. **`MaterialTheme.melon.brand.<color>`** — fixed brand identity (`plum`, `magenta`, `coral`, `amber`, `peach`, `alwaysDarkBg`). Same value in light and dark, but routed through the theme's `CompositionLocal` for one consistent entry point. Use when iOS reads `UNESColor.plum`/`coral`/etc. directly.

3. **`MaterialTheme.melon.surface.<token>`** — adaptive iOS tokens that don't have a Material 3 ColorScheme slot: `card`, `cardLine`, `line`, `pressedAccent`. Use when a card needs to match iOS exactly (the Material `surfaceContainer*` slots are close but not identical to iOS `card`).

### iOS → Android mapping

| iOS (`UNESColor`)                            | Android                                                                                                           |
| -------------------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| `ink`, `ink2`, `ink3`, `ink4`                | `colorScheme.onBackground` / `onSurface` / `onSurfaceVariant` / `outline` / `outlineVariant` (in that order)      |
| `surface`, `surface2`, `surface3`            | `colorScheme.surface`, `surfaceVariant`, `surfaceContainerHigh`                                                   |
| `pageBg`                                     | `colorScheme.background`                                                                                          |
| `card`                                       | `melon.surface.card`                                                                                              |
| `cardLine`                                   | `melon.surface.cardLine`                                                                                          |
| `line`                                       | `melon.surface.line`                                                                                              |
| `accent` (adaptive coral↔amber)              | `colorScheme.primary`                                                                                             |
| `accentPress`                                | `melon.surface.pressedAccent`                                                                                     |
| `plum`, `magenta`, `coral`, `amber`, `peach` | `melon.brand.plum` / `.magenta` / `.coral` / `.amber` / `.peach`                                                  |
| `darkBg` (always-dark splash/welcome)        | `melon.brand.alwaysDarkBg`                                                                                        |
| `surfaceLight`, `inkFixed`                   | use `melon.brand.alwaysDarkBg` context + `Color.White`/`InkLight` directly only if needed for always-dark screens |

If the M3 mapping in `Theme.kt` doesn't give you the exact iOS look on a specific surface (e.g. iOS uses `card` and Material's `surfaceContainerLowest` doesn't match), prefer `melon.surface.card` over a custom `Color(0x…)`.

### Typography

`MaterialTheme.typography` is wired to mirror `UNESFont`: display + headline roles use **Fraunces** (iOS "serif moments"), title/body/label roles use **Inter** (iOS sans). Both are downloaded via Google Fonts on first request. If iOS uses `UNESFont.serif(28)` reach for `MaterialTheme.typography.headlineSmall` (or the closest size); if iOS uses `UNESFont.sans(16, weight: .medium)` reach for `titleMedium`. Do not declare ad-hoc `TextStyle(fontFamily = FontFamily.Default, …)` in feature code.

### Strings

Always prefer string resources over hardcoded literals in Android code. Any user-facing text (labels, buttons, titles, error messages, content descriptions, etc.) must be declared in `strings.xml` and consumed via `stringResource(R.string.…)` (Compose) or `getString(R.string.…)` (Android framework). Do not inline literal strings in `Text("…")`, `contentDescription = "…"`, or similar call sites. This keeps the app translatable and consistent across screens. The only acceptable exceptions are non-user-facing strings (log tags, analytics keys, route names, test fixtures).

## Source Control Guidelines

- Do not add any co-author by claude or any other co-author note to the commits
