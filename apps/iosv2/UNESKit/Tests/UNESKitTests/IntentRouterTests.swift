import Testing

@testable import UNESKit

/// Buffer-replay semantics of the intent route hub: routes posted with a
/// subscriber flow through; routes posted without one wait in a
/// single-element buffer (newest wins) for the first subscriber, and a
/// delivered route is never replayed.
struct IntentRouterTests {
    @Test
    func deliversToTheActiveSubscriber() async {
        let hub = IntentRouteHub()
        var routes = hub.stream().makeAsyncIterator()

        hub.send(.tab(.messages))

        #expect(await routes.next() == .tab(.messages))
    }

    @Test
    func replaysTheBufferedRouteToTheFirstSubscriber() async {
        let hub = IntentRouteHub()
        hub.send(.tab(.schedule))

        var routes = hub.stream().makeAsyncIterator()

        #expect(await routes.next() == .tab(.schedule))
        // Replayed exactly once — the next value is live, not the buffer.
        hub.send(.tab(.me))
        #expect(await routes.next() == .tab(.me))
    }

    @Test
    func newestUnconsumedRouteWins() async {
        let hub = IntentRouteHub()
        hub.send(.tab(.home))
        hub.send(.tab(.classes))

        var routes = hub.stream().makeAsyncIterator()

        #expect(await routes.next() == .tab(.classes))
        // .home was overwritten — the stream moves straight to live routes.
        hub.send(.tab(.me))
        #expect(await routes.next() == .tab(.me))
    }

    @Test
    func deliveredRouteIsNotReplayedToAResubscription() async {
        let hub = IntentRouteHub()
        hub.send(.tab(.messages))
        var first = hub.stream().makeAsyncIterator()
        #expect(await first.next() == .tab(.messages))

        // Nothing buffered ahead of the resubscription's first live route.
        var second = hub.stream().makeAsyncIterator()
        hub.send(.tab(.me))
        #expect(await second.next() == .tab(.me))
    }

    @Test
    func postWhileSubscribedLeavesNothingPending() async {
        let hub = IntentRouteHub()
        var first = hub.stream().makeAsyncIterator()
        hub.send(.tab(.schedule))
        #expect(await first.next() == .tab(.schedule))

        var second = hub.stream().makeAsyncIterator()
        hub.send(.tab(.home))
        #expect(await second.next() == .tab(.home))
    }
}
