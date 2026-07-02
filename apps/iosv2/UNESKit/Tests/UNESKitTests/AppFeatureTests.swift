import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

@MainActor
struct AppFeatureTests {
    static nonisolated let referenceDate = Date(timeIntervalSince1970: 1_776_000_000)
    @Test
    func selectingATabUpdatesState() async {
        let store = TestStore(initialState: AppFeature.State()) {
            AppFeature()
        }

        await store.send(.tabChanged(.messages)) {
            $0.tab = .messages
        }
    }

    @Test
    func homeShortcutSwitchesTab() async {
        let store = TestStore(initialState: AppFeature.State()) {
            AppFeature()
        }

        await store.send(.home(.seeScheduleTapped))
        await store.receive(.home(.delegate(.openSchedule))) {
            $0.tab = .schedule
        }
    }

    @Test
    func unreadCountFeedsTheMessagesBadge() async {
        let store = TestStore(initialState: AppFeature.State()) {
            AppFeature()
        }

        await store.send(.home(.delegate(.unreadMessagesChanged(3)))) {
            $0.unreadMessages = 3
        }
    }

    @Test
    func resumingFromBackgroundRehydratesEveryTab() async {
        // A week apart, so the two overviews carry disjoint dayStamps — the
        // exact shape of a suspension that crossed into a new week.
        let staleWeek = ScheduleOverview.preview(now: Self.referenceDate)
        let freshWeek = ScheduleOverview.preview(now: Self.referenceDate.addingTimeInterval(7 * 86_400))

        // Every tab already hydrated, so re-sent .task actions only restart
        // the mirror observations.
        var initial = AppFeature.State()
        initial.home.overview = .empty
        initial.schedule.overview = staleWeek
        initial.disciplines.overview = DisciplinesOverview()
        initial.messages.overview = .preview(now: Self.referenceDate)
        initial.me.overview = .empty
        initial.me.userName = "Ana"

        let store = TestStore(initialState: initial) {
            AppFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.homeRepository.observe = { .finished }
            $0.scheduleRepository.observe = {
                AsyncStream { continuation in
                    continuation.yield(freshWeek)
                    continuation.finish()
                }
            }
            $0.disciplinesRepository.observe = { .finished }
            $0.messagesRepository.observe = { .finished }
            $0.meRepository.observe = { .finished }
        }
        // Each tab's own hydration flow is covered by its feature tests; here
        // only the resume broadcast and the schedule replay matter.
        store.exhaustivity = .off

        await store.send(.sceneBackgrounded) {
            $0.wasBackgrounded = true
        }
        await store.send(.sceneActivated) {
            $0.wasBackgrounded = false
        }
        await store.receive(.home(.task))
        await store.receive(.schedule(.task))
        await store.receive(.disciplines(.task))
        await store.receive(.messages(.task))
        await store.receive(.me(.task))
        // The restarted observation replays the mirror recomputed with the
        // current date — the stale week gives way to the fresh one.
        await store.receive(.schedule(.overviewUpdated(freshWeek))) {
            $0.schedule.overview = freshWeek
        }
    }

    @Test
    func transientDeactivationDoesNotRehydrate() async {
        let store = TestStore(initialState: AppFeature.State()) {
            AppFeature()
        }

        // Control center, call banners, and app-switcher peeks bounce through
        // .inactive without reaching .background — no rehydration should run.
        await store.send(.sceneActivated)
    }
}
