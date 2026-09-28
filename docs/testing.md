# Verification commands and merge evidence

Run commands from the repository root. Install the versions in `.mise.toml`
(`mise install`, then `mise exec -- <command>`) and run
`bun install --frozen-lockfile` for TypeScript checks. Android also needs its SDK,
accepted SDK licenses, and JDK 21. Native iOS needs the Xcode version and simulator
configured in `.github/workflows/ios.yml`.

Native app, package and UI commands are documented in [iOS testing](ios-testing.md).
Dedicated-device measurement and profile generation are in
[Android performance](android-performance.md).

| Command | Evidence |
| --- | --- |
| `bun run check` | Oxlint and Oxfmt checks for the TypeScript repository. |
| `bun run verify:repository` | Verification-tool tests, Android conventions, fixture/credential checks and hermetic mock-server tests. |
| `bun run android:check` | ktlint, release and benchmark test-APK assembly, Android debug unit tests, KMP JVM tests, Android lint. |
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

After the workflow is merged and has produced a run, the repository maintainer
must select **Required verification** as a required status check in the relevant
ruleset/branch protection. Adding workflow source does not activate that setting.
The existing iOS `xcode-27` runner label also needs an available runner; changing
these files cannot provision or verify one. No remote repository settings are
changed by this implementation.

Android CI uploads HTML unit-test and lint reports plus XML test results, even
after failure, with 14-day retention. The emulator lane uploads screenshots,
hierarchy/log evidence and instrumentation results. Native iOS uploads its test
results through its reusable workflow. Failed setup can leave no reports; an
artifact warning is not a successful test result.

The emulator lane runs synthetic Home startup, Home rendering/accessibility,
invalid credentials with retry, failed initial sync with retry, cached offline
Home, expired-session recovery UI and the empty inbox. It builds the scenario
app once and reuses that APK while clearing its isolated state between journeys.

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

This is a scoped architecture check, not compiler-aware Android lint. It handles
comments, string literals and declaration nesting but does not resolve aliases,
propagated string values, arbitrary wrappers or all Kotlin syntax. Framework
Android lint remains part of `android:check`. Human review still applies the full
rules in `CLAUDE.md`, including other modules and API visibility decisions.

`scripts/verification/android-conventions-baseline.json` records existing debt by
path, rule, offending source and count. New findings fail. Existing baseline
entries are not blanket exemptions for their files; changing a literal or adding
another instance also fails. Remove entries as debt is fixed. The initial
inventory includes legacy widget colors, transparency/unspecified sentinels,
monospace typography, literal labels and public app declarations. Those require
deliberate cleanup, not automatic rewrites that could alter UI or framework entry
points. `bun run scripts/check-android-conventions.ts --inventory` prints the
current inventory for review; do not regenerate the baseline to hide new debt.

## Synthetic fixtures and credential checks

`bun run check:fixtures` scans tracked and non-ignored source files for known
credential formats, and fixture/contract/mock files for non-reserved email
domains, CPF formatting and numeric student identifiers. Versioned contract JSON
must contain an `origin` explaining its synthetic provenance. Failures print only
the path and rule, never the matched credential.

The credential check covers private keys, GitHub/Slack tokens, AWS access key IDs,
Google service-account documents and JWTs. It intentionally permits public
analytics/client identifiers. It is a local pattern check, not comprehensive
secret discovery or Git-history scanning; configure the hosting provider's
secret scanning independently where available. Detection does not establish that
a person, message or record is synthetic.

When converting an incident into a fixture, start from the behavior and create
new people, IDs, dates, text and credentials. Use reserved `example.invalid`
addresses and plainly synthetic tokens. Record origin beside the fixture and
review it before committing. Never paste a production response into this public
repository and rely on a scanner to sanitize it. Keep raw evidence restricted.
