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
    func overviewShortcutSwitchesTab() async {
        let store = TestStore(initialState: AppFeature.State()) {
            AppFeature()
        }

        await store.send(.overview(.openScheduleTapped))
        await store.receive(.overview(.delegate(.openSchedule))) {
            $0.tab = .schedule
        }
    }
}
