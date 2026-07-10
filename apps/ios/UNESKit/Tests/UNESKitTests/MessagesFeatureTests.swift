import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

@MainActor
struct MessagesFeatureTests {
    static nonisolated let referenceDate = Date(timeIntervalSince1970: 1_776_000_000)

    private nonisolated static func overview() -> MessagesOverview {
        .preview(now: referenceDate)
    }

    @Test
    func taskHydratesFromTheMirrorBeforeRefreshing() async {
        let stale = Self.overview()
        var fresh = Self.overview()
        fresh.syncedAt = Self.referenceDate
        let (updates, mirror) = AsyncStream.makeStream(of: MessagesOverview.self)
        // The observation replays the stale mirror before the refresh lands.
        mirror.yield(stale)

        let store = TestStore(initialState: MessagesFeature.State()) {
            MessagesFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.messagesRepository.observe = { updates }
            $0.messagesRepository.refresh = { [fresh] _ in
                mirror.yield(fresh)
                mirror.finish()
            }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.overviewUpdated(stale)) {
            $0.isLoading = false
            $0.overview = stale
        }
        await store.receive(.delegate(.unreadChanged(3)))
        await store.receive(.overviewUpdated(fresh)) {
            $0.overview = fresh
        }
        await store.receive(.delegate(.unreadChanged(3)))
    }

    @Test
    func failureSurfacesOnlyWithoutData() async {
        let store = TestStore(initialState: MessagesFeature.State()) {
            MessagesFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.messagesRepository.observe = { .finished }
            $0.messagesRepository.cached = { _ in nil }
            $0.messagesRepository.refresh = { _ in throw APIError.emptyEnvelope }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.refreshFailed(APIError.emptyEnvelope.localizedDescription)) {
            $0.isLoading = false
            $0.errorMessage = APIError.emptyEnvelope.localizedDescription
        }
    }

    @Test
    func offlineTaskKeepsShowingTheMirrorData() async {
        let cached = Self.overview()
        let (updates, mirror) = AsyncStream.makeStream(of: MessagesOverview.self)
        mirror.yield(cached)
        mirror.finish()

        let store = TestStore(initialState: MessagesFeature.State()) {
            MessagesFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.messagesRepository.observe = { updates }
            $0.messagesRepository.cached = { _ in cached }
            $0.messagesRepository.refresh = { _ in throw APIError.emptyEnvelope }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.overviewUpdated(cached)) {
            $0.isLoading = false
            $0.overview = cached
        }
        await store.receive(.delegate(.unreadChanged(3)))
        // Stale beats an error screen: the failed refresh falls back to the
        // mirror instead of surfacing.
        await store.receive(.overviewUpdated(cached))
        await store.receive(.delegate(.unreadChanged(3)))
    }

    @Test
    func markAllReadFlipsLocallyAndPersists() async {
        let persisted = LockIsolated(false)
        var initial = MessagesFeature.State()
        initial.overview = Self.overview()

        let store = TestStore(initialState: initial) {
            MessagesFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.messagesRepository.markAllRead = { _ in persisted.setValue(true) }
        }

        await store.send(.markAllReadTapped) {
            for index in $0.overview!.messages.indices {
                $0.overview!.messages[index].unread = false
            }
        }
        await store.receive(.delegate(.unreadChanged(0)))
        #expect(persisted.value)
    }

    @Test
    func openingAMessageMarksItReadAndPushesTheDetail() async {
        let readIds = LockIsolated<[String]>([])
        var initial = MessagesFeature.State()
        initial.overview = Self.overview()
        let unreadMessage = Self.overview().messages[0]

        let store = TestStore(initialState: initial) {
            MessagesFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.messagesRepository.markRead = { id, _ in readIds.withValue { $0.append(id) } }
        }

        var opened = unreadMessage
        opened.unread = false
        await store.send(.messageTapped(unreadMessage)) {
            $0.overview?.messages[0].unread = false
            $0.path[id: 0] = .detail(MessageDetailFeature.State(message: opened))
        }
        await store.receive(.delegate(.unreadChanged(2)))
        #expect(readIds.value == [unreadMessage.id])

        // Re-opening a read message pushes without another write.
        await store.send(.messageTapped(opened)) {
            $0.path[id: 1] = .detail(MessageDetailFeature.State(message: opened))
        }
        #expect(readIds.value == [unreadMessage.id])
    }

    @Test
    func starTogglePersistsAndSyncsBackToTheList() async {
        let starCalls = LockIsolated<[String]>([])
        var initial = MessagesFeature.State()
        initial.overview = Self.overview()
        let message = Self.overview().messages[0]
        initial.path.append(.detail(MessageDetailFeature.State(message: message)))

        let store = TestStore(initialState: initial) {
            MessagesFeature()
        } withDependencies: {
            $0.messagesRepository.setStarred = { id, starred in
                starCalls.withValue { $0.append("\(id):\(starred)") }
            }
        }

        var starred = message
        starred.starred = true
        await store.send(.path(.element(id: 0, action: .detail(.starTapped)))) {
            $0.path[id: 0] = .detail(MessageDetailFeature.State(message: starred))
        }
        await store.receive(
            .path(.element(id: 0, action: .detail(.delegate(.starredChanged(id: message.id, starred: true)))))
        ) {
            $0.overview?.messages[0].starred = true
        }
        #expect(starCalls.value == ["\(message.id):true"])
    }
}
