import ComposableArchitecture
import Foundation

extension EventsRepository: DependencyKey {
    static let liveValue = EventsRepository(
        upcoming: { now in
            @Dependency(\.apiClient) var apiClient
            let list = try await apiClient.get(EventListDTO.self, from: "api/me/events")
            let today = now.dayStamp
            // The backend's default window reaches back a month — keep only
            // what is still ahead or running (multi-day events).
            return list.events
                .map(\.domain)
                .filter { ($0.end ?? $0.start) >= today }
                .sorted { ($0.start, $0.summary) < ($1.start, $1.summary) }
        }
    )
}
