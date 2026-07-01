import ComposableArchitecture
import Testing

@testable import UNESKit

@MainActor
struct AppFeatureTests {
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
}
