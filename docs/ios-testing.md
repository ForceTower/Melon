# Native iOS verification

Run from the repository root with Xcode 27.1 and an iOS 27 simulator. The native
app and UNESKit remain independent of KMP. Install Bun through `mise install bun`
for simulator selection and the hermetic HTTP server.

```sh
bash apps/ios/verify.sh app
bash apps/ios/verify.sh kit
bash apps/ios/verify.sh ui
```

The script selects the first available iOS 27 iPhone simulator. `IOS_DESTINATION`
overrides that selection.
Use `xcrun simctl list devices available` to find a local device and select it with
`IOS_DESTINATION='platform=iOS Simulator,id=YOUR-SIMULATOR-UUID'`. `IOS_ARTIFACTS`
overrides `artifacts/ios`. Commands return the xcodebuild exit status, preserve
logs, and create uniquely named `.xcresult` bundles. Open a result in Xcode to
inspect assertions, screenshots, accessibility results, and view hierarchies.
Successful runs also export a JSON test summary; UI runs extract their screenshot
and hierarchy attachments into `artifacts/ios/ui-attachments` with a manifest.

The shared merge workflow calls the reusable iOS workflow. The `xcode-27` runner
label must be available in the GitHub repository; source configuration cannot
provision that runner or enforce branch protection.

## Scenarios and contracts

`PilotScenarioTests` reads the canonical `contracts/v1/pilot.json` directly from
the checkout using the compiled test source path. Run these tests on a simulator
on the same machine as the checkout, rather than moving a built test bundle to a
different machine or physical device. This avoids maintaining a divergent Swift
copy of the shared contract. Tests decode auth, profile, onboarding, semester,
and message DTOs; execute login through the live repository; and confirm that
the real GRDB mirror retains its last successful semester after a failed refresh.

The existing `UNESIntentTests` target is an XCUITest target. It now also contains
`ScenarioJourneyTests`, which enters the synthetic account through Login, waits
for initial sync, enters Home, and checks cached content after the backend becomes
unavailable. A second journey rejects bad credentials. Its scenario IDs match
the shared catalog: `auth.login`, `auth.invalid-credentials`, `home.populated`,
`messages.empty`, and `home.offline-with-cache`. Each journey fails on unmocked
HTTP requests; the offline check waits for a request to reach the unavailable
backend before asserting that cached content remains visible.

The UI command owns a hermetic mock server on `127.0.0.1:8788`, separate from
Android's default port 8787. It never enables the production proxy. The Debug
app launch environment `MELON_SCENARIO=auth.login` selects a fresh in-memory
session, defaults, and migrated GRDB database, fixes reducer time to
`2026-10-02T13:00:00Z`, and starts at Login. Auth, sync, profile, messages, and Home
use their real HTTP repositories. Push, analytics, remote logs, widget/watch
publication, legacy migration, and other external integrations use preview
dependencies. The app delegate does not initialize external SDKs in this mode.
Release builds contain no scenario selector.

Scenario launches pin the process timezone to `America/Bahia`.

For a manual inspection, start `MELON_MOCK_PORT=8788 bun scripts/mock-melon.ts`, install the Debug app,
then launch the simulator app with:

```sh
SIMCTL_CHILD_MELON_SCENARIO=auth.login SIMCTL_CHILD_TZ=America/Bahia \
  xcrun simctl launch booted dev.forcetower.unes.ios \
  -AppleLanguages '(pt-BR)' -AppleLocale pt_BR
xcrun simctl io booted screenshot artifacts/ios/manual.png
xcrun simctl spawn booted log show --last 5m --predicate 'process == "UNES"'
```

Use `scenario` / `synthetic-only` to sign in. This environment hook controls
reducer time; SwiftUI timeline animations still use the system display clock.

## Current coverage boundaries

Enrollment Review checks its injected clock when it opens and again on each
submit attempt. A known `endDate` blocks submission only after the cutoff;
the exact cutoff is allowed, and an `OPEN` portal remains authoritative when its
published `startDate` is still ahead. Reopening a submitted proposal does not
bypass its deadline. The existing blocker dock explains an expired deadline;
the flow does not add a continuous timer or submit real enrollment data in tests.

`EnrollmentDeadlineTests` protects expired open/reopened proposals and a cutoff
crossed while Review stays open, plus the two allowed boundary cases. The red
run reproduced repository submit calls after expiration; after the fix, all six
enrollment suites pass, including existing conflict, queue, workload, draft,
whole-proposal submission, retry, and wire-mapping tests. Run the package suite
with `bash apps/ios/verify.sh kit`, or, from `apps/ios/UNESKit`, filter `xcodebuild test -scheme UNESKit`
with `-only-testing:UNESKitTests/EnrollmentDeadlineTests` and an installed
simulator destination for the focused regression. Native enrollment still needs
an installed-app journey and decoding coverage for the shared enrollment JSON;
these reducer/mapping tests use synthetic in-process fixtures.

The [screen inventory](scenario-coverage.md) maps remaining native and Android
states to their source models and distinguishes executable pilot cases from
planned fixtures, states that do not apply, and states the UI does not model.

The login screen has an automated element-detection and trait
[accessibility audit](https://developer.apple.com/documentation/xcuiautomation/xcuiapplication/performaccessibilityaudit%28for%3A_%3A%29).
Contrast, hit regions, Dynamic Type, and VoiceOver review remain separate work.
Screenshots are retained evidence for review, not an approved pixel-difference
baseline. Wider screen states, device/theme/text-size matrices, fully pinned
SwiftUI timeline clocks, and a gallery/difference renderer remain open.

The intent target runs explicitly, including declaration and schedule-response
checks. Its existing data-dependent entity/Spotlight tests skip without a
persisted signed-in mirror; the UI harness intentionally uses memory storage and
does not claim to verify persistent Spotlight indexing. Do not use a real student
account to make those tests pass in CI. Use a dedicated simulator: those system
intent queries read persisted widget/Spotlight state outside the scoped UI store.

Xcode 27.1 with the installed iOS 27.0 simulator rejects AppIntentsTesting runtime
operations with `AppIntentsServicesSecurityErrorDomain` code `803` ("Unable to run
internal tests on a Customer build"). Those three runtime checks explicitly skip
only for that exact platform error; other errors fail normally. Intent/entity
declaration tests and all UI journeys still run. Resolve this Apple runtime
limitation before claiming end-to-end Siri/Spotlight execution coverage.
