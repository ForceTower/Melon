import Foundation

/// One closed semester distilled into the Retrospectiva deck.
/// Everything is computed from the local mirror; the turma percentile is
/// merged in afterwards from the one live endpoint and stays nil offline.
struct RetrospectiveDeck: Equatable, Sendable {
    /// Drives the adaptive copy: calouro / semestre difícil / veterano.
    enum Profile: Equatable, Sendable {
        case freshman
        case struggled
        case veteran
    }

    struct Glance: Equatable, Sendable {
        var disciplines: Int
        var classHours: Int
        var weeks: Int
    }

    struct Grades: Equatable, Sendable {
        /// Hours-weighted mean of the semester's published final grades.
        var media: Double
        var bestGrade: Double
        var bestDiscipline: String
    }

    struct Attendance: Equatable, Sendable {
        /// 0–100, from class-hours attended vs total.
        var percent: Int
        var missedHours: Int
    }

    /// The comeback beat: the approved discipline that demanded the most —
    /// a survived Prova Final outranks everything, then the lowest passing
    /// grade.
    struct Victory: Equatable, Sendable {
        var discipline: String
        var grade: Double
        var viaFinal: Bool
    }

    struct ScoreCard: Equatable, Sendable {
        var value: Double
        var previous: Double?
        /// CR after each recent semester, oldest→newest, this one last —
        /// the real sparkline (the design mocks it; we have the truth).
        var series: [Double]

        var delta: Double? { previous.map { value - $0 } }
        var isFirst: Bool { previous == nil }
        var isDown: Bool { (delta ?? 0) < -0.005 }
    }

    struct Turma: Equatable, Sendable {
        var discipline: String
        var percentile: Int
        var cohortSize: Int
    }

    var semesterCode: String
    /// "2026.1".
    var semesterLabel: String
    /// "2026.2" — the closing card's hand-off.
    var nextLabel: String
    var glance: Glance
    var grades: Grades?
    var attendance: Attendance?
    var victory: Victory?
    var score: ScoreCard?
    /// Names of disciplines with `approved == false`.
    var failures: [String]
    /// Best turma comparison — nil until the endpoint answers.
    var turma: Turma?

    var profile: Profile {
        if score?.isFirst ?? true { return .freshman }
        if !failures.isEmpty { return .struggled }
        return .veteran
    }
}

/// One discipline's turma comparison from `GET api/retrospective`.
struct RetrospectivePercentile: Equatable, Sendable {
    var disciplineId: String
    var name: String
    var percentile: Int?
    var cohortSize: Int
}

extension RetrospectiveDeck {
    /// Folds the live percentiles in, keeping the single best rank — the
    /// story tells one turma brag, not a table.
    func merging(_ percentiles: [RetrospectivePercentile]) -> RetrospectiveDeck {
        var merged = self
        let best = percentiles
            .compactMap { row -> Turma? in
                guard let percentile = row.percentile else { return nil }
                return Turma(discipline: row.name, percentile: percentile, cohortSize: row.cohortSize)
            }
            .max { $0.percentile < $1.percentile }
        merged.turma = best ?? turma
        return merged
    }
}
