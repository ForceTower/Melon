import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

@MainActor
struct RetrospectiveFeatureTests {
    @Test
    func theClosingCardParksWithoutThePausedPill() async {
        let clock = TestClock()
        let store = makeStore(clock: clock)

        await store.send(.deckLoaded(Self.deck))
        await clock.advance(by: .seconds(RetrospectiveFeature.cardDuration))
        await store.receive(\.autoAdvanced)
        await clock.advance(by: .seconds(RetrospectiveFeature.cardDuration))
        await store.receive(\.autoAdvanced)

        #expect(store.state.currentCard == .encerramento)
        #expect(store.state.playback == .parked)
        #expect(!store.state.showsPausedPill)
    }

    @Test
    func aHoldShowsThePausedPill() async {
        let store = makeStore(clock: TestClock())

        await store.send(.deckLoaded(Self.deck))
        await store.send(.pressBegan)

        #expect(store.state.playback == .paused)
        #expect(store.state.showsPausedPill)
    }

    private static let deck = RetrospectiveDeck(
        semesterCode: "20261",
        semesterLabel: "2026.1",
        nextLabel: "2026.2",
        glance: .init(disciplines: 5, classHours: 340, weeks: 18),
        failures: []
    )

    private func makeStore(clock: TestClock<Duration>) -> TestStoreOf<RetrospectiveFeature> {
        var state = RetrospectiveFeature.State(semesterCode: "20261")
        state.isShowingAnnounce = false
        let store = TestStore(initialState: state) {
            RetrospectiveFeature()
        } withDependencies: {
            $0.continuousClock = clock
            $0.date = .constant(Date(timeIntervalSince1970: 0))
        }
        store.exhaustivity = .off
        return store
    }
}
