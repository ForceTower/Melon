import Foundation
import Testing

@testable import UNESKit

struct WatchStoreTests {
    static let referenceDate = Date(timeIntervalSince1970: 1_776_000_000)

    @Test
    func pushedPayloadRoundTripsThroughTheStore() async throws {
        let pushed = WatchSnapshot.preview(now: Self.referenceDate)
        // The wire hop the payload takes from the phone to the watch.
        let decoded = try JSONDecoder().decode(
            WatchSnapshot.self,
            from: JSONEncoder().encode(pushed)
        )

        let store = try WatchStore.inMemory()
        try await store.apply(decoded)
        let stored = try #require(try store.current())

        #expect(stored.coefficient == pushed.coefficient)
        #expect(stored.coefficientDelta == pushed.coefficientDelta)
        #expect(stored.attendancePercent == pushed.attendancePercent)
        #expect(stored.remainingAbsences == pushed.remainingAbsences)
        #expect(stored.nextExam == pushed.nextExam)
        #expect(abs(stored.syncedAt.timeIntervalSince(pushed.syncedAt)) < 0.001)

        // Disciplines come back name-sorted with grade order preserved.
        #expect(Set(stored.disciplines.map(\.id)) == Set(pushed.disciplines.map(\.id)))
        for discipline in pushed.disciplines {
            #expect(stored.disciplines.first { $0.id == discipline.id } == discipline)
        }

        #expect(stored.schedule.semesterCode == pushed.schedule.semesterCode)
        #expect(
            stored.schedule.sessions.sorted { $0.classId < $1.classId }
                == pushed.schedule.sessions.sorted { $0.classId < $1.classId }
        )
        #expect(
            stored.schedule.topics.sorted { $0.classId < $1.classId }
                == pushed.schedule.topics.sorted { $0.classId < $1.classId }
        )
    }

    @Test
    func messagesKeepPushOrderAndContent() async throws {
        let pushed = WatchSnapshot.preview(now: Self.referenceDate)
        let decoded = try JSONDecoder().decode(
            WatchSnapshot.self,
            from: JSONEncoder().encode(pushed)
        )

        let store = try WatchStore.inMemory()
        try await store.apply(decoded)
        let stored = try #require(try store.current())

        #expect(stored.messages.map(\.id) == pushed.messages.map(\.id))
        for (storedMessage, pushedMessage) in zip(stored.messages, pushed.messages) {
            #expect(storedMessage.origin == pushedMessage.origin)
            #expect(storedMessage.subject == pushedMessage.subject)
            #expect(storedMessage.body == pushedMessage.body)
            #expect(storedMessage.senderName == pushedMessage.senderName)
            #expect(storedMessage.unread == pushedMessage.unread)
            #expect(storedMessage.attachments == pushedMessage.attachments)
            #expect(abs(storedMessage.receivedAt.timeIntervalSince(pushedMessage.receivedAt)) < 0.001)
        }
    }

    @Test
    func localReadOverlaySurvivesPushesUntilTheMessageLeaves() async throws {
        var snapshot = WatchSnapshot.preview(now: Self.referenceDate)
        let readId = try #require(snapshot.messages.first { $0.unread }?.id)

        let store = try WatchStore.inMemory()
        try await store.apply(snapshot)
        try await store.markMessageRead(id: readId, now: Self.referenceDate)
        #expect(try store.current()?.messages.first { $0.id == readId }?.unread == false)

        // The same message pushed unread again stays read locally.
        try await store.apply(snapshot)
        #expect(try store.current()?.messages.first { $0.id == readId }?.unread == false)

        // Once it leaves the payload the overlay row is pruned, so a later
        // reappearance honors whatever the phone says.
        let remaining = snapshot.messages.filter { $0.id != readId }
        snapshot.messages = remaining
        try await store.apply(snapshot)
        snapshot.messages = [WatchSnapshot.preview(now: Self.referenceDate).messages.first { $0.id == readId }!]
        try await store.apply(snapshot)
        #expect(try store.current()?.messages.first { $0.id == readId }?.unread == true)
    }

    @Test
    func emptyPushWipesTheStore() async throws {
        let store = try WatchStore.inMemory()
        try await store.apply(.preview(now: Self.referenceDate))
        try await store.apply(nil)
        #expect(try store.current() == nil)
    }
}
