import ComposableArchitecture
import Foundation

@DependencyClient
struct EventsRepository: Sendable {
    /// The academic calendar around `now`, earliest first — a window wide
    /// enough to cover the running academic year, past events included.
    var calendar: @Sendable (_ now: Date) async throws -> [AcademicEvent]
}

extension EventsRepository: TestDependencyKey {
    static let testValue = EventsRepository()

    static let previewValue = EventsRepository(
        calendar: { now in .preview(now: now) }
    )
}

extension DependencyValues {
    var eventsRepository: EventsRepository {
        get { self[EventsRepository.self] }
        set { self[EventsRepository.self] = newValue }
    }
}
