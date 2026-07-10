import ComposableArchitecture
import Foundation

private let log = Log.scoped("EventsRepository")

extension EventsRepository: DependencyKey {
    static let liveValue = EventsRepository(
        calendar: { now in
            @Dependency(\.apiClient) var apiClient
            // The endpoint defaults to ±1 month; ask for a window wide enough
            // to cover the running academic year on both sides.
            let calendar = Calendar.current
            let since = calendar.date(byAdding: .month, value: -3, to: now) ?? now
            let until = calendar.date(byAdding: .month, value: 9, to: now) ?? now
            log.debug("calendar start since=\(since.dayStamp) until=\(until.dayStamp)")
            do {
                let list = try await apiClient.get(
                    EventListDTO.self,
                    from: "api/me/events",
                    query: [
                        URLQueryItem(name: "since", value: since.dayStamp),
                        URLQueryItem(name: "until", value: until.dayStamp),
                    ]
                )
                let events = list.events
                    .map(\.domain)
                    .sorted { ($0.start, $0.summary) < ($1.start, $1.summary) }
                log.info("calendar ok count=\(events.count)")
                return events
            } catch {
                switch error {
                case APIError.server(401, _):
                    log.warn("calendar unauthorized")
                case let APIError.server(status, message):
                    log.warn("calendar server \(status) message=\(message ?? "<none>")")
                case APIError.emptyEnvelope:
                    log.warn("calendar 2xx envelope had null data")
                case is URLError:
                    log.warn("calendar transport failure", error: error)
                default:
                    log.error("calendar failed", error: error)
                }
                throw error
            }
        }
    )
}
