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
            $0.eventsRepository.upcoming = { _ in throw APIError.emptyEnvelope }
        }

        await store.send(.task) {
            $0.userName = "Mariana Souza"
        }
        await store.receive(.overviewUpdated(cached)) {
            $0.overview = .preview
            $0.syncedAt = Self.referenceDate
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
            $0.eventsRepository.upcoming = { _ in throw APIError.emptyEnvelope }
        }

        await store.send(.task) {
            $0.userName = "Mariana Souza"
        }
        await store.receive(.profileLoaded(.preview)) {
            $0.profile = .preview
        }
    }

    @Test
    func eventsFeedTheCalendarTeaser() async {
        let events: [AcademicEvent] = .preview(now: Self.referenceDate)

        let store = TestStore(initialState: MeFeature.State()) {
            MeFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.sessionStore = .inMemory(initial: .preview)
            $0.meRepository.observe = { .finished }
            $0.profileRepository.current = { throw APIError.emptyEnvelope }
            $0.eventsRepository.upcoming = { _ in events }
        }

        await store.send(.task) {
            $0.userName = "Mariana Souza"
        }
        await store.receive(.eventsLoaded(events)) {
            $0.events = events
        }
    }

    @Test
    func shortcutsOpenAndCloseTheirSheet() async {
        let store = TestStore(initialState: MeFeature.State()) {
            MeFeature()
        }

        await store.send(.shortcutTapped(.calendar)) {
            $0.activeShortcut = .calendar
        }
        await store.send(.shortcutDismissed) {
            $0.activeShortcut = nil
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
        // The other rows still wait for their features.
        await store.send(.settingsRowTapped(.about))
    }

    @Test
    func logoutKeepingDataClearsOnlyTheSession() async {
        let summary = LocalDataSummary(semesters: 7, messages: 142)
        let sessionStore = SessionStore.inMemory(initial: .preview)
        let wiped = LockIsolated(false)

        let store = TestStore(initialState: MeFeature.State(userName: "Mariana Souza")) {
            MeFeature()
        } withDependencies: {
            $0.sessionStore = sessionStore
            $0.meRepository.localData = { summary }
            $0.meRepository.wipeLocalData = { wiped.setValue(true) }
        }

        await store.send(.logoutTapped) {
            $0.isLogoutPromptPresented = true
        }
        await store.send(.logoutConfirmed(keepData: true)) {
            $0.isLogoutPromptPresented = false
        }
        await store.receive(.delegate(.loggedOut(firstName: "Mariana", keptData: true, dataSummary: summary)))

        #expect(sessionStore.current() == nil)
        #expect(wiped.value == false)
    }

    @Test
    func logoutWipingDataResetsTheMirrorAndTheme() async {
        let sessionStore = SessionStore.inMemory(initial: .preview)
        let wiped = LockIsolated(false)

        let initialState = MeFeature.State(userName: "Mariana Souza")
        initialState.$theme.withLock { $0 = .dark }

        let store = TestStore(initialState: initialState) {
            MeFeature()
        } withDependencies: {
            $0.sessionStore = sessionStore
            $0.meRepository.localData = { LocalDataSummary(semesters: 7, messages: 142) }
            $0.meRepository.wipeLocalData = { wiped.setValue(true) }
        }

        await store.send(.logoutConfirmed(keepData: false)) {
            $0.$theme.withLock { $0 = .system }
        }
        await store.receive(.delegate(.loggedOut(firstName: "Mariana", keptData: false, dataSummary: nil)))

        #expect(sessionStore.current() == nil)
        #expect(wiped.value == true)
    }
}
