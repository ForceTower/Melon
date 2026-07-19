import Foundation

// MARK: - Mirror records → cross-semester CoefficientSummary

/// The coefficient (CR) across every mirrored semester: the mean of all
/// disciplines taken, each weighted by its class-hours, walked
/// chronologically so the sparkline plots the CR as it stood after each
/// semester.
struct CoefficientHistory {
    var semesters: [SemesterRecord]
    var disciplines: [DisciplineRecord]
    var disciplineOffers: [DisciplineOfferRecord]
    var classes: [ClassRecord]
    var studentClasses: [StudentClassRecord]

    /// Nil until some discipline has a closed result — callers keep the
    /// active snapshot's partial mean as the stand-in until then.
    func summary() -> CoefficientSummary? {
        let spark = checkpoints().map(\.value)
        guard let value = spark.last else { return nil }
        return CoefficientSummary(
            value: value,
            spark: spark,
            delta: spark.count >= 2 ? value - spark[spark.count - 2] : nil
        )
    }

    /// The CR as it stood after each semester that closed anything, in
    /// chronological order — the sparkline with its semesters attached, so
    /// the Retrospectiva can read the before/after around one semester.
    func checkpoints() -> [CoefficientCheckpoint] {
        let ordered = semesters.sorted { ($0.startDate, $0.code) < ($1.startDate, $1.code) }
        let takenBySemester = takenBySemester()
        // The newest semester with enrollments is still open: a missing
        // result there is a class in progress. In every earlier semester the
        // student has already moved on, so a missing result is an
        // abandonment — it scores 0 over the discipline's full hours.
        let openSemesterId = ordered.last { !(takenBySemester[$0.id] ?? []).isEmpty }?.id

        var gradeHours = 0.0
        var totalHours = 0.0
        var points: [CoefficientCheckpoint] = []
        for semester in ordered {
            var closedAny = false
            for taken in takenBySemester[semester.id] ?? [] where taken.hours > 0 {
                if taken.finalGrade == nil, semester.id == openSemesterId { continue }
                let grade = taken.finalGrade ?? 0
                gradeHours += grade * Double(taken.hours)
                totalHours += Double(taken.hours)
                closedAny = true
            }
            if closedAny {
                points.append(CoefficientCheckpoint(semesterId: semester.id, value: gradeHours / totalHours))
            }
        }
        return points
    }

    struct CoefficientCheckpoint: Equatable {
        var semesterId: String
        var value: Double
    }

    private struct TakenDiscipline {
        var finalGrade: Double?
        var hours: Int
    }

    /// One entry per enrolled discipline offer, keyed by semester id. Group
    /// rows (theory + practice) replicate the discipline's result — dedup the
    /// way the Turmas mapping does: first non-nil over name-sorted groups,
    /// hours through the discipline → offer → widest-group fallback.
    private func takenBySemester() -> [String: [TakenDiscipline]] {
        let classesById = Dictionary(classes.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        let offersById = Dictionary(disciplineOffers.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        // Discipline ids repeat across semesters — the table's key is
        // (semesterId, id).
        let disciplineHours = Dictionary(
            disciplines.compactMap { row in row.hours.map { ([row.semesterId, row.id], $0) } },
            uniquingKeysWith: { first, _ in first }
        )
        let enrollmentsByOffer = Dictionary(
            grouping: studentClasses.compactMap { row -> (offerId: String, row: StudentClassRecord, group: ClassRecord)? in
                guard let group = classesById[row.classId] else { return nil }
                return (group.offerId, row, group)
            },
            by: \.offerId
        )

        var taken: [String: [TakenDiscipline]] = [:]
        // Offer order keeps the running sums bit-identical between passes, so
        // observation re-fetches never look like changes.
        for (offerId, enrollments) in enrollmentsByOffer.sorted(by: { $0.key < $1.key }) {
            let ordered = enrollments
                .sorted { ($0.group.groupName ?? "", $0.row.id) < ($1.group.groupName ?? "", $1.row.id) }
            let semesterId = ordered[0].row.semesterId
            let offer = offersById[offerId]
            let hours = offer.flatMap { disciplineHours[[semesterId, $0.disciplineId]] }
                ?? offer?.hours
                ?? ordered.map(\.group.hours).max() ?? 0
            taken[semesterId, default: []].append(
                TakenDiscipline(
                    finalGrade: ordered.firstNonNil(\.row.finalGrade).flatMap(parseDecimal),
                    hours: hours
                )
            )
        }
        return taken
    }
}
