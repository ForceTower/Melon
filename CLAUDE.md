# Melon Client Agent Instructions

## Overall

- This repository holds the **client applications** (Android, iOS, and the landing site).
  The backend they talk to is a separate, closed-source service; API contracts are
  consumed over HTTP and mirrored in `packages/shared-kmp` for native consumers.
- Code readability matters most.
- ALWAYS read and understand relevant files before proposing edits. Do not speculate
  about code you have not inspected.

## Tooling

- `mise` is used for tool version management (see `.mise.toml`).
- `bun` is used for the TypeScript landing site (Node package management + script runner).
  We do NOT use `npm`, `yarn`, or `pnpm`. `bunx` must be used in place of `npx`.
- `oxlint` is used for linting, `oxfmt` for formatting (we do NOT use `prettier`,
  `eslint`, or anything else). Run `bun run fix` to format + lint with fixes.
- `gradle` is used for the JVM side (Android app + Kotlin Multiplatform shared package).
- Native iOS is a standard Xcode project (`apps/ios`).

## Monorepo Structure

- `apps/android` — Native Android app (Kotlin + Jetpack Compose), Gradle.
- `apps/ios` — Native iOS app (Swift + SwiftUI + The Composable Architecture); local
  SPM package `UNESKit`, no KMP.
- `apps/landing` — Marketing site (Astro), deployed to Cloudflare.
- `packages/shared-kmp` — Shared business logic via Kotlin Multiplatform, packaged as an
  XCFramework for iOS and a library for Android. Split into `core/` (database, network),
  `features/` (auth, dashboard, …), and `umbrella/` (combined build target).
- `build-logic/` — Gradle convention plugins shared across the JVM projects.

## File Organization & Naming

- **TypeScript file naming**: `kebab-case.ts`; use named exports over default exports.
- **Swift files**: standard Swift conventions (`PascalCase.swift` for types).
- **Kotlin files**: `PascalCase.kt` matching the primary type, `camelCase.kt` for
  top-level declarations.

## TypeScript & Type Safety (landing)

- Avoid `any` and type casting. Prefer type guards over casts.
- Avoid explicit types when TypeScript can infer them.

## Development Workflow for iOS

- The project builds with **Swift 6** and `SWIFT_STRICT_CONCURRENCY = complete`. Code that
  compiles can still crash at runtime when actor-isolation preconditions are violated —
  write isolation-safe code, don't just satisfy the compiler.
- If you make big changes to iOS and are on macOS, try to compile the app.
- English identifiers/comments always; pt-BR only for user-facing copy (folder is
  `Features/Enrollment`, not `Matricula`).
- Native back chevrons only; multi-step flows value-route on the host `NavigationStack`
  (no `fullScreenCover`/nested stacks). Every screen gets a bare `#Preview`.

## Cross-Platform (KMP / Native) Notes

- `packages/shared-kmp` holds the shared business logic and is consumed by the Android
  app. The iOS app (`apps/ios`) is fully native and does not consume KMP; the iOS
  XCFramework target is kept building regardless.
- KMP changes only need the Android (Gradle) build to verify — no iOS compile required.
  Keep both KMP targets (Android library + iOS XCFramework) in the build config.

## Android (Kotlin)

- **Visibility**: declare classes, functions, and top-level properties as `internal` by
  default. Widen to `public` only when the symbol is genuinely consumed from another
  Gradle module. Use `private` when the symbol stays inside a single file.

## Android Design System (`apps/android/design-system`)

**Hardcoded colors are not allowed in Android code.** Every color used in feature code,
components, or screens must come from the theme — `MaterialTheme.colorScheme.*`,
`MaterialTheme.melon.brand.*`, or `MaterialTheme.melon.surface.*`. Raw `Color(0x…)`
literals, `Color.Red`/`Color.White`/etc., and direct references to backing constants are
forbidden in feature code. If a color you need does not exist in the theme, add it to the
design system first, then consume it via the theme. The same rule applies to typography
(`MaterialTheme.typography.*`, never `FontFamily.Default`) and motion (`MelonMotion.*`,
never magic spring values).

The token source of truth is `apps/android/design-system/DesignTokens.reference.swift`
(`UNESColor`, `UNESFont`, `UNESMotion`) — a reference-only Swift file preserved from the
original iOS app, which this design system mirrors. Read it before porting a screen so
you know which tokens the original flow used. If it defines a token, Android has the
equivalent token; if it doesn't exist yet, add it to the design system first.

### Color access — three tiers, all routed through the theme

1. **`MaterialTheme.colorScheme.<slot>`** — iOS adaptive _neutrals_ + adaptive accent.
   Use for any standard Compose component (Button, Card, Surface, Text, etc.).
2. **`MaterialTheme.melon.brand.<color>`** — fixed brand identity (`plum`, `magenta`,
   `coral`, `amber`, `peach`, `alwaysDarkBg`). Same value in light and dark.
3. **`MaterialTheme.melon.surface.<token>`** — adaptive iOS tokens without a Material 3
   ColorScheme slot: `card`, `cardLine`, `line`, `pressedAccent`.

### Typography

`MaterialTheme.typography` mirrors `UNESFont`: display + headline roles use **Fraunces**
(iOS "serif moments"), title/body/label roles use **Inter** (iOS sans). Do not declare
ad-hoc `TextStyle(fontFamily = FontFamily.Default, …)` in feature code.

### Strings

Always prefer string resources over hardcoded literals. Any user-facing text must be
declared in `strings.xml` and consumed via `stringResource(R.string.…)` (Compose) or
`getString(R.string.…)`. Do not inline literal strings in `Text("…")`,
`contentDescription = "…"`, or similar call sites. The only acceptable exceptions are
non-user-facing strings (log tags, analytics keys, route names, test fixtures).

## Source Control

- Do not add any co-author trailer or "Generated with …" note to commits.
