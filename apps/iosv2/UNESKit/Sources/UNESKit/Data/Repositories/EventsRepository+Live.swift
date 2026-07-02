import ComposableArchitecture
import Foundation

extension EventsRepository: DependencyKey {
    static let liveValue = EventsRepository(
        calendar: { now in
            @Dependency(\.apiClient) var apiClient
            // The endpoint defaults to ±1 month; ask for a window wide enough
            // to cover the running academic year on both sides.
            let calendar = Calendar.current
            let since = calendar.date(byAdding: .month, value: -3, to: now) ?? now
            let until = calendar.date(byAdding: .month, value: 9, to: now) ?? now
            let list = try await apiClient.get(
                EventListDTO.self,
                from: "api/me/events",
                query: [
                    URLQueryItem(name: "since", value: since.dayStamp),
                    URLQueryItem(name: "until", value: until.dayStamp),
                ]
            )
            return list.events
                .map(\.domain)
                .sorted { ($0.start, $0.summary) < ($1.start, $1.summary) }
        }
    )
}
