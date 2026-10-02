# Reproducible app scenarios

The executable pilot catalog is `scripts/scenarios.ts`. Its account and wire
responses live in `contracts/v1/pilot.json`, authored from synthetic data. Time is
fixed at **2026-10-02 10:00 America/Bahia**. Allocation weekdays use Sunday=0;
the pilot has a Friday class from 10:00 to 12:00.

## Android

Connect a device (or start an emulator), find its serial with `adb devices`, then:

```sh
bun run android:scenario home.populated --serial DEVICE_ID
```

This builds and installs `com.forcetower.uefs.scenario` and its instrumentation
APK, clears only that package, forwards port 8787, starts a private hermetic
server, and performs the real login and initial sync. The ordinary app and debug
installation have different IDs. Use `--no-build` only when the APKs already
match your source. Each invocation gets fresh server and app storage.

The runner selects pt-BR for this package. Its `AppClock` controls Home, schedule,
profile and ready projections. Analytics, crash reporting, remote logging,
remote configuration, push registration, reminders and store prompts are disabled
in the scenario build; normal release builds cannot select the scenario mode.
Scenario feature gates use their false defaults instead of debug's all-on policy.
The enrollment journeys explicitly enable only the enrollment gate through an
override guarded by the isolated build flag; ordinary builds cannot activate it.

| ID | Assertion / transition |
| --- | --- |
| `auth.login` | Welcome reaches the credential form. |
| `auth.invalid-credentials` | Rejected login shows its error; the same form can retry successfully. |
| `sync.unavailable` | A failed initial fetch offers retry; restoring the server completes sync. |
| `home.populated` | Real HTTP decoding and database writes reach Home with the synthetic name and class. |
| `home.offline-with-cache` | A forced refresh reaches the unavailable server; the saved class remains visible. |
| `auth.session-expired` | Rejected refresh shows the session banner while keeping cached content. |
| `messages.empty` | A successful empty inbox is shown without a network error. |
| `enrollment.schedule-conflict` | Conflicting saved selections disable submission and produce no submit request. |
| `enrollment.under-minimum` | Below-minimum workload disables submission and produces no submit request. |
| `enrollment.over-maximum` | Above-maximum workload disables submission and produces no submit request. |
| `enrollment.deadline-expired` | A stale OPEN window cannot permit submission after the deadline. |
| `enrollment.submit-retry` | Select a full waitlisted section with a prerequisite warning, remove the previous selection, and recover from failed submission without changing the complete desired set. |

Enrollment uses the additional shared provider example `contracts/v1/enrollment.json`.
The mock records attempted and accepted selections separately. Its named variants
change constraints, selected sections or prerequisite metadata, preserving the
same synthetic people. Prerequisites remain informational; the unmet prerequisite
variant deliberately exercises the supported client warning shape even though
the current provider marks returned prerequisites as met. Deadline variants keep
OPEN to reproduce a stale response. Exact deadline equality is accepted, and an
OPEN window is not rejected merely because its published start is in the future.

`artifacts/android/<id>/index.html` links the latest immutable run directory with screenshots, hierarchy, Compose semantics,
instrumentation assertions, available app logcat, mock request counts, device,
commit, dirty-worktree status, fixture version and reproduction command. Earlier
attempts remain available under `runs/`. A failed assertion or unexpected
HTTP route fails the run. Missing evidence is recorded in `result.json`.

`OverviewContentTest` separately renders Home's recovery banner with light/default
text and dark/large text. It checks banner precedence and callbacks, runs Android
accessibility checks, and captures the Compose root. This proves rendering;
`ScenarioJourneyTest` proves that the installed app can reach the state.

```sh
bun run android:scenario home.populated --render --serial DEVICE_ID
java scripts/CompareScreenshots.java approved.png actual.png artifacts/difference.png
```

The comparator emits changed-pixel JSON and a magenta difference image, exiting
nonzero for changed pixels or mismatched dimensions. An optional final ratio
sets an explicitly reviewed tolerance; default is exact. Compare only the same
device, OS, density, font scale, locale, theme and fixture version. Copy a reviewed
image to your baseline store explicitly; the runner never accepts baselines.
No universal pixel baseline is checked in for the physical-device pilot. CI
publishes evidence for review while the standard device configuration is chosen.

Build/run all instrumentation directly against an already running hermetic server:

```sh
bun run mock
# In another terminal, with ANDROID_SERIAL set to the intended device:
adb -s "$ANDROID_SERIAL" reverse tcp:8787 tcp:8787
./gradlew :apps:android:app:connectedScenarioAndroidTest -Pmelon.testBuildType=scenario
```

Direct instrumentation assumes a fresh scenario package for the login journey.
The command wrapper supplies that reset. Screenshots are review evidence; no
unreviewed visual baseline is silently accepted. Device timing, notification
permission prompts, remote images and the remaining feature catalog are not
covered by this pilot.

## Mock server controls

```sh
bun run mock
curl http://127.0.0.1:8787/debug/scenarios
curl http://127.0.0.1:8787/debug/state
curl -X POST http://127.0.0.1:8787/debug/reset
curl -X POST http://127.0.0.1:8787/debug/scenario/home.offline-with-cache
```

The default server never proxies. Unknown method/path pairs return 501, including
during simulated outages, and remain recorded across scenario switches. Full
reset restores enrollment submission, event phase/revision, credentials,
reauthentication, session expiry, token counters, scenario, and diagnostic counts.
Independent handler instances have independent state. Dates do not use the host
timezone. `MELON_MOCK_NOW` overrides server time; use the canonical clock when
running the fixed-clock Android build. `MELON_MOCK_PORT` changes the listening port.

Manual controls also accept POST at `/debug/session/expire|restore`,
`/debug/refresh-mode/rotate|reject|unavailable`, `/debug/credentials/ok|invalid|none`,
`/debug/reauth-mode/accept|reject|unavailable` and
`/debug/campus-phase/upcoming|live|ended`.

`bun run mock --proxy` explicitly enables the previous production passthrough
mode for manual use. It is excluded from tests and CI. The older campus event
rehearsal fixture is available only in proxy mode; the hermetic pilot has no
featured event. Use only synthetic credentials with the hermetic server.

## Adding coverage

Add a named catalog entry, extend the synthetic wire fixture only as necessary,
and implement the UI assertion and transition in `ScenarioJourneyTest`. Add mock
handler tests for any new response mode. Document data origin, reset behavior,
clock, relevant flags and expected visible outcome. Run the one-scenario command
and inspect its screenshot as well as its assertions. Any new endpoint must have
a deliberate local response; do not add a catch-all success response.

For iOS, see [the native guide](ios-testing.md). For device performance and release
profiles, see [the testing guide](testing.md).
