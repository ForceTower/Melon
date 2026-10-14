# Verification commands and merge evidence

Run commands from the repository root. Install the versions in `.mise.toml`
(`mise install`, then `mise exec -- <command>`) and run
`bun install --frozen-lockfile` for TypeScript checks. Android also needs its SDK,
accepted SDK licenses, and JDK 21. Native iOS needs the Xcode build pinned in
`apps/ios/select-xcode.sh` and an iOS 27 iPhone simulator.

Native app, package and UI commands are documented in [iOS testing](ios-testing.md).
Dedicated-device measurement and profile generation are in
[Android performance](android-performance.md).

If an older system Node or Bun shadows the pinned runtime inside scripts, use
`mise exec -- bun --bun run check` (and the same prefix for other Bun scripts).
This keeps nested commands on Bun's runtime instead of the system executable.

| Command | Evidence |
| --- | --- |
| `bun run check` | Oxlint and Oxfmt checks for the TypeScript repository. |
| `bun run verify:repository` | Verification-tool tests, Android conventions, fixture/credential checks and hermetic mock-server tests. |
| `bun run android:check` | ktlint, release and benchmark test-APK assembly, Android debug unit tests, KMP JVM tests, custom lint tests and Android lint. |
| `bun run android:scenario home.populated --serial emulator-5554` | Installed scenario build, synthetic-account journey and captured device evidence. |
| `bun run landing:check` | Astro type checking and production build. |

`android:check` sets `melon.verification=true` to disable Crashlytics artifact
uploads during validation. The normal release packaging configuration is still
built; this command does not publish a release or diagnostic mappings.

For a focused regression, pass Gradle's test filter to the relevant task, for
example:

```sh
./gradlew :apps:android:app:testDebugUnitTest --tests '*DeepLinkParserTest'
./gradlew :packages:shared-kmp:features:auth:jvmTest
```

Use `bun run fix` for TypeScript formatting and lint fixes, and
`./gradlew ktlintFormat` for Kotlin. `ktlintCheck` covers every `*.kt`/`*.kts` file
in the repository (including `build-logic`) against the rules in `.editorconfig`,
which Android Studio also reads, and writes a checkstyle report to
`build/reports/ktlint/ktlint.xml`. Do not use root Gradle `build`
or `check` in the Linux lane: those aggregate tasks can select Apple targets.
Both Android and iOS KMP targets remain configured; native iOS itself does not
consume KMP.

Every behavior change should name the behavior at risk and add the smallest
useful test that protects it. A bug fix should include a regression that fails
for the reported reason before the fix and passes afterward. Record the
reproduction command and both outcomes in the PR. When that is impractical,
record the specific limitation, verification performed and remaining gap.
Documentation and mechanical changes do not require artificial test quotas.

## CI and repository setup

`Verify` runs for every PR and push to `main`, with no workflow-level path filter.
Repository checks always run. `scripts/ci-affected.ts` selects Android, native iOS
and landing lanes from the changed paths; deleted/renamed paths are included.
Shared fixtures, scripts, root configuration and workflow changes select all
platforms. KMP and Gradle changes select Android without requiring macOS.
Documentation-only changes skip platform builds. Manual dispatch selects all.

The final **Required verification** job runs even when a dependency fails or is
skipped. It rejects failures, cancellations, missing results and skipped lanes
that were selected. Its selection and gate logic have tests, including negative
cases. The job summary records selected platforms and final lane results.

On pushes to `main`, a passing **Required verification** hands the selected iOS and
Android lanes to the `Distribute` workflow, which uploads them to TestFlight and the
Play internal track; see [internal distribution](distribution.md). A newer push
cancels superseded PR runs but queues behind a running `main` run, so uploads are
never interrupted.

On October 2, 2026, `main` protection was configured to require **Required
verification** from the GitHub Actions app (ID `15368`). Strict checking requires
the branch to be current with `main`. Existing signature, linear-history,
force-push and deletion settings were preserved. Administrators can bypass the
protection and push to `main` directly; those pushes still run **Verify**, and
`Distribute` only uploads once **Required verification** passes.
This follows successful remote runs of every selected lane, including native
iOS on the existing `xcode-27` runner. The setting is repository configuration,
not something a workflow file activates by itself.

Inspect the active requirement with:

```sh
gh api repos/ForceTower/Melon/branches/main/protection/required_status_checks
gh api repos/ForceTower/Melon/branches/main/protection/enforce_admins
```

Android CI uploads HTML unit-test and lint reports, ktlint checkstyle, and XML test results, even
after failure, with 14-day retention. The emulator lane uploads screenshots,
hierarchy/log evidence and instrumentation results. Native iOS uploads its test
results through its reusable workflow. Failed setup can leave no reports; an
artifact warning is not a successful test result.

`android:check` also writes Gradle's HTML timing report under `build/reports/profile/`,
uploaded with the Android reports. It records configuration, dependency resolution
and task durations. Compare the same toolchain and task selection with similar cache
state; a fresh checkout, cached build and no-op build measure different costs.
The GitHub job/step timestamps provide total runner duration. Keep the original
failed run when investigating a suspected flake; these reports do not classify
flakes or establish a performance threshold automatically.

The emulator lane runs synthetic Home startup, Home rendering/accessibility,
invalid credentials with retry, failed initial sync with retry, cached offline
Home, expired-session recovery UI, the empty inbox and message-push taps that
outrun the mirror or point at a missing message. Enrollment journeys cover
conflicts, minimum/maximum hours, expired deadlines, and a failed submission
followed by a successful full-proposal replacement with waitlist/prerequisite
warning behavior. It builds the scenario app and instrumentation APK before
booting the emulator, using a single-use Gradle daemon so compilation does not
compete with the emulator for resources. Every journey uses `--no-build` and
clears its isolated state. System error dialogs remain visible: a blocked or
unresponsive app must still fail the journey.

Repository tooling/mock tests also emit JUnit XML at
`artifacts/repository-tests.xml`, uploaded by the always-running repository lane.

Record first-attempt failures before a diagnostic rerun. A quarantine must have
an issue, owner, expiry date and explicit coverage gap; use the quarantine issue
form. The workflows do not automatically retry failures or accept screenshot
baselines. The [scenario guide](scenarios.md) documents the local image comparator;
its self-test runs in the Android check command. CI publishes screenshots for
review while a stable device configuration and approved visual baselines are
established, so capture alone is not a pixel-difference gate.

## Android source conventions

`bun run check:conventions` lexes Kotlin in the Android app's main, debug and
scenario source sets. It checks direct raw/preset Compose colors, direct backing
theme tokens, `FontFamily` overrides, inline `Text`/`contentDescription` literals,
unqualified springs and top-level declarations without `internal` or `private`.
Backing names come from the existing theme color/font definitions. The design
system may define tokens and expose cross-module APIs, so this first checker is
scoped to the app. Unit/instrumentation test fixtures are excluded.

This fast lexer handles comments, string literals and declaration nesting. The
`apps/android/lint-checks` JVM module adds symbol-resolving checks to the app's
existing `lintDebug` pass through `lintChecks`, without becoming an app dependency:

| Issue | Additional enforcement |
| --- | --- |
| `MelonThemeColor` | Constant Compose colors, preset colors and direct backing tokens, including aliases. Transparent, Unspecified and runtime widget color restoration are allowed. |
| `MelonThemeTypography` | Raw font families, default/preset families and backing font tokens, including aliases. |
| `MelonThemeMotion` | Qualified and aliased Compose `spring` calls. |
| `MelonStringResource` | Constant text and descriptions passed to Compose/Glance Text, BasicText, Icon and Image, including aliases and reordered arguments. |
| `MelonInternalVisibility` | Kotlin top-level app declarations parsed through PSI, including annotations, generics and type aliases. |

The checks report errors and exclude test/generated sources. They are scoped to
the app: design-system token definitions and public library APIs are intentional.
The debug lint pass covers main/debug; the fast checker also covers scenario
sources without adding another Android variant analysis. Arbitrary wrappers,
dynamic string composition and cross-function data provenance still need review.
These checks do not attempt a whole-program call graph.

Run `./gradlew :apps:android:lint-checks:test` for focused detector tests, or
`./gradlew :apps:android:app:lintDebug` for real app analysis. The detector tests
include invalid examples, valid theme/resource usage, unrelated symbols with
the same names, and compiled Kotlin fixtures for library property resolution.
Keep the `android-lint` catalog version aligned with AGP when upgrading and rerun
these tests. They run in `android:check`; their JUnit/HTML results use the same
CI artifact paths as the other JVM tests. A normal `assembleDebug` does not run
these tests or the custom lint analysis. A first lint run compiles the small JVM
module; later runs reuse Gradle's task outputs/cache when inputs are unchanged.

`scripts/verification/android-conventions-baseline.json` records accepted debt by
path, rule, offending source and count. It is empty: the original 113 findings were
fixed, so any finding fails. Fix new findings through the design system, string
resources or visibility modifiers instead of adding baseline entries; an entry is
not a blanket exemption for its file, and changing its literal or adding another
instance also fails. `bun run scripts/check-android-conventions.ts --inventory`
prints the current findings for review; do not regenerate the baseline to hide debt.

## Synthetic fixtures and credential checks

`bun run check:fixtures` scans tracked and non-ignored source files for known
credential formats, and fixture/contract/mock files for non-reserved email
domains, CPF formatting and numeric student identifiers. Versioned contract JSON
must contain an `origin` explaining its synthetic provenance. Failures print only
the path and rule, never the matched credential.

The credential check covers private keys, GitHub/Slack tokens, AWS access key IDs,
Google service-account documents and JWTs. It intentionally permits public
analytics/client identifiers. It is a local pattern check, not comprehensive
secret discovery or Git-history scanning. GitHub secret scanning and push
protection were read back as enabled on October 14, 2026; non-provider patterns
and validity checks were disabled. These hosting settings are independent of the
workflow and must be checked separately on forks. Detection does not establish
that a person, message or record is synthetic.

When converting an incident into a fixture, start from the behavior and create
new people, IDs, dates, text and credentials. Use reserved `example.invalid`
addresses and plainly synthetic tokens. Record origin beside the fixture and
review it before committing. Never paste a production response into this public
repository and rely on a scanner to sanitize it. Keep raw evidence restricted.
