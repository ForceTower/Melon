import ComposableArchitecture
import Foundation

@DependencyClient
struct EventsRepository: Sendable {
    /// Ongoing and future academic events, soonest first. The backend serves
    /// a window of roughly one month around today.
    var upcoming: @Sendable (_ now: Date) async throws -> [AcademicEvent]
}

extension EventsRepository: TestDependencyKey {
    static let testValue = EventsRepository()

    static let previewValue = EventsRepository(
        upcoming: { now in .preview(now: now) }
    )
}

extension DependencyValues {
    var eventsRepository: EventsRepository {
        get { self[EventsRepository.self] }
        set { self[EventsRepository.self] = newValue }
    }
}
