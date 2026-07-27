# In-app updates (Android) — implementation spec

**Status: implemented.** Play In-App Updates for `apps/android`
only — iOS updates ship through the App Store and need nothing here. The goal
is to shorten the tail of stale installs: most updates download silently in the
background (flexible flow), and only a release explicitly marked as critical
interrupts the user (immediate flow). Everything must degrade to a no-op on
devices without Play (sideloads, emulators, debug builds).

## Dependency

| Piece | Change |
| --- | --- |
| `gradle/libs.versions.toml` | `play-app-update = "2.1.0"` under `[versions]`; `play-app-update-ktx = { module = "com.google.android.play:app-update-ktx", version.ref = "play-app-update" }` under `[libraries]` |
| `apps/android/app/build.gradle.kts` | `implementation(libs.play.app.update.ktx)` |

2.1.0 (May 2023) is the latest partitioned release and the first with the
Activity Result–based `startUpdateFlowForResult` overload — required for apps
targeting Android 14+, which we do (`targetSdk = 37`). Verify latest at
implementation time; anything ≥ 2.1.0 works with this spec.

The `-ktx` artifact is kept **because we use its suspend API**
(`requestAppUpdateInfo()`), not just out of habit — with plain callbacks it
would buy nothing. The closest catalog precedent
(`play-services-mlkit-document-scanner`) inlines its version; the `[versions]`
ref here is a deliberate deviation so the pin sits visibly alongside the other
Play/Firebase versions.

Licensee gate: the `app-update` POM declares its license by URL only
("Play Core Software Development Kit Terms of Service"), so the `licensee {}`
block in `apps/android/app/build.gradle.kts` needs
`allowUrl("https://developer.android.com/guide/playcore/license")` next to the
existing Play services / ML Kit entries, or the release build fails.

## API surface

Use the modern flow only — the convention plugin
(`build-logic/convention/src/main/kotlin/melon.android-application.gradle.kts`)
sets `allWarningsAsErrors = true`, so the deprecated
`startUpdateFlowForResult(activity, requestCode)` overloads would fail the
build anyway.

- Info fetch: `AppUpdateManager.requestAppUpdateInfo()` — the ktx suspend
  wrapper — called from suspend functions on the coordinator, launched in
  `MainActivity.lifecycleScope`. Tying the coroutine to the activity's
  lifecycle means a destroyed activity cancels the check before it can touch a
  dead launcher; no defensive catch needed.
- Launcher: `registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult())`
  registered as a `MainActivity` field (must exist before `STARTED`; precedent
  at `MaterialsUploadSheet.kt`). There is no dedicated `AppUpdateResultContract`
  in the library; `StartIntentSenderForResult` is the documented contract.
- Start: `appUpdateManager.startUpdateFlowForResult(appUpdateInfo, launcher, AppUpdateOptions.newBuilder(type).build())`.
  Returns `Boolean` — `false` means the flow could not start; log and stop.
- Result codes in the launcher callback: `RESULT_OK`,
  `RESULT_CANCELED` (user declined), and
  `com.google.android.play.core.install.model.ActivityResult.RESULT_IN_APP_UPDATE_FAILED`.
  The callback delegates to `appUpdater.onUpdateFlowResult(result.resultCode)`
  (declines feed the suppression window below; everything else is logged and
  ignored).

Logging lane: **Timber**, matching the platform-glue code in
`dev.forcetower.unes.firebase.*` (the Kermit `Logger` lane is for
KMP-facing ViewModels like `ConnectedViewModel`). `Timber.DebugTree` is only
planted in debug (`MelonApp.kt`), which is exactly where the feature gets
exercised by hand; release stays silent by design — these are expected states,
not errors, so no Crashlytics non-fatals.

## Update strategy

Decision runs once per cold start, driven by `updateAvailability()`,
`updatePriority()` (0–5, set per release **via the Play Developer API only** —
releases cut from the Console UI default to 0, so the priority rows below are
dead weight unless our release process sets it; the staleness rows are the
everyday workhorse) and `clientVersionStalenessDays()` (null until Play has
observed the update for that device):

| Condition (first match wins; requires `UPDATE_AVAILABLE`) | Flow |
| --- | --- |
| `updatePriority >= 4` and `isUpdateTypeAllowed(IMMEDIATE)` | IMMEDIATE |
| `stalenessDays >= 30` and `isUpdateTypeAllowed(IMMEDIATE)` | IMMEDIATE |
| `updatePriority >= 2` and `isUpdateTypeAllowed(FLEXIBLE)` and not suppressed | FLEXIBLE |
| `stalenessDays >= 7` and `isUpdateTypeAllowed(FLEXIBLE)` and not suppressed | FLEXIBLE |
| otherwise | do nothing |

Rationale: priority 4–5 is reserved for "broken API contract / security"
releases we explicitly mark; 30 days of ignoring updates also earns the
fullscreen flow. Routine releases stay invisible for a week so users aren't
nudged on every minor bump, then download silently. The `versionCode` scheme in
`apps/android/app/build.gradle.kts` (`2130000 + gitVersionCode`) needs no
change — priority and staleness both come from Play, not from the binary.

**Decline suppression**: without it, a daily user who declines a flexible
prompt gets re-asked every cold start until the 30-day IMMEDIATE escalation.
On `RESULT_CANCELED`, persist the decline day in a Preferences DataStore
(file-local `preferencesDataStore` following the `ThemePreferenceStore`
pattern, `apps/android/app/src/main/kotlin/dev/forcetower/unes/theme/ThemePreferenceStore.kt`)
and suppress the FLEXIBLE rows for **7 days**. The IMMEDIATE rows ignore
suppression — a critical release must not be muted by an earlier routine
decline.

The table is implemented as a pure function so it gets a plain-JUnit table
test, same pattern as `parseDeepLink` in
`apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/connected/DeepLinks.kt`:

```kotlin
internal fun chooseUpdateType(
    availability: Int,      // AppUpdateInfo.updateAvailability()
    priority: Int,
    stalenessDays: Int?,
    immediateAllowed: Boolean,
    flexibleAllowed: Boolean,
    flexibleSuppressed: Boolean,
): Int? // AppUpdateType.IMMEDIATE / AppUpdateType.FLEXIBLE / null
```

Anything other than `UpdateAvailability.UPDATE_AVAILABLE` returns null — the
function can express "no update available" on its own rather than leaning on
`isUpdateTypeAllowed` happening to be false in that state.

## Where the check lives

New `internal class InAppUpdater` in
`apps/android/app/src/main/kotlin/dev/forcetower/unes/update/InAppUpdater.kt`,
a Hilt `@Singleton` following the `DeepLinkHandler` precedent: the singleton is
activity-agnostic, `MainActivity` feeds it what only an activity has.

- Constructor-injects `AppUpdateManager` (provided by a new
  `apps/android/app/src/main/kotlin/dev/forcetower/unes/di/PlayModule.kt`:
  `@Provides @Singleton` from `AppUpdateManagerFactory.create(@ApplicationContext …)`)
  and `@ApplicationContext Context` for the suppression DataStore.
  Injecting the manager rather than building it inline keeps the class
  substitutable in tests.
- Exposes `val updateDownloaded: StateFlow<Boolean>` — flips true when a
  flexible download finishes — plus `fun completeUpdate()` (delegates to
  `appUpdateManager.completeUpdate()`), `fun dismissUpdateBanner()` (resets
  the flag for this session), and `fun onUpdateFlowResult(resultCode: Int)`
  (records declines, logs the rest).
- `suspend fun checkOnLaunch(launcher: ActivityResultLauncher<IntentSenderRequest>)`
  runs this exact sequence:
  1. If `checked` is already true, return; set `checked = true`. The flag
     lives on the singleton, so it means "once per process" — config changes
     and activity recreation don't re-prompt, and process-death restore
     (non-null `savedInstanceState`) still gets its check. A config change
     racing the in-flight check can cost that process its check; accepted,
     next launch retries.
  2. `requestAppUpdateInfo()` — any exception (sideload, emulator, `.debug`
     app id → `ERROR_APP_NOT_OWNED`) is caught, logged, done.
  3. If `installStatus() == InstallStatus.DOWNLOADED` (a flexible download
     finished while the process was dead), set `updateDownloaded = true` and
     **return** — never re-run the consent sheet for bits already on disk.
  4. `chooseUpdateType(…)`; null → return.
  5. IMMEDIATE → set `startedImmediate = true`; FLEXIBLE → register the
     `InstallStateUpdatedListener` first. Then `startUpdateFlowForResult(…)`;
     a `false` return is logged and dropped.
- `suspend fun resumeIfStalled(launcher: …)` — the IMMEDIATE re-entry (below).
  **No-ops unless `startedImmediate` is true**: Play reports
  `DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS` for a background flexible download
  too, and resuming that as IMMEDIATE would hijack a silent download into a
  fullscreen blocker.
- Never stores the launcher in a field — the singleton outlives the activity,
  so the launcher is a parameter; the lifecycle-scoped coroutines (above)
  guarantee it is never touched after the activity is gone.

`MainActivity` (`apps/android/app/src/main/kotlin/dev/forcetower/unes/MainActivity.kt`)
changes:

- `@Inject internal lateinit var appUpdater: InAppUpdater` (alongside
  `themePreferences` / `deepLinks`).
- Field: `private val updateFlowLauncher = registerForActivityResult(StartIntentSenderForResult()) { result -> appUpdater.onUpdateFlowResult(result.resultCode) }`.
- `onCreate`: `lifecycleScope.launch { appUpdater.checkOnLaunch(updateFlowLauncher) }` —
  unconditionally; dedupe is the singleton's `checked` flag, not a
  `savedInstanceState` gate (which would skip the process-death restore path
  the DOWNLOADED recovery exists for; the deeplink gate guards re-navigation,
  a hazard this check doesn't have).
- New `onResume()` override:
  `lifecycleScope.launch { appUpdater.resumeIfStalled(updateFlowLauncher) }`.

The feature runs in **all build types** — no `BuildConfig.DEBUG` gate. Debug
builds carry the `.debug` application-id suffix, which is never on Play, so
step 2 fails with `ERROR_APP_NOT_OWNED` and the feature is a natural no-op —
while the Timber logs stay real in the one build where someone is watching
logcat. (A DEBUG gate was considered and rejected: it would make the logs
dead code and block testing on debug-signed builds.)

## FLEXIBLE flow

1. `checkOnLaunch` decides FLEXIBLE → register an `InstallStateUpdatedListener`
   → `startUpdateFlowForResult(…, FLEXIBLE)`. Play shows its own consent
   sheet; the download then runs in background.
2. Listener: on `InstallStatus.DOWNLOADED`, set `updateDownloaded = true` and
   unregister. On `INSTALLED`, `FAILED`, or `CANCELED`, just unregister —
   terminal states never surface UI. No download-progress UI in v1.
3. Restart prompt: surfaced through the existing shell plumbing —
   `ConnectedViewModel`
   (`apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/connected/ConnectedViewModel.kt`)
   injects `InAppUpdater` and re-exposes `updateDownloaded` plus
   `completeUpdate()` / `dismissUpdateBanner()` pass-throughs, exactly how it
   already re-exposes `deepLinkHandler.targets`.
4. `ConnectedScreen` collects it and shows a new `UpdateReadyBanner`
   composable in the shell `Column`, between the content `Box` and
   `ConnectedNavigationBar`, visible only when
   `updateDownloaded && !welcomeOwnsScreen` — the campus-event fullscreen
   welcome deliberately owns the whole screen (it already hides the nav bar
   and flips the system-bar icons), and the banner must respect that. The
   banner only renders inside the authenticated shell on purpose: onboarding
   stays uninterrupted, and a flexible download that completes mid-onboarding
   is picked up at next launch by step 3 of `checkOnLaunch`.
5. Tapping the action calls `completeUpdate()`; Play restarts the app itself.
   Tapping the close affordance calls `dismissUpdateBanner()` — session-only:
   the banner returns on next cold start via the `installStatus == DOWNLOADED`
   short-circuit, which is the desired gentle re-ask.

`UpdateReadyBanner` lives at
`apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/connected/UpdateReadyBanner.kt`
and follows the `AttentionBanner` pattern
(`apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/disciplines/components/AttentionBanner.kt`),
mirroring its exact plate recipe so the two read as siblings:
`RoundedCornerShape(18.dp)`, `.background(tint.copy(alpha = 0.14f).compositeOver(MaterialTheme.melon.surface.card))`,
`.border(1.dp, tint.copy(alpha = 0.30f), shape)` — with
`tint = MaterialTheme.colorScheme.primary`. `melon.status.ok` was considered
and rejected: the adaptive status trio marks graded academic signals
(pass/attention/fail), and a green banner would read as "success", not
"action available". Typography via `MaterialTheme.typography`, text action +
trailing close icon. `AnimatedVisibility` uses the house pattern (identical at
`HistorySemesterCard` and `LicensesGroupCard`):

```kotlin
enter = expandVertically(tween(280, easing = MelonMotion.EmphasizedEasing)) +
    fadeIn(tween(280, easing = MelonMotion.EmphasizedEasing)),
exit = shrinkVertically(tween(220, easing = MelonMotion.EmphasizedEasing)) +
    fadeOut(tween(220, easing = MelonMotion.EmphasizedEasing)),
```

(not `MelonMotion.spring()` — `AnimatedVisibility` needs a
`FiniteAnimationSpec`, and no existing `AnimatedVisibility` in the repo uses a
spring). No new design-system tokens are needed; the component stays
feature-local like `AttentionBanner` does. There is no snackbar host anywhere
in the app — do not introduce one for this. Bare `@Preview` included, per
house style.

## IMMEDIATE flow

`startUpdateFlowForResult(…, IMMEDIATE)` hands the screen to Play's fullscreen
updater. If the user backgrounds the app mid-update, `resumeIfStalled` (called
from `MainActivity.onResume`) re-fetches `appUpdateInfo` and, when
`startedImmediate` is set and
`updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS`,
restarts the flow with the same launcher so the update can't be wedged
half-applied. `RESULT_CANCELED` from an immediate flow is accepted — we do not
loop the prompt; the user gets asked again next cold start (and staleness will
eventually escalate).

## Failure handling

Every failure path is silent — the app must behave exactly as if the feature
didn't exist:

| Case | Behavior |
| --- | --- |
| Play unavailable (sideload, emulator, `.debug` app id, no Play account) | `requestAppUpdateInfo()` throws (e.g. `ERROR_APP_NOT_OWNED`); caught, logged, done |
| User declines (`RESULT_CANCELED`) | Record decline day; FLEXIBLE suppressed 7 days; at most one prompt per cold start |
| `RESULT_IN_APP_UPDATE_FAILED` | Log, stop |
| `startUpdateFlowForResult` returns `false` (flow could not start) | Log, stop |
| Download fails mid-flexible (`InstallStatus.FAILED`) | Unregister listener, no UI |
| Activity destroyed mid-check | `lifecycleScope` cancellation kills the coroutine before it can touch the dead launcher |

No error states, no retry UI, no Crashlytics non-fatals — Timber debug logs
only (visible in debug builds, silent in release; see the logging-lane note
under API surface).

## Strings

pt-BR defaults in `apps/android/app/src/main/res/values/strings.xml`, English
mirror in `values-en/strings.xml`, new
`<!-- region Connected · Atualização do app -->` (matching the existing
`Area · Sub` region convention, e.g. `Connected · Tab bar`):

| Key | pt-BR | en |
| --- | --- | --- |
| `update_ready_title` | Atualização pronta | Update ready |
| `update_ready_body` | Uma nova versão do UNES já foi baixada. | A new version of UNES has been downloaded. |
| `update_ready_action` | Reiniciar | Restart |
| `update_ready_dismiss` | Dispensar | Dismiss |

(`update_ready_dismiss` is the close icon's `contentDescription`.) Play
renders all consent/progress UI for both flows itself, so these four banner
strings are the entire user-facing surface we own.

## Testing

- `UpdateStrategyTest` — plain-JUnit table test over `chooseUpdateType`,
  mirroring `apps/android/app/src/test/kotlin/dev/forcetower/unes/ui/feature/connected/DeepLinkParserTest.kt`.
  Covers every row of the strategy table plus the
  `availability != UPDATE_AVAILABLE`, `stalenessDays == null`,
  `isUpdateTypeAllowed == false`, and `flexibleSuppressed` branches.
- A `FakeAppUpdateManager`-driven flow test was considered and **cut from
  v1**: the fake's only constructor takes `android.content.Context`, the repo
  has no Robolectric (and adding it means a catalog entry, a test dependency,
  and `unitTests` configuration in the shared convention plugin), and the test
  would mostly exercise Google's own fake. If a second consumer ever justifies
  Robolectric as its own infra change, the fake supports everything needed —
  `setUpdateAvailable`, `setUpdatePriority`, `setClientVersionStalenessDays`,
  `setUpdateNotAvailable`, `userAcceptsUpdate`/`userRejectsUpdate`,
  `downloadStarts`/`downloadCompletes`/`downloadFails`.
- Manual: upload a release-signed build to **internal app sharing**, install
  the previous link's build on a device, open the newer link so Play learns of
  the update, then launch the app. Internal app sharing ignores the 12-hour
  Play caching, so both flows are exercisable in minutes. Against real Play,
  staleness only accrues by waiting — those branches are covered by the
  strategy test, or by temporarily hardcoding `chooseUpdateType` inputs during
  a manual pass.

## Non-goals (v1)

- **No backend/remote-config kill switch or forced-update gate.** The app does
  have Firebase Remote Config infrastructure
  (`apps/android/app/src/main/kotlin/dev/forcetower/unes/firebase/FeatureFlags.kt`),
  so a `update_required_min_version` gate would be cheap to add later — but
  `updatePriority` set at release time already covers "everyone must take this
  build", so v1 doesn't duplicate it.
- No download-progress UI for the flexible flow.
- No update prompts during onboarding (banner is Connected-shell only).
- No Settings "check for updates" entry.
- No analytics events on prompt/accept/decline.
- No Robolectric (see Testing — separate infra change if ever justified).
- Nothing on iOS or in `packages/shared-kmp`.

## File-by-file change list

New files:

| File | Contents |
| --- | --- |
| `apps/android/app/src/main/kotlin/dev/forcetower/unes/update/InAppUpdater.kt` | `chooseUpdateType` pure function + `InAppUpdater` singleton + file-local suppression DataStore (one file, like `DeepLinks.kt` bundles parser + handler) |
| `apps/android/app/src/main/kotlin/dev/forcetower/unes/di/PlayModule.kt` | `@Provides @Singleton AppUpdateManager` |
| `apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/connected/UpdateReadyBanner.kt` | Restart banner composable + preview |
| `apps/android/app/src/test/kotlin/dev/forcetower/unes/update/UpdateStrategyTest.kt` | Strategy table test |

Modified files:

| File | Change |
| --- | --- |
| `gradle/libs.versions.toml` | version + library entries |
| `apps/android/app/build.gradle.kts` | dependency + licensee `allowUrl` |
| `apps/android/app/src/main/kotlin/dev/forcetower/unes/MainActivity.kt` | inject `InAppUpdater`, launcher field delegating results, unconditional lifecycleScope-launched `checkOnLaunch` in `onCreate`, new `onResume` |
| `apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/connected/ConnectedViewModel.kt` | inject `InAppUpdater`, expose `updateDownloaded` + `completeUpdate()` + `dismissUpdateBanner()` |
| `apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/connected/ConnectedScreen.kt` | collect state, render `UpdateReadyBanner` above the nav bar, gated on `!welcomeOwnsScreen` |
| `apps/android/app/src/main/res/values/strings.xml` | four pt-BR strings |
| `apps/android/app/src/main/res/values-en/strings.xml` | four English strings |
