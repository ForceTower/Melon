import Foundation

// MARK: - Lookup tables shared by the snapshot → domain mappings

/// Joins over the snapshot's flat record arrays, built once per mapping pass
/// and shared by the Home and Schedule overviews.
struct SnapshotIndex {
    let enrolledClassIds: Set<String>
    let spacesById: [String: SpaceRecord]
    let studentClassesById: [String: StudentClassRecord]
    /// Disciplines in card order (locale-aware by name); index == colorIndex.
    let sortedDisciplines: [DisciplineRecord]

    private let snapshot: SemesterSnapshot
    private let classesById: [String: ClassRecord]
    private let offersById: [String: DisciplineOfferRecord]
    private let disciplinesById: [String: DisciplineRecord]
    private let teachersById: [String: TeacherRecord]
    private let teacherIdByClass: [String: String]
    private let colorIndexByDisciplineId: [String: Int]

    init(snapshot: SemesterSnapshot) {
        self.snapshot = snapshot
        enrolledClassIds = Set(snapshot.studentClasses.map(\.classId))
        classesById = Dictionary(snapshot.classes.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        offersById = Dictionary(snapshot.disciplineOffers.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        disciplinesById = Dictionary(snapshot.disciplines.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        spacesById = Dictionary(snapshot.spaces.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        teachersById = Dictionary(snapshot.teachers.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        teacherIdByClass = Dictionary(snapshot.classTeachers.map { ($0.classId, $0.teacherId) }, uniquingKeysWith: { first, _ in first })
        studentClassesById = Dictionary(snapshot.studentClasses.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        sortedDisciplines = snapshot.disciplines
            .sorted { $0.name.localizedStandardCompare($1.name) == .orderedAscending }
        colorIndexByDisciplineId = Dictionary(
            sortedDisciplines.enumerated().map { ($0.element.id, $0.offset) },
            uniquingKeysWith: { first, _ in first }
        )
    }

    func discipline(forClass classId: String) -> DisciplineRecord? {
        classesById[classId]
            .flatMap { offersById[$0.offerId] }
            .flatMap { disciplinesById[$0.disciplineId] }
    }

    func offerId(forClass classId: String) -> String? {
        classesById[classId]?.offerId
    }

    func teacherName(forClass classId: String) -> String? {
        teacherIdByClass[classId].flatMap { teachersById[$0]?.name }
    }

    func colorIndex(forClass classId: String) -> Int {
        discipline(forClass: classId).flatMap { colorIndexByDisciplineId[$0.id] } ?? 0
    }

    func displayCode(forClass classId: String) -> String {
        discipline(forClass: classId).map(displayCode(for:)) ?? ""
    }

    func displayCode(for discipline: DisciplineRecord) -> String {
        if let code = discipline.code?.trimmingCharacters(in: .whitespaces), !code.isEmpty {
            return code
        }
        return String(discipline.name.prefix(4)).uppercased()
    }

    /// Subject of the lecture posted for `date`, when the plan is filled in.
    func topic(forClass classId: String, on date: String) -> String? {
        snapshot.lectures
            .first {
                $0.classId == classId && $0.date == date
                    && $0.subject?.trimmingCharacters(in: .whitespaces).isEmpty == false
            }?
            .subject
    }

    /// Plain mean of the grades posted across every class of the discipline.
    func partialGrade(forDiscipline disciplineId: String) -> Double? {
        let offerIds = Set(snapshot.disciplineOffers.filter { $0.disciplineId == disciplineId }.map(\.id))
        let classIds = Set(snapshot.classes.filter { offerIds.contains($0.offerId) }.map(\.id))
        let studentClassIds = Set(snapshot.studentClasses.filter { classIds.contains($0.classId) }.map(\.id))
        let values = snapshot.studentGrades
            .filter { studentClassIds.contains($0.studentClassId) }
            .compactMap { $0.value.flatMap(Double.init) }
        guard !values.isEmpty else { return nil }
        return values.reduce(0, +) / Double(values.count)
    }
}

// MARK: - Day sessions

/// One merged block of a class on a given weekday.
struct DaySession {
    var allocationId: String
    var classId: String
    var startMinute: Int
    var endMinute: Int?
    var spaceId: String?
}

extension SemesterSnapshot {
    /// SAGRES may encode one class morning as several back-to-back slots;
    /// folds runs of the same class (gaps ≤ 15 min) into single sessions.
    func mergedSessions(on day: Int, index: SnapshotIndex) -> [DaySession] {
        let slots = allocations
            .compactMap { allocation -> DaySession? in
                guard index.enrolledClassIds.contains(allocation.classId),
                      allocation.day == day,
                      let start = parseHhMm(allocation.startTime)
                else { return nil }
                return DaySession(
                    allocationId: allocation.id,
                    classId: allocation.classId,
                    startMinute: start,
                    endMinute: parseHhMm(allocation.endTime),
                    spaceId: allocation.spaceId
                )
            }
            .sorted { ($0.startMinute, $0.allocationId) < ($1.startMinute, $1.allocationId) }

        var merged: [DaySession] = []
        for slot in slots {
            if var last = merged.last,
               last.classId == slot.classId,
               let lastEnd = last.endMinute,
               slot.startMinute <= lastEnd + 15 {
                last.endMinute = max(lastEnd, slot.endMinute ?? slot.startMinute)
                last.spaceId = last.spaceId ?? slot.spaceId
                merged[merged.count - 1] = last
            } else {
                merged.append(slot)
            }
        }
        return merged
    }
}
