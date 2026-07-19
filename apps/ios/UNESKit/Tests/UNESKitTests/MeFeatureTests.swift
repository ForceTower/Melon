import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

@MainActor
struct MeFeatureTests {
    static nonisolated let referenceDate = Date(timeIntervalSince1970: 1_776_000_000)

    @Test
    func taskHydratesFromTheMirror() async {
        let cached = CachedMeOverview(overview: .preview, syncedAt: Self.referenceDate)
        let (updates, mirror) = AsyncStream.makeStream(of: CachedMeOverview.self)
        mirror.yield(cached)
        mirror.finish()

        let store = TestStore(initialState: MeFeature.State()) {
            MeFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.sessionStore = .inMemory(initial: .preview)
            $0.meRepository.observe = { updates }
            $0.profileRepository.current = { throw APIError.emptyEnvelope }
        }

        await store.send(.task) {
            $0.userName = "Mariana Souza"
        }
        await store.receive(.overviewUpdated(cached)) {
            $0.overview = .preview
        }
    }

    @Test
    func profileFeedsTheIdentityLines() async {
        let store = TestStore(initialState: MeFeature.State()) {
            MeFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.sessionStore = .inMemory(initial: .preview)
            $0.meRepository.observe = { .finished }
            $0.profileRepository.current = { .preview }
        }

        await store.send(.task) {
            $0.userName = "Mariana Souza"
        }
        await store.receive(.profileLoaded(.preview)) {
            $0.profile = .preview
        }
    }

    @Test
    func calendarShortcutPushesTheCalendar() async {
        let store = TestStore(initialState: MeFeature.State()) {
            MeFeature()
        }

        await store.send(.shortcutTapped(.calendar)) {
            $0.path.append(.calendar(CalendarFeature.State()))
        }
    }

    @Test
    func deeplinkRewritesThePathToTheOrganicStack() async {
        // Existing depth on the tab — the deeplink must land predictably
        // anyway, replacing the stack rather than piling on top of it.
        var initialState = MeFeature.State()
        initialState.path.append(.calendar(CalendarFeature.State()))
        let material = Material.preview()[0]

        let store = TestStore(initialState: initialState) {
            MeFeature()
        }

        await store.send(.deeplinkOpened(.material(material))) {
            var path = StackState<MeFeature.Path.State>()
            path.append(.materials(MaterialsFeature.State()))
            path.append(.materialsDetail(MaterialsDetailFeature.State(material: material)))
            $0.path = path
        }
    }

    @Test
    func resumeWakesAPushedCalendar() async {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Self.referenceDate)

        var initialState = MeFeature.State(userName: "Mariana Souza")
        initialState.overview = .preview
        initialState.path.append(.calendar(CalendarFeature.State()))

        let store = TestStore(initialState: initialState) {
            MeFeature()
        } withDependencies: {
            $0.calendar = calendar
            $0.date = .constant(Self.referenceDate)
            $0.sessionStore = .inMemory(initial: .preview)
            $0.meRepository.observe = { .finished }
            $0.eventsRepository.calendar = { _ in [] }
        }

        await store.send(.task)
        await store.receive(.path(.element(id: 0, action: .calendar(.task)))) {
            var woken = CalendarFeature.State()
            woken.today = today
            woken.selectedDay = today
            $0.path[id: 0] = .calendar(woken)
        }
        await store.receive(.path(.element(id: 0, action: .calendar(.eventsLoaded([]))))) {
            var refreshed = CalendarFeature.State()
            refreshed.today = today
            refreshed.selectedDay = today
            refreshed.fetchedAt = Self.referenceDate
            $0.path[id: 0] = .calendar(refreshed)
        }
    }

    @Test
    func countdownShortcutPushesTheCalculator() async {
        let store = TestStore(initialState: MeFeature.State()) {
            MeFeature()
        }

        await store.send(.shortcutTapped(.countdown)) {
            $0.path.append(.countdown(FinalCountdownFeature.State()))
        }
    }

    @Test
    func settingsRowPushesTheSettingsScreen() async {
        let store = TestStore(initialState: MeFeature.State(userName: "Mariana Souza")) {
            MeFeature()
        }

        await store.send(.settingsRowTapped(.settings)) {
            $0.path[id: 0] = .settings(SettingsFeature.State(userName: "Mariana Souza"))
        }
    }

    @Test
    func aboutRowPresentsTheSheetAndRefinesTheChannel() async {
        let resolved = AppInfo(
            version: AppInfo.preview.version,
            build: AppInfo.preview.build,
            machineId: AppInfo.preview.machineId,
            deviceModel: AppInfo.preview.deviceModel,
            osVersion: AppInfo.preview.osVersion,
            channel: "TestFlight",
            installSource: "TestFlight"
        )

        let store = TestStore(initialState: MeFeature.State()) {
            MeFeature()
        } withDependencies: {
            $0.appInfo.current = { .preview }
            $0.appInfo.resolved = { resolved }
        }

        await store.send(.settingsRowTapped(.about)) {
            $0.aboutInfo = .preview
        }
        await store.receive(.aboutInfoResolved(resolved)) {
            $0.aboutInfo = resolved
        }
        await store.send(.aboutDismissed) {
            $0.aboutInfo = nil
        }
    }

    @Test
    func aboutCopyPutsTheDebugBlockOnThePasteboard() async {
        let clock = TestClock()
        let copied = LockIsolated<String?>(nil)

        let store = TestStore(initialState: MeFeature.State()) {
            MeFeature()
        } withDependencies: {
            $0.appInfo.current = { .preview }
            $0.appInfo.resolved = { .preview }
            $0.pasteboard.copy = { copied.setValue($0) }
            $0.continuousClock = clock
        }

        await store.send(.settingsRowTapped(.about)) {
            $0.aboutInfo = .preview
        }
        await store.receive(.aboutInfoResolved(.preview))

        await store.send(.aboutCopyTapped) {
            $0.isAboutCopied = true
        }
        await clock.advance(by: .milliseconds(1800))
        await store.receive(.aboutCopyFeedbackExpired) {
            $0.isAboutCopied = false
        }
        #expect(copied.value == AppInfo.preview.debugText)

        await store.send(.aboutDismissed) {
            $0.aboutInfo = nil
        }
    }

    @Test
    func licensesRowPushesTheLicensesScreen() async {
        let store = TestStore(initialState: MeFeature.State()) {
            MeFeature()
        }

        await store.send(.settingsRowTapped(.licenses)) {
            $0.path[id: 0] = .licenses(LicensesFeature.State())
        }
    }

    @Test
    func logoutWipesTheMirrorThemeAndSession() async {
        let sessionStore = SessionStore.inMemory(initial: .preview)
        let wiped = LockIsolated(false)
        let unregistered = LockIsolated(false)

        let initialState = MeFeature.State(userName: "Mariana Souza")
        initialState.$theme.withLock { $0 = .dark }

        let store = TestStore(initialState: initialState) {
            MeFeature()
        } withDependencies: {
            $0.sessionStore = sessionStore
            $0.meRepository.wipeLocalData = { wiped.setValue(true) }
            $0.push.unregister = { unregistered.setValue(true) }
        }

        await store.send(.logoutTapped) {
            $0.isLogoutPromptPresented = true
        }
        await store.send(.logoutConfirmed) {
            $0.isLogoutPromptPresented = false
            $0.$theme.withLock { $0 = .system }
        }
        await store.receive(.delegate(.loggedOut(firstName: "Mariana")))

        #expect(sessionStore.current() == nil)
        #expect(wiped.value == true)
        #expect(unregistered.value == true)
    }

    @Test
    func shortcutGridFollowsTheFeatureFlags() {
        withDependencies {
            // Isolate app storage so this neither reads flag pollution from
            // other suites nor writes into UserDefaults.standard.
            $0.defaultAppStorage = UserDefaults(suiteName: "shortcutGridFollowsTheFeatureFlags")!
        } operation: {
            var state = MeFeature.State()

            state.$isEnrollmentEnabled.withLock { $0 = false }
            state.$isCertificateEnabled.withLock { $0 = false }
            state.$isHistoryEnabled.withLock { $0 = false }
            state.$isParadoxoEnabled.withLock { $0 = false }
            state.$isMaterialsEnabled.withLock { $0 = false }
            state.$isRetrospectiveEnabled.withLock { $0 = false }
            // `shortcuts` returns allCases in DEBUG, so assert the gating on
            // `gatedShortcuts`, which always applies the flag filter.
            #expect(state.gatedShortcuts == [.calendar, .countdown])

            state.$isEnrollmentEnabled.withLock { $0 = true }
            state.$isCertificateEnabled.withLock { $0 = true }
            state.$isHistoryEnabled.withLock { $0 = true }
            state.$isParadoxoEnabled.withLock { $0 = true }
            state.$isMaterialsEnabled.withLock { $0 = true }
            state.$isRetrospectiveEnabled.withLock { $0 = true }
            // Retrospectiva also needs an open window, not just the flag.
            #expect(!state.gatedShortcuts.contains(.retrospective))

            state.retrospectiveSemester = "20261"
            #expect(state.gatedShortcuts == MeShortcut.allCases)
        }
    }

    @Test
    func documentShortcutsOpenTheRequestSheet() async {
        let initialState = MeFeature.State(userName: "Mariana Souza")
        initialState.$isCertificateEnabled.withLock { $0 = true }

        let store = TestStore(initialState: initialState) {
            MeFeature()
        } withDependencies: {
            $0.localDocuments.load = { _ in nil }
        }

        await store.send(.shortcutTapped(.certificate)) {
            $0.document = MeDocumentFeature.State(
                document: .enrollmentCertificate,
                studentName: "Mariana Souza"
            )
        }
    }
}
