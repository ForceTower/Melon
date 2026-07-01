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
