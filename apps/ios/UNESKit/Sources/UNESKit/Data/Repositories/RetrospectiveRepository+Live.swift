import ComposableArchitecture
import Foundation

private let log = Log.scoped("RetrospectiveRepository")

extension RetrospectiveRepository: DependencyKey {
    static let liveValue = RetrospectiveRepository(
        percentiles: { semesterCode in
            @Dependency(\.apiClient) var apiClient
            do {
                let dto: RetrospectiveDTO = try await apiClient.get(
                    from: "api/retrospective",
                    query: [URLQueryItem(name: "semester", value: semesterCode)]
                )
                return dto.domain
            } catch {
                logFailure(error)
                throw error
            }
        }
    )

    private static func logFailure(_ error: any Error) {
        switch error {
        case let APIError.server(status, message):
            log.warn("percentiles failed status=\(status) message=\(message ?? "-")")
        case APIError.emptyEnvelope:
            log.warn("percentiles failed reason=emptyEnvelope")
        case let error as URLError:
            log.debug("percentiles offline code=\(error.code.rawValue)")
        default:
            log.warn("percentiles failed", error: error)
        }
    }
}

// MARK: - DTOs

private struct RetrospectiveDTO: Decodable {
    struct Discipline: Decodable {
        var disciplineId: String
        var name: String
        var percentile: Int?
        var cohortSize: Int
    }

    var disciplines: [Discipline]

    var domain: [RetrospectivePercentile] {
        disciplines.map {
            RetrospectivePercentile(
                disciplineId: $0.disciplineId,
                name: $0.name,
                percentile: $0.percentile,
                cohortSize: $0.cohortSize
            )
        }
    }
}
