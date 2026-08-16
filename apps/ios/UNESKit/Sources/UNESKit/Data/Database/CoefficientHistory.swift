import Foundation

// MARK: - Mirror records → cross-semester CoefficientSummary

/// The coefficient (CR) of one program: the mean of every discipline taken
/// in it, weighted by class-hours, walked chronologically so the sparkline
/// plots the CR as it stood after each of its semesters.
///
/// A program is a semester track — nil is the regular undergrad calendar,
/// anything else a program that runs its own (a mestrado, an EAD course).
/// The CR is never taken across programs: a student who finished a
/// graduação and started a mestrado has two, and their mean describes
/// neither degree (issue #55).
struct CoefficientHistory {
    var semesters: [SemesterRecord]
    var disciplines: [DisciplineRecord]
    var disciplineOffers: [DisciplineOfferRecord]
    var classes: [ClassRecord]
    var studentClasses: [StudentClassRecord]

    /// The CR to show where there is room for exactly one number: the newest
    /// program that has closed anything. A program the student just started
    /// has no CR of its own yet, so it falls through to the previous one
    /// rather than leaving the surface blank.
    ///
    /// Nil until some discipline has a closed result — callers keep the
    /// active snapshot's partial mean as the stand-in until then.
    func summary() -> CoefficientSummary? {
        tracksByRecency().lazy.compactMap { summary(track: $0) }.first
    }

    /// Every program the student has a CR in, newest first. Programs with
    /// nothing closed yet are left out — they have no coefficient to show.
    func programs() -> [ProgramCoefficient] {
        tracksByRecency().compactMap { track in
            summary(track: track).map { ProgramCoefficient(track: track, summary: $0) }
        }
    }

    /// One program's CR, or nil when none of its disciplines has closed.
    func summary(track: String?) -> CoefficientSummary? {
        let spark = checkpoints(track: track).map(\.value)
        guard let value = spark.last else { return nil }
        return CoefficientSummary(
            value: value,
            spark: spark,
            delta: spark.count >= 2 ? value - spark[spark.count - 2] : nil
        )
    }

    /// The programs the student has semesters in, newest first.
    private func tracksByRecency() -> [String?] {
        var tracks: [String?] = []
        for semester in semesters.sorted(by: { ($0.startDate, $0.code) > ($1.startDate, $1.code) })
        where !tracks.contains(semester.track) {
            tracks.append(semester.track)
        }
        return tracks
    }

    /// The CR as it stood after each semester of one program that closed
    /// anything, in chronological order — the sparkline with its semesters
    /// attached, so the Retrospectiva can read the before/after around one
    /// semester.
    func checkpoints(track: String?) -> [CoefficientCheckpoint] {
        let ordered = semesters
            .filter { $0.track == track }
            .sorted { ($0.startDate, $0.code) < ($1.startDate, $1.code) }
        let takenBySemester = takenBySemester()

        var gradeHours = 0.0
        var totalHours = 0.0
        var points: [CoefficientCheckpoint] = []
        for semester in ordered {
            var closedAny = false
            for taken in takenBySemester[semester.id] ?? [] where taken.hours > 0 {
                // Only an explicit result closes a discipline. A missing
                // final grade with no posted verdict is a class still in
                // progress, an exemption, or mirror data the sync hasn't
                // caught up with — none of those may read as a 0, which
                // would drag the whole CR down. An explicit failure without
                // a posted mean (e.g. reprovado por falta) still scores 0
                // over the discipline's full hours.
                let grade: Double
                if let finalGrade = taken.finalGrade {
                    grade = finalGrade
                } else if taken.approved == false {
                    grade = 0
                } else {
                    continue
                }
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
        var approved: Bool?
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
                    approved: ordered.firstNonNil(\.row.approved),
                    hours: hours
                )
            )
        }
        return taken
    }
}
