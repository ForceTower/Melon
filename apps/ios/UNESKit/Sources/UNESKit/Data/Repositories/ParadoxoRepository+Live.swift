import ComposableArchitecture
import Foundation

private let log = Log.scoped("ParadoxoRepository")

extension ParadoxoRepository: DependencyKey {
    static let liveValue = ParadoxoRepository(
        overview: {
            @Dependency(\.apiClient) var apiClient
            log.debug("overview start")
            do {
                let dto: ParadoxoOverviewDTO = try await apiClient.get(from: "api/paradoxo/overview")
                let overview = dto.domain
                log.info("overview ok pulse=\(overview.pulse.count) mine=\(overview.myDisciplines.count)")
                return overview
            } catch {
                logFailure("overview", error: error)
                throw error
            }
        },
        index: {
            @Dependency(\.apiClient) var apiClient
            log.debug("index start")
            do {
                let dto: ParadoxoIndexDTO = try await apiClient.get(from: "api/paradoxo/index")
                let entries = dto.entries.compactMap(\.domain)
                log.info("index ok entries=\(entries.count)")
                return entries
            } catch {
                logFailure("index", error: error)
                throw error
            }
        },
        discipline: { id in
            @Dependency(\.apiClient) var apiClient
            log.debug("discipline start id=\(id)")
            do {
                let dto: ParadoxoDisciplineDTO = try await apiClient.get(from: "api/paradoxo/disciplines/\(id)")
                return dto.domain
            } catch {
                logFailure("discipline", error: error)
                throw error
            }
        },
        teacher: { id in
            @Dependency(\.apiClient) var apiClient
            log.debug("teacher start id=\(id)")
            do {
                let dto: ParadoxoTeacherDTO = try await apiClient.get(from: "api/paradoxo/teachers/\(id)")
                return dto.domain
            } catch {
                logFailure("teacher", error: error)
                throw error
            }
        }
    )

    private static func logFailure(_ operation: String, error: Error) {
        switch error {
        case APIError.server(401, _):
            log.warn("\(operation) unauthorized")
        case let APIError.server(status, message):
            log.warn("\(operation) server \(status) message=\(message ?? "<none>")")
        case APIError.emptyEnvelope:
            log.warn("\(operation) 2xx envelope had null data")
        case is URLError:
            log.warn("\(operation) transport failure", error: error)
        default:
            log.error("\(operation) failed", error: error)
        }
    }
}

// MARK: - DTOs (`api/paradoxo/*`)

private struct ParadoxoRefDTO: Decodable {
    var kind: String
    var id: String

    var domain: ParadoxoEntityRef? {
        ParadoxoEntityKind(rawValue: kind).map { ParadoxoEntityRef(kind: $0, id: id) }
    }
}

private struct ParadoxoOverviewDTO: Decodable {
    struct PulseFact: Decodable {
        var id: String
        var kind: String
        var metric: Double
        var title: String
        var subtitle: String
        var ref: ParadoxoRefDTO

        /// Unknown kinds are newer server insights — dropped, not failed.
        var domain: ParadoxoPulseFact? {
            guard let kind = ParadoxoPulseFact.Kind(rawValue: kind), let ref = ref.domain else { return nil }
            return ParadoxoPulseFact(id: id, kind: kind, metric: metric, title: title, subtitle: subtitle, ref: ref)
        }
    }

    struct Mine: Decodable {
        var id: String
        var code: String
        var name: String
        var mean: Double
        var sampleCount: Int
        var spark: [Double]? = nil
        var myPercentile: Int? = nil

        var domain: ParadoxoDisciplineSummary {
            ParadoxoDisciplineSummary(
                id: id, code: code, name: name, mean: mean,
                sampleCount: sampleCount, spark: spark ?? [], myPercentile: myPercentile
            )
        }
    }

    struct Ranking: Decodable {
        var kind: String
        var entries: [Entry]

        struct Entry: Decodable {
            var ref: ParadoxoRefDTO
            var name: String
            var code: String? = nil
            var mean: Double
            var studentCount: Int
            var delta: Double? = nil

            var domain: ParadoxoRankedEntry? {
                guard let ref = ref.domain else { return nil }
                return ParadoxoRankedEntry(
                    ref: ref, name: name, code: code, mean: mean,
                    studentCount: studentCount, delta: delta
                )
            }
        }

        var domain: ParadoxoRanking? {
            guard let kind = ParadoxoExploreKind(rawValue: kind) else { return nil }
            return ParadoxoRanking(kind: kind, entries: entries.compactMap(\.domain))
        }
    }

    var pulse: [PulseFact]
    var myDisciplines: [Mine]? = nil
    var rankings: [Ranking]? = nil
    var studentCount: Int? = nil
    var meanCount: Int? = nil

    var domain: ParadoxoOverview {
        ParadoxoOverview(
            pulse: pulse.compactMap(\.domain),
            myDisciplines: (myDisciplines ?? []).map(\.domain),
            rankings: (rankings ?? []).compactMap(\.domain),
            studentCount: studentCount ?? 0,
            meanCount: meanCount ?? 0
        )
    }
}

private struct ParadoxoIndexDTO: Decodable {
    struct Entry: Decodable {
        var ref: ParadoxoRefDTO
        var name: String
        var code: String? = nil
        var mean: Double
        var studentCount: Int

        var domain: ParadoxoIndexEntry? {
            guard let ref = ref.domain else { return nil }
            let searchable = [code, name].compactMap(\.self).joined(separator: " ")
            return ParadoxoIndexEntry(
                ref: ref, name: name, code: code, mean: mean, studentCount: studentCount,
                searchKey: ParadoxoIndexEntry.fold(searchable)
            )
        }
    }

    var entries: [Entry]
}

private struct ParadoxoSemesterMeanDTO: Decodable {
    var semester: String
    var mean: Double

    var domain: ParadoxoSemesterMean { ParadoxoSemesterMean(semester: semester, mean: mean) }
}

private struct ParadoxoDisciplineDTO: Decodable {
    struct Teacher: Decodable {
        var id: String
        var name: String
        var mean: Double
        var sampleCount: Int
        var lastSemester: String? = nil
        var history: [ParadoxoSemesterMeanDTO]? = nil

        var domain: ParadoxoDisciplineTeacher {
            ParadoxoDisciplineTeacher(
                id: id, name: name, mean: mean, sampleCount: sampleCount,
                lastSemester: lastSemester, history: (history ?? []).map(\.domain)
            )
        }
    }

    var id: String
    var code: String
    var name: String
    var department: String? = nil
    var mean: Double
    var studentCount: Int
    var approved: Int
    var failed: Int
    var quit: Int
    var history: [ParadoxoSemesterMeanDTO]
    var distribution: [Double]? = nil
    var myGrade: Double? = nil
    var teachers: [Teacher]? = nil

    var domain: ParadoxoDisciplineDetails {
        ParadoxoDisciplineDetails(
            id: id, code: code, name: name, department: department,
            mean: mean, studentCount: studentCount,
            approved: approved, failed: failed, quit: quit,
            history: history.map(\.domain),
            distribution: distribution ?? [],
            myGrade: myGrade,
            teachers: (teachers ?? []).map(\.domain)
        )
    }
}

private struct ParadoxoTeacherDTO: Decodable {
    struct Discipline: Decodable {
        var id: String
        var code: String
        var name: String
        var mean: Double
        var sampleCount: Int

        var domain: ParadoxoTeacherDiscipline {
            ParadoxoTeacherDiscipline(id: id, code: code, name: name, mean: mean, sampleCount: sampleCount)
        }
    }

    var id: String
    var name: String
    var mean: Double
    var studentCount: Int
    var approved: Int
    var failed: Int
    var quit: Int
    var lastSemester: String? = nil
    var history: [ParadoxoSemesterMeanDTO]? = nil
    var distribution: [Double]? = nil
    var disciplines: [Discipline]? = nil

    var domain: ParadoxoTeacherDetails {
        ParadoxoTeacherDetails(
            id: id, name: name, mean: mean, studentCount: studentCount,
            approved: approved, failed: failed, quit: quit,
            lastSemester: lastSemester,
            history: (history ?? []).map(\.domain),
            distribution: distribution ?? [],
            disciplines: (disciplines ?? []).map(\.domain)
        )
    }
}
