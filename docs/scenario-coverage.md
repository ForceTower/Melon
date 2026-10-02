# Screen scenario coverage inventory

Inspected on October 2, 2026. This is the phone/tablet client inventory, based on
the Android ViewModel/state models and native iOS reducers and views linked below.
Watch screens, widgets, Siri, and system permission dialogs are separate surfaces;
their integration gaps are called out rather than counted as covered phone screens.

**Only the pilot IDs in the first table are registered executable scenarios.** The
remaining tables are the backlog for named fixtures and installed-app journeys,
not claims that those scenarios or screenshots already exist. Existing unit tests
and previews protect portions of these features; their presence does not make a
screen an automated scenario. Execution results live in CI artifacts and the
[testing guide](testing.md), not in this inventory.

## Executable pilot

The [shared catalog](../scripts/scenarios.ts) owns these stable IDs and the
[synthetic contract](../contracts/v1/pilot.json) owns their account and clock.
Android's scenario command accepts the catalog IDs; see the platform guide for
the exact command and current execution evidence. Native iOS uses a real HTTP
login and initial sync with an in-memory GRDB mirror in the
[iOS verification command](ios-testing.md).

| ID | Android entry point | Native iOS evidence / remaining scope |
| --- | --- | --- |
| `auth.login` | Device journey executed; credential form entry checked. | XCUITest asserts an initially disabled submit button, enters synthetic credentials, and reaches Ready through real repositories. |
| `auth.invalid-credentials` | Device journey executed, including successful retry. | XCUITest checks the credential error copy, a real login request, and that Ready is not reached. |
| `sync.unavailable` | Device journey executed, including successful retry. | No installed-app case yet. `SyncFeature` has step progress and auth-failure routing, but no separate non-auth failure screen. |
| `home.populated` | Device journey executed. | XCUITest reaches Home and finds the synthetic discipline after initial sync. |
| `home.offline-with-cache` | Device journey executed; counted failed HTTP refresh before checking cache. | XCUITest waits for a refresh request against the unavailable server before checking retained content; a shared-contract repository test verifies the GRDB cache and sync timestamp survive failure. |
| `auth.session-expired` | Device journey executed; 401, rejected refresh, and recovery banner checked. | Reducer/token-refresh tests exist, but the native UI harness does not yet reproduce the expired-session banner from a rejected HTTP refresh. |
| `messages.empty` | Device journey executed; successful empty inbox checked. | XCUITest opens Messages, confirms an actual request, and checks the successful empty-state label. |

`sync.ready` is an iOS screenshot attachment name reached by the login journey,
not an independently registered backend scenario. Native artifacts contain seven
screenshots and matching hierarchies, including the login accessibility check.
They are review evidence, not approved image-difference baselines. The automated
iOS accessibility audit currently checks login element detection and traits.

## How to read the remaining inventory

Every row below is **planned scenario coverage** unless it references a pilot ID.
The suggested family is a naming prefix, not a supported CLI argument yet.

- **N/A** means the state does not apply to that screen's responsibility, such as
  an HTTP loading screen for a local calculator or static welcome page.
- **Not modeled** means an operation can fail but the inspected state/view has no
  separate representation of that failure. It is a visible coverage/design gap,
  not an instruction to invent an error screen in a test fixture.
- Permission-denied states belong to the actual notification, passkey, camera,
  scanner, or biometric interaction. They are N/A for ordinary read-only tab
  content. Expired authentication is handled through the shell/Home recovery
  surfaces; do not create a separate login screen for every tab.
- Gate-off cases should verify the actual entry-point behavior. The
  [Android gates][a-gates] and [native Me gates][i-me] hide feature shortcuts; a
  hidden shortcut is not evidence that a direct destination is inaccessible.

### Entry, account, and recovery

| Screen / proposed family | Meaningful cases and transitions | Source and applicability notes |
| --- | --- | --- |
| Splash and root shell — `launch.*` | Signed-out routing to onboarding; saved session routing to tabs; tab/deep-link destination; logout clears the connected flow; native legacy migration outcomes. | [Android routing][a-routes], [Splash][a-splash], [native RootFeature][i-root]. Empty content and a screen-specific permission-denied state are N/A. |
| Welcome — `onboarding.welcome.*` | Start intro; jump directly to login; back navigation. | [Android onboarding][a-onboarding], [native onboarding][i-onboarding]. Loading, offline, HTTP error, and data-empty states are N/A. |
| Intro carousel — `onboarding.intro.*` | Each slide, next/back/skip, final-slide notification authorization granted/denied; continue to login regardless of the OS permission result. | Android `IntroCarouselScreen`; native `IntroFeature`. Permission outcome is a platform interaction, not an extra carousel screen. |
| Login — `auth.*` | Empty/partial/valid form; submitting; valid/invalid credentials; network/server failure; password visibility; passkey success/cancellation/unavailable; Android email-style username warning. | [Android LoginViewModel][a-login], [native LoginFeature][i-login]. Two credential cases are pilot; passkeys and recovery are still planned UI coverage. |
| Initial sync — `sync.*` | Each step active/completed; delayed onboarding status; usable Ready data; authentication failure; interruption/re-entry. Android failed/retry path. | [Android SyncViewModel][a-sync], [native SyncFeature][i-sync]. Native non-auth errors advance tolerant steps and may poll; a dedicated failure view is not modeled there. |
| Ready — `sync.ready.*` | Overview with/without next class, grades, attendance, or semester; enter connected shell. Android initially loading. | [Android ReadyViewModel][a-ready], [native ReadyFeature][i-ready]. iOS receives an already-built `ReadyOverview`, so a separate fetch/loading state is N/A. |
| Expired session / upstream credentials — `auth.recovery.*` | Expired Melon token; invalid stored portal credentials; keep cached content; reopen login; password submitting/rejected/network failure; captcha-required reauthentication; successful recovery refreshes. | [Android OverviewViewModel][a-home], [native Home/ReauthFeature][i-home]. These are distinct failures. Native installed-app expiry and captcha cases remain unimplemented in the scenario harness. |
| Profile editor, photo picker/crop — `profile.edit.*` | Name edit/reset; keep/remove/replace photo; save in flight; save failure/retry; successful updated identity; picker/camera/crop cancellation. | [Android MeViewModel][a-me], [native ProfileEditFeature][i-me]. Camera/photo authorization needs a controlled system interaction; no dedicated permission-denied reducer state is declared. |
| Logout / farewell — `auth.logout.*` | Confirmation/cancel; flashing transition; signed-out farewell; next login must not retain the previous account's cache. | Android `LogoutStep` in [MeViewModel][a-me]; native `FarewellFeature` and [RootFeature][i-root]. HTTP loading/error and data-empty farewell screens are N/A. |

### Academic tabs and local tools

| Screen / proposed family | Meaningful cases and transitions | Source and applicability notes |
| --- | --- | --- |
| Home — `home.*` | Populated and no academic data; upcoming/live/day-done hero; no next class; absent grades/attendance/exam/messages; cached refresh failure; semester boundary; banner precedence; campus-event gate on/off. Native retrospective banner eligibility/seen/dismissed. | [Android Overview state and hero models][a-home], [native HomeFeature][i-home]. Populated/offline are pilot. Android has no dedicated top-level loading/error field; native has `isLoading`/`errorMessage`. |
| Schedule agenda — `schedule.agenda.*` | Selected weekday; free day; future/current/completed classes; expanded class actions; cached schedule; class-to-detail navigation. | [Android Schedule][a-schedule], [native ScheduleFeature][i-schedule]. Native declares loading/error; Android's `raw` schedule model has no separate fetch-error state. |
| Schedule grid / class sheet — `schedule.grid.*` | Empty/populated week; overlapping class layout; current-time marker; select/dismiss class; switch agenda/grid preference; compact/wide layouts. | [Android grid][a-grid], [native ScheduleGridFeature][i-grid]. Native declares loading/error; Android shares `ScheduleUiState`. No separate permission state. |
| Disciplines list/history — `disciplines.*` | No/current/past semesters; pending historical downloads; downloading/completed/failed download; missing/partial/final grades; open a discipline. | [Android DisciplinesListViewModel][a-disciplines], [native DisciplinesFeature][i-disciplines]. Native exposes initial load error; Android exposes `downloadError`, not an initial top-level fetch-error screen. |
| Discipline detail — `discipline.detail.*` | Seed versus hydrated detail; group switch; grades absent/partial/final; attendance; lessons/materials present/absent; material gate off. | [Android DisciplineDetailViewModel][a-detail], [native DisciplineDetailFeature][i-detail]. Neither state declares a dedicated fetch-error field; retain and test the actual seed/cache behavior. |
| Messages inbox — `messages.*` | Loading, populated, empty, filtered empty; unread/read and starred filters; incremental reveal/paging; mark read; cached content after refresh failure. | [Android MessagesViewModel][a-messages], [native MessagesFeature][i-messages]. Successful empty is pilot. Native has `errorMessage`; Android declares loading but no separate fetch-error state. |
| Message detail / share — `message.detail.*` | Seed/detail hydration on Android; read/star toggles; body and attachments; share open/dismiss; notification points to missing or unmirrored content. | Android `MessageDetailScreen` and [MessagesViewModel][a-messages]; [native MessageDetailFeature][i-message-detail]. Native detail starts with a `MessageItem`; an independent initial-fetch spinner is N/A. |
| Calendar agenda/month/event detail — `calendar.*` | No events; populated agenda/month; category/scope filters; selected day with/without events; academic and personal events; detail open/dismiss. | [Android Calendar][a-calendar], [native CalendarFeature][i-calendar]. Both state models lack a dedicated remote loading/failure screen. |
| Personal event composer — `calendar.personal.*` | New/edit; blank title disables save; optional end/reminder/discipline/notes; save/cancel; delete confirmation. Android drops an end that is not after start. | Android `components/CalPersonalEventSheet`; native `CalendarPersonalEventFeature`. This is local editing: remote loading/expired-session states are N/A. Reminder authorization belongs to the notification integration. |
| Final Countdown — `countdown.*` | Free calculation versus a selected discipline; blank and entered rows; weighted/unweighted; verdicts `passed`, `ontrack`, `borderline`, `borderlineFinal`, `final`, `impossible`, `failed`, `failingTrack`, `empty`; next-semester label. | [Android calculator][a-countdown], [native calculator][i-countdown]. Network loading/error and permission-denied calculation screens are N/A; seed academic choices from a controlled mirror. |
| Folio Runner — `folio.*` | Ready → playing → game over → restart; jump/duck; saved best score; exit; native Paper-icon unlock. | [Android engine][a-folio], [native engine/view][i-folio]. HTTP loading/error, auth expiry, and remote empty states are N/A. Freeze game time/randomness before adding screenshots. |
| Retrospective — `retrospective.*` | Eligibility window; announce/seen; deck/card index; pause/resume; reduced motion; share open/dismiss; finish. | [Native RetrospectiveFeature][i-retrospective]. No corresponding Android feature/routed screen was found: Android N/A for this inventory. A dedicated HTTP loading/error state is not declared by the native reducer. |

### Enrollment, progress, events, and content

| Screen / proposed family | Meaningful cases and transitions | Source and applicability notes |
| --- | --- | --- |
| Enrollment hub — `enrollment.window.*` | Initial load/failure/retry; not eligible/unavailable; upcoming/open/closed/unknown window; offers fetch fails independently; resume existing selection; reopen submitted proposal. | [Android EnrollmentModels/ViewModel][a-enrollment], [native EnrollmentFeature/session][i-enrollment]. Gate-off entry is a separate Me scenario. Picks/catalogue are not a durable offline draft; do not claim offline submission. |
| Enrollment offers/search — `enrollment.offers.*` | Empty/populated catalogue; mandatory/optional filter; query with/no matches; selected versus unselected discipline. | Android `EnrollmentOffersScreen`; native `EnrollmentOffersFeature`, using the same parent session. Independent child fetch/loading and permission states are N/A. |
| Enrollment discipline/section picker — `enrollment.sections.*` | Seat available/tight/full; queue enabled/disabled; selected section; collision with existing pick; unmet prerequisite warning; section without a timetable. | Android `EnrollmentModels`/`EnrollmentDisciplineScreen`; native `EnrollmentDisciplineFeature`. Prerequisites are warnings in the Android model, not an invented client-side submission blocker. |
| Enrollment timetable — `enrollment.timetable.*` | Scheduled choices; overlapping choices; unscheduled-choice count; empty selection. | Android `EnrollmentTimetableScreen`; native `EnrollmentTimetableFeature`. Projection of the parent session; independent network and permission states are N/A. |
| Enrollment review — `enrollment.review.*` | Empty proposal; under minimum/over maximum hours; conflicts; valid proposal; readonly/reopened; allows-other/waitlist options; submitting; server failure/retry. | Android `EnrollmentBlocker`/`canSubmit`; native `EnrollmentReviewFeature`. Mock-only submissions must replace the complete synthetic proposal, never a real enrollment. |
| Enrollment success — `enrollment.success.*` | Submitted proposal summary, preference counts, done/return navigation. | Android `EnrollmentSuccessScreen`; native `EnrollmentSuccessFeature`. Fetch-loading/offline-error/permission states are N/A; submission failure belongs to Review. |
| Course progress/version picker — `progress.*` | Missing/loading/failed/populated progress; complementary-hours explainer; version picker; switch in flight/failure/success. | [Android CourseProgressViewModel][a-progress], [native CourseProgressFeature][i-progress]. Gate-off shortcut and successful cached display need distinct fixtures. |
| Curriculum flow/entry sheet — `curriculum.*` | Period/type lens; selected period; prerequisite trail/no trail; entry sheet; manual completion in flight/failure/success. | Android shares `CourseProgressUiState`; native `CurriculumFlowFeature`. Initial content comes from progress; do not add an independent HTTP loading screen without implementation. |
| Campus event hub/welcome — `campus-event.*` | No current event / hidden Home card; welcome unseen/seen/dismissed; upcoming/live/ended event; selected day and audience filter, including no matches; refreshing on Android. | [Android CampusEventViewModel][a-campus], [native CampusEventFeature][i-campus]. Native entry requires an event value; no standalone native load-error state is declared. |
| Campus activity/workshop/speaker/venue/organization destinations — `campus-event.detail.*` | Each destination with populated and missing optional metadata; registration/link actions; list filters/empty collections where present. | Detail screens in [Android campus event][a-campus] and [native campus event][i-campus]. They consume the event payload; independent initial HTTP loading/error is N/A. |
| Paradoxo overview/search — `paradoxo.*` | Loading/failure/retry; overview and rankings; query matching disciplines/teachers or neither. | [Android ParadoxoUiState][a-paradoxo], [native ParadoxoFeature][i-paradoxo]. Feature gate-off is separate from an empty search. |
| Paradoxo ranked explore list — `paradoxo.explore.*` | Ranking category, populated/empty entries, entry navigation. | Android `ParadoxoExploreScreen`; native `ParadoxoExploreFeature` receives a ranking. Independent network loading/error is N/A. |
| Paradoxo discipline/teacher detail — `paradoxo.detail.*` | Loading/failed/loaded detail; retry; missing optional statistics; expand a teacher within discipline detail. | Android `ParadoxoDetail.Loading/Failed/Loaded`; native discipline/teacher reducers in [Paradoxo][i-paradoxo]. Permission state is N/A. |
| Library search/advanced form — `library.search.*` | Overview loading/failure/retry; recent searches present/cleared; query/scope; advanced terms; submit/dismiss. | [Android LibraryUiState][a-library], [native LibraryFeature][i-library]. Gate-off entry is separate; no account/permission screen is defined here. |
| Library results/refinement — `library.results.*` | Too-broad query; initial/page loading; empty/populated/filtered-empty; sort/facets/grouping; availability checking/unavailable; pagination and duplicate results. | Android `LibraryResultsScreen` has first-page and append `LoadState.Error`/retry. Native `searchFailed` clears results on reset and has no distinct search-error view: document/test that gap, do not label it a successful empty response. |
| Library work detail — `library.work.*` | Work metadata with optional/missing fields; availability/copies and refresh; record expanded/collapsed; copy call number/ISBN/reference/ID confirmation. | Android `library/work`; native `LibraryWorkDetailFeature`. Work is handed in by the parent; independent initial-load state is N/A. |
| Materials hub — `materials.*` | Loading/load failed/retry; overview with/without disciplines/materials; saved shelf; upload entry. | [Android MaterialsHubViewModel][a-materials], [native MaterialsFeature][i-materials]. Gate-off shortcut is separate. |
| Materials discipline list — `materials.list.*` | Seed header before detail; loading/load failed/retry; empty/published/own materials; text/type filter and no matches; upload completion refresh. | Android `MaterialsListUiState`; native `MaterialsListFeature`. Keep own moderation states distinct from the public shelf. |
| Saved materials — `materials.saved.*` | Loading/load failed/retry; empty versus grouped saved materials; open detail. | Android `MaterialsSavedUiState`; native `MaterialsSavedFeature`. “Saved” denotes the server-side collection; it is not proof the file is downloaded for offline use. |
| Material detail/report — `material.detail.*` | Published versus own pending/rejected status; useful/save toggles and rollback/failure toast; file opening/success/failure; report reason and confirmation; own-upload edit. | Android `MaterialsDetailUiState`; native `MaterialsDetailFeature`. Native is seeded with a material; Android also declares initial load/failure. |
| Upload/file source/scanner — `materials.upload.*` | Discipline picker/locked discipline; file pick/cancel/read failure; details validation; guidelines acceptance; submit in flight/failure/success; scanner/picker failure. | Android `MaterialsUploadUiState`; native `MaterialsUploadFeature`. System camera/scanner authorization needs its own controlled interaction; there is no dedicated permission-denied reducer field. |

### Profile hub, settings, and documents

| Screen / proposed family | Meaningful cases and transitions | Source and applicability notes |
| --- | --- | --- |
| Me hub — `me.*` | Identity/profile/score present or absent; each remote shortcut enabled/disabled; open document/tool/settings; profile save feedback; logout entry. | [Android MeUiState][a-me], [native MeFeature][i-me]. Neither declares a top-level fetch-error screen; test cached/partial identity as it exists. |
| Certificate/history document sheet — `documents.*` | Intro, saved, captcha, generating, fresh, stale cached copy, no-copy failure; retry/refresh; open/share/export a synthetic document. | Android `DocumentStage`; native `MeDocumentFeature.Stage`. Connection/unavailable failures differ from stale fallback. Gate-off certificate/history entries must also be covered. |
| Settings — `settings.*` | System/light/dark theme; agenda/grid preference; spoiler levels; notification toggles; reminder flag and preference; credential hidden/revealed/copied; failed credential retrieval or mutation where represented. | [Android SettingsUiState][a-settings], [native SettingsFeature][i-settings]. Native also has biometric password reveal, app icons, secret-icon discovery and celebration/toast states. Neither has a generic full-screen loading/error state. |
| Passkeys list/add/detail — `passkeys.*` | No keys/populated/loading/load failure where represented; choose device/security key; platform auth/cancel/failure/success; edit name in flight/failure; delete confirmation/in flight/failure/success. | [Android PasskeysContract][a-passkeys], [native PasskeysFeature][i-passkeys]. Native declares creation/mutation errors and `hasLoaded`, but no independent list-load error field. Platform auth should be injected for deterministic tests. |
| About and license screens — `about.*`, `licenses.*` | About/build info and copy feedback; license groups/search/filter/expanded entry/copy; Android asset loading, missing manifest, and export availability. | Android [licenses][a-licenses] and Me; native [LicensesFeature][i-licenses] and Me. Remote offline/auth-expired/permission states are N/A for bundled license content. |

## Cross-cutting cases still needing fixtures

Use each feature's real state model when implementing these. They are not all
cross-products of every screen:

1. Initial empty storage, already-cached storage, failed refresh, process death,
   relaunch, logout, and account change. Distinguish memory-cache evidence from
   persistence and upgrade evidence.
2. Same stable ID under light/dark appearance, large text, compact/wide layout,
   and a second OS version. The native pilot's reducer clock is fixed, but its
   SwiftUI display timelines still use system time; pixel baselines need that
   clock pinned first.
3. Notifications denied/granted, notification deep links, passkey cancellation,
   camera/scanner failure, biometric rejection, and external file open/share.
   Inject platform results and use synthetic files; do not replace a failure
   with a success stub simply to obtain a screenshot.
4. Feature gates at both values, including stale saved flags and unavailable
   entry points. Explicitly exercise production behavior instead of depending
   on debug defaults.
5. Intent/entity declarations run in native CI. Out-of-process Siri/Spotlight
   checks remain constrained by the documented Apple runtime error and missing
   persisted synthetic mirror; see [iOS verification](ios-testing.md).

When adding a planned case, register an exact ID in the catalog, supply synthetic
origin/version/storage/time/flags, state its expected visible outcome and actions,
and wire a behavioral assertion before collecting its screenshot. Keep unknown
HTTP requests failing and update this inventory in the same change.

[a-routes]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/navigation/AppRoutes.kt
[a-splash]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/splash/SplashViewModel.kt
[a-onboarding]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/onboarding
[a-login]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/onboarding/login/LoginViewModel.kt
[a-sync]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/onboarding/sync/SyncViewModel.kt
[a-ready]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/onboarding/ready/ReadyViewModel.kt
[a-home]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/overview
[a-schedule]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/schedule
[a-grid]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/schedule/grid
[a-disciplines]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/disciplines/DisciplinesListViewModel.kt
[a-detail]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/disciplinedetail/DisciplineDetailViewModel.kt
[a-messages]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/messages/MessagesViewModel.kt
[a-calendar]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/calendar
[a-countdown]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/finalcountdown
[a-folio]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/foliorunner/FolioRunnerEngine.kt
[a-enrollment]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/enrollment
[a-progress]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/courseprogress/CourseProgressViewModel.kt
[a-campus]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/campusevent
[a-paradoxo]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/paradoxo/ParadoxoModels.kt
[a-library]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/library
[a-materials]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/materials
[a-me]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/me/MeViewModel.kt
[a-settings]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/settings
[a-passkeys]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/settings/passkeys/PasskeysContract.kt
[a-licenses]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/ui/feature/licenses
[a-gates]: ../apps/android/app/src/main/kotlin/dev/forcetower/unes/remote/FeatureFlags.kt
[i-root]: ../apps/ios/UNESKit/Sources/UNESKit/App/RootFeature.swift
[i-onboarding]: ../apps/ios/UNESKit/Sources/UNESKit/Features/Onboarding
[i-login]: ../apps/ios/UNESKit/Sources/UNESKit/Features/Onboarding/Login/LoginFeature.swift
[i-sync]: ../apps/ios/UNESKit/Sources/UNESKit/Features/Onboarding/Sync/SyncFeature.swift
[i-ready]: ../apps/ios/UNESKit/Sources/UNESKit/Features/Onboarding/Ready/ReadyFeature.swift
[i-home]: ../apps/ios/UNESKit/Sources/UNESKit/Features/Home
[i-schedule]: ../apps/ios/UNESKit/Sources/UNESKit/Features/Schedule/ScheduleFeature.swift
[i-grid]: ../apps/ios/UNESKit/Sources/UNESKit/Features/ScheduleGrid/ScheduleGridFeature.swift
[i-disciplines]: ../apps/ios/UNESKit/Sources/UNESKit/Features/Disciplines/DisciplinesFeature.swift
[i-detail]: ../apps/ios/UNESKit/Sources/UNESKit/Features/Disciplines/Detail/DisciplineDetailFeature.swift
[i-messages]: ../apps/ios/UNESKit/Sources/UNESKit/Features/Messages/MessagesFeature.swift
[i-message-detail]: ../apps/ios/UNESKit/Sources/UNESKit/Features/Messages/Detail/MessageDetailFeature.swift
[i-calendar]: ../apps/ios/UNESKit/Sources/UNESKit/Features/Calendar
[i-countdown]: ../apps/ios/UNESKit/Sources/UNESKit/Features/FinalCountdown
[i-folio]: ../apps/ios/UNESKit/Sources/UNESKit/Features/FolioRunner/FolioRunnerView.swift
[i-retrospective]: ../apps/ios/UNESKit/Sources/UNESKit/Features/Retrospective/RetrospectiveFeature.swift
[i-enrollment]: ../apps/ios/UNESKit/Sources/UNESKit/Features/Enrollment
[i-progress]: ../apps/ios/UNESKit/Sources/UNESKit/Features/CourseProgress
[i-campus]: ../apps/ios/UNESKit/Sources/UNESKit/Features/CampusEvent
[i-paradoxo]: ../apps/ios/UNESKit/Sources/UNESKit/Features/Paradoxo
[i-library]: ../apps/ios/UNESKit/Sources/UNESKit/Features/Library
[i-materials]: ../apps/ios/UNESKit/Sources/UNESKit/Features/Materials
[i-me]: ../apps/ios/UNESKit/Sources/UNESKit/Features/Me
[i-settings]: ../apps/ios/UNESKit/Sources/UNESKit/Features/Settings/SettingsFeature.swift
[i-passkeys]: ../apps/ios/UNESKit/Sources/UNESKit/Features/Settings/Passkeys/PasskeysFeature.swift
[i-licenses]: ../apps/ios/UNESKit/Sources/UNESKit/Features/Licenses/LicensesFeature.swift
