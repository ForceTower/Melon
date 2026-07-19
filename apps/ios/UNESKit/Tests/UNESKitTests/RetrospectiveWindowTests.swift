import Foundation
import Testing

@testable import UNESKit

/// The auto-detected Retrospectiva window: opens when a semester ends with
/// enough results decided, closes 14 days later — no per-semester flag.
struct RetrospectiveWindowTests {
    let calendar = Calendar.current

    private func date(month: Int, day: Int) -> Date {
        calendar.date(from: DateComponents(year: 2026, month: month, day: day, hour: 10))!
    }

    private func store(endDate: String, approved: Bool?) async throws -> MirrorStore {
        let store = MirrorStore(writer: try inMemoryDatabase())
        var snapshot = MirrorFixtures.payload().snapshot
        snapshot.semester.startDate = "2026-02-18"
        snapshot.semester.endDate = endDate
        snapshot.studentClasses[0].finalGrade = approved == nil ? nil : "7.7"
        snapshot.studentClasses[0].approved = approved
        try await store.apply(semesters: [snapshot.semester], snapshot: snapshot, syncedAt: .now)
        return store
    }

    @Test
    func opensOnceTheSemesterEndsDecided() async throws {
        let store = try await store(endDate: "2026-07-08", approved: true)
        #expect(try await store.retrospectiveWindowCode(now: date(month: 7, day: 19)) == "20261")
    }

    @Test
    func closesFourteenDaysAfterTheEnd() async throws {
        let store = try await store(endDate: "2026-07-08", approved: true)
        #expect(try await store.retrospectiveWindowCode(now: date(month: 7, day: 22)) == "20261")
        #expect(try await store.retrospectiveWindowCode(now: date(month: 7, day: 23)) == nil)
    }

    @Test
    func staysShutWhileTheSemesterRuns() async throws {
        let store = try await store(endDate: "2026-12-10", approved: true)
        #expect(try await store.retrospectiveWindowCode(now: date(month: 7, day: 19)) == nil)
    }

    @Test
    func staysShutWhileResultsArePending() async throws {
        let store = try await store(endDate: "2026-07-08", approved: nil)
        #expect(try await store.retrospectiveWindowCode(now: date(month: 7, day: 19)) == nil)
    }
}
