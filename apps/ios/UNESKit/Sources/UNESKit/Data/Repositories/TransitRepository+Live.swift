import ComposableArchitecture
import Foundation

private let log = Log.scoped("TransitRepository")

extension TransitRepository: DependencyKey {
    static let liveValue: TransitRepository = {
        let cache = TransitNetworkCache()
        let outages = TransitOutages()
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
                    // Once per screen entry; being offline isn't a bug.
                    logFailure("network failed", error: error, connection: .info)
                    if let stale = await cache.any() { return stale }
                    throw error
                }
            },
            arrivals: { stopId, direction, lineNumbers in
                @Dependency(\.apiClient) var apiClient
                let key = "\(stopId)/\(direction.rawValue)"
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
                    await outages.recovered("arrivals", key: key)
                    return board
                } catch {
                    await outages.failed("arrivals", key: key, error: error)
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
                    let follow = try dto.domain()
                    await outages.recovered("follow", key: arrivalId)
                    return follow
                } catch {
                    await outages.failed("follow", key: arrivalId, error: error)
                    throw error
                }
            }
        )
    }()
}

/// Connection trouble — no transport, a non-2xx answer — is logged at
/// `connection`, which the caller picks by how often the read runs. Anything
/// else is a bug in this app or the API and stays an error.
private func logFailure(_ what: String, error: any Error, connection: LogLevel) {
    switch error {
    case is CancellationError:
        break
    case let APIError.server(status, message):
        log.write(connection, "\(what) server \(status) message=\(message ?? "<none>")")
    case is URLError:
        log.write(connection, "\(what) transport failure", error: error)
    default:
        log.error(what, error: error)
    }
}

/// The polled reads log once per outage: the first failure and the recovery
/// at info (one remote record each), the repeats in between at debug, which
/// stays on the device. A screen polling every few seconds offline must not
/// ship a record per tick.
private actor TransitOutages {
    private var failing: Set<String> = []

    func failed(_ read: String, key: String, error: any Error) {
        guard !LogPipeline.isCancellation(error) else { return }
        if failing.insert(key).inserted {
            logFailure("\(read) failing key=\(key)", error: error, connection: .info)
        } else {
            log.debug("\(read) still failing key=\(key)")
        }
    }

    func recovered(_ read: String, key: String) {
        guard failing.remove(key) != nil else { return }
        log.info("\(read) recovered key=\(key)")
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
