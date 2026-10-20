import ComposableArchitecture
import Foundation

private let log = Log.scoped("TransitRepository")

extension TransitRepository: DependencyKey {
    static let liveValue: TransitRepository = {
        let cache = TransitNetworkCache()
        return TransitRepository(
            network: {
                if let cached = await cache.fresh() { return cached }
                @Dependency(\.apiClient) var apiClient
                do {
                    let dto: TransitNetworkDTO = try await apiClient.get(from: "api/transit/network")
                    let network = try dto.domain()
                    await cache.store(network)
                    log.info("network ok lines=\(network.lines.count) routes=\(network.routes.count)")
                    return network
                } catch {
                    logFailure("network", error: error)
                    if let stale = await cache.any() { return stale }
                    throw error
                }
            },
            arrivals: { stopId, direction, lineNumbers in
                @Dependency(\.apiClient) var apiClient
                do {
                    let dto: TransitStopArrivalsDTO = try await apiClient.get(
                        from: "api/transit/stops/\(stopId)/arrivals",
                        query: [
                            URLQueryItem(name: "direction", value: direction.rawValue),
                            URLQueryItem(name: "lines", value: lineNumbers.joined(separator: ",")),
                        ]
                    )
                    let board = try dto.domain()
                    log.debug("arrivals ok stop=\(stopId) count=\(board.arrivals.count) feed=\(board.feed.status.rawValue)")
                    return board
                } catch {
                    logFailure("arrivals", error: error)
                    throw error
                }
            },
            follow: { lineNumber, direction, stopId, arrivalId, vehicleId in
                @Dependency(\.apiClient) var apiClient
                var query = [
                    URLQueryItem(name: "line", value: lineNumber),
                    URLQueryItem(name: "direction", value: direction.rawValue),
                    URLQueryItem(name: "stopId", value: stopId),
                    URLQueryItem(name: "arrivalId", value: arrivalId),
                ]
                if let vehicleId {
                    query.append(URLQueryItem(name: "vehicleId", value: vehicleId))
                }
                do {
                    let dto: TransitFollowDTO = try await apiClient.get(from: "api/transit/follow", query: query)
                    return try dto.domain()
                } catch {
                    logFailure("follow", error: error)
                    throw error
                }
            }
        )
    }()

    private static func logFailure(_ operation: String, error: Error) {
        switch error {
        case is CancellationError:
            break
        case let APIError.server(status, message):
            log.warn("\(operation) server \(status) message=\(message ?? "<none>")")
        case is URLError:
            log.warn("\(operation) transport failure", error: error)
        default:
            log.error("\(operation) failed", error: error)
        }
    }
}

/// Stops and geometry change when the city edits a route, so a few hours of
/// reuse is safe; a stale copy still beats no map when the request fails.
private actor TransitNetworkCache {
    private static let ttl: TimeInterval = 6 * 3600
    private var value: (network: TransitNetwork, storedAt: Date)?

    func fresh() -> TransitNetwork? {
        guard let value, Date().timeIntervalSince(value.storedAt) < Self.ttl else { return nil }
        return value.network
    }

    func any() -> TransitNetwork? {
        value?.network
    }

    func store(_ network: TransitNetwork) {
        value = (network, Date())
    }
}
