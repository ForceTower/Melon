import ComposableArchitecture
import Foundation

private let log = Log.scoped("CommutePlanRepository")

extension CommutePlanRepository: DependencyKey {
    static let liveValue = CommutePlanRepository(
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
            do {
                let dto: TransitCommutePlanDTO = try await apiClient.get(from: "api/transit/plan")
                try await MirrorStore(writer: wrappedDatabase).applyCommutePlan(dto.domain)
                log.debug("refresh ok directions=\(dto.domain.directions.count)")
            } catch {
                logFailure("refresh", error: error)
                throw error
            }
        },
        save: { plan in
            @Dependency(\.apiClient) var apiClient
            @Dependency(\.database) var wrappedDatabase
            do {
                let dto: TransitCommutePlanDTO = try await apiClient.put(
                    at: "api/transit/plan",
                    body: TransitCommutePlanInputDTO(plan)
                )
                let saved = dto.domain
                try await MirrorStore(writer: wrappedDatabase).applyCommutePlan(saved)
                log.info("save ok directions=\(saved.directions.count)")
                return saved
            } catch {
                logFailure("save", error: error)
                throw error
            }
        },
        remove: {
            @Dependency(\.apiClient) var apiClient
            @Dependency(\.database) var wrappedDatabase
            do {
                let dto = try await apiClient.delete(TransitCommutePlanDTO.self, "api/transit/plan")
                try await MirrorStore(writer: wrappedDatabase).applyCommutePlan(dto.domain)
                log.info("remove ok")
            } catch {
                logFailure("remove", error: error)
                throw error
            }
        }
    )

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
