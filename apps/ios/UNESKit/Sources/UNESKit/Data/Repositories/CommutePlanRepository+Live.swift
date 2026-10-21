import ComposableArchitecture
import Foundation

private let log = Log.scoped("CommutePlanRepository")

extension CommutePlanRepository: DependencyKey {
    static let liveValue: CommutePlanRepository = {
        let operations = PlanOperations()
        return CommutePlanRepository(
            observe: {
                @Dependency(\.database) var wrappedDatabase
                let mirror = MirrorStore(writer: wrappedDatabase)
                return AsyncStream { continuation in
                    let task = Task {
                        do {
                            for try await plan in mirror.commutePlanUpdates() {
                                continuation.yield(plan)
                            }
                        } catch {
                            log.error("observe failed", error: error)
                        }
                        continuation.finish()
                    }
                    continuation.onTermination = { _ in task.cancel() }
                }
            },
            refresh: {
                @Dependency(\.apiClient) var apiClient
                @Dependency(\.database) var wrappedDatabase
                let client = apiClient
                let mirror = MirrorStore(writer: wrappedDatabase)
                do {
                    try await operations.refresh {
                        let dto: TransitCommutePlanDTO = try await client.get(from: "api/transit/plan")
                        // A save or removal arrived meanwhile: this answer is already old.
                        try Task.checkCancellation()
                        try await mirror.applyCommutePlan(dto.domain)
                        log.debug("refresh ok directions=\(dto.domain.directions.count)")
                    }
                } catch {
                    // Once per screen entry, and the mirror stays; not a bug.
                    logFailure("refresh failed", error: error, connection: .info)
                    throw error
                }
            },
            save: { plan in
                @Dependency(\.apiClient) var apiClient
                @Dependency(\.database) var wrappedDatabase
                let client = apiClient
                let mirror = MirrorStore(writer: wrappedDatabase)
                do {
                    return try await operations.mutate {
                        let dto: TransitCommutePlanDTO = try await client.put(
                            at: "api/transit/plan",
                            body: TransitCommutePlanInputDTO(plan)
                        )
                        let saved = dto.domain
                        try await mirror.applyCommutePlan(saved)
                        log.info("save ok directions=\(saved.directions.count)")
                        return saved
                    }
                } catch {
                    logFailure("save failed", error: error, connection: .warn)
                    throw error
                }
            },
            remove: {
                @Dependency(\.apiClient) var apiClient
                @Dependency(\.database) var wrappedDatabase
                let client = apiClient
                let mirror = MirrorStore(writer: wrappedDatabase)
                do {
                    try await operations.mutate {
                        let dto = try await client.delete(TransitCommutePlanDTO.self, "api/transit/plan")
                        try await mirror.applyCommutePlan(dto.domain)
                        log.info("remove ok")
                    }
                } catch {
                    logFailure("remove failed", error: error, connection: .warn)
                    throw error
                }
            }
        )
    }()
}

/// Connection trouble — no transport, a non-2xx answer — is logged at
/// `connection`: a refresh runs on every screen entry, a save or removal
/// once per tap with an alert the student sees. Anything else is a bug in
/// this app or the API and stays an error.
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

/// Runs plan requests one at a time, in the order they were made, each with
/// its mirror write, so an older answer can never overwrite a newer one.
/// Refreshes give way: a save or removal cancels the ones still pending
/// instead of queueing behind them, and a refresh stops with its caller.
/// Saves and removals always run to the end, so the mirror ends up holding
/// what the server answered even if the screen that asked has gone.
private actor PlanOperations {
    private var tail: Task<Void, Never>?
    private var refreshes: Set<Task<Void, any Error>> = []

    func refresh(_ operation: @escaping @Sendable () async throws -> Void) async throws {
        let task = enqueue(operation)
        refreshes.insert(task)
        defer { refreshes.remove(task) }
        try await withTaskCancellationHandler {
            try await task.value
        } onCancel: {
            task.cancel()
        }
    }

    func mutate<T: Sendable>(_ operation: @escaping @Sendable () async throws -> T) async throws -> T {
        for refresh in refreshes { refresh.cancel() }
        refreshes.removeAll()
        return try await enqueue(operation).value
    }

    private func enqueue<T: Sendable>(_ operation: @escaping @Sendable () async throws -> T) -> Task<T, any Error> {
        let previous = tail
        let task = Task {
            await previous?.value
            try Task.checkCancellation()
            return try await operation()
        }
        tail = Task { _ = try? await task.value }
        return task
    }
}
