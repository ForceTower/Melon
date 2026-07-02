import Foundation

/// One semester's mirrored scope, in record form. Both the wire payload
/// (`SemesterPayloadDTO.snapshot`) and the local mirror (`MirrorStore`)
/// produce this shape, so the Home and Ready mappings cannot drift between
/// the network and database paths.
struct SemesterSnapshot: Equatable, Sendable {
    var semester: SemesterRecord
    var disciplines: [DisciplineRecord] = []
    var disciplineOffers: [DisciplineOfferRecord] = []
    var classes: [ClassRecord] = []
    var teachers: [TeacherRecord] = []
    var classTeachers: [ClassTeacherRecord] = []
    var spaces: [SpaceRecord] = []
    var allocations: [AllocationRecord] = []
    var studentClasses: [StudentClassRecord] = []
    var studentGrades: [StudentGradeRecord] = []
    var lectures: [LectureRecord] = []
}

extension SemesterSnapshot {
    /// Semester-wide class-hours of absence. Upstream reports totalFaltas
    /// once per discipline and the backend replicates it onto every group
    /// row, so count one value per offer — summing the raw rows would
    /// double-count multi-group (theory + practice) disciplines.
    var totalMissedClassHours: Int {
        let offerByClass = Dictionary(classes.map { ($0.id, $0.offerId) }, uniquingKeysWith: { first, _ in first })
        var missedByOffer: [String: Int] = [:]
        for studentClass in studentClasses {
            guard let missed = studentClass.missedClasses else { continue }
            let offerId = offerByClass[studentClass.classId] ?? studentClass.classId
            missedByOffer[offerId] = missedByOffer[offerId] ?? missed
        }
        return missedByOffer.values.reduce(0, +)
    }

    /// Upstream times come as "HH:mm:ss"; the UI speaks "HH:mm".
    func hhMm(_ time: String?) -> String? {
        guard let time, time.count >= 5 else { return time }
        return String(time.prefix(5))
    }

    func parseHhMm(_ value: String?) -> Int? {
        guard let value, !value.isEmpty else { return nil }
        let parts = value.split(separator: ":")
        guard parts.count >= 2, let h = Int(parts[0]), let m = Int(parts[1].prefix(2)) else { return nil }
        return h * 60 + m
    }
}
