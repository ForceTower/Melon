import Foundation

// MARK: - Semester snapshot → [DisciplineSummary]

extension SemesterSnapshot {
    /// One summary per enrolled discipline offer, in the same name-sorted
    /// order Home uses — so index == colorIndex and a discipline keeps its
    /// color across tabs.
    func disciplineSummaries(now: Date, calendar: Calendar = .current) -> [DisciplineSummary] {
        let classesById = Dictionary(classes.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        let teachersById = Dictionary(teachers.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        let teacherIdByClass = Dictionary(classTeachers.map { ($0.classId, $0.teacherId) }, uniquingKeysWith: { first, _ in first })
        let gradesByStudentClass = Dictionary(grouping: studentGrades, by: \.studentClassId)

        let enrollmentsByOffer = Dictionary(
            grouping: studentClasses.compactMap { studentClass -> (offerId: String, studentClass: StudentClassRecord, group: ClassRecord)? in
                guard let group = classesById[studentClass.classId] else { return nil }
                return (group.offerId, studentClass, group)
            },
            by: \.offerId
        )

        let sortedDisciplines = disciplines
            .sorted { $0.name.localizedStandardCompare($1.name) == .orderedAscending }

        return sortedDisciplines.enumerated().compactMap { position, discipline in
            let enrollments = disciplineOffers
                .filter { $0.disciplineId == discipline.id }
                .flatMap { enrollmentsByOffer[$0.id] ?? [] }
                // Theory groups ("T01") sort ahead of practice ("T01P01"), so
                // "first" picks (teacher, result flags) land on the main group.
                .sorted { ($0.group.groupName ?? "", $0.studentClass.id) < ($1.group.groupName ?? "", $1.studentClass.id) }
            guard !enrollments.isEmpty else { return nil }

            // The backend replicates the discipline's grade set onto every
            // group row — dedup by upstream id or the chips double up and the
            // average over-weights nothing in particular.
            var seenGradeKeys: Set<String> = []
            let grades = enrollments
                .flatMap { gradesByStudentClass[$0.studentClass.id] ?? [] }
                .sorted { ($0.ordinal, $0.date ?? "", $0.id) < ($1.ordinal, $1.date ?? "", $1.id) }
                .filter { seenGradeKeys.insert($0.platformId ?? $0.id).inserted }

            let today = now.dayStamp
            let nextEvaluation = grades
                .first { $0.value == nil && ($0.date ?? "") >= today }
                .flatMap { grade -> UpcomingEvaluation? in
                    guard let date = grade.date, let target = parseDayStamp(date, calendar: calendar) else { return nil }
                    let days = calendar.dateComponents(
                        [.day],
                        from: calendar.startOfDay(for: now),
                        to: target
                    ).day ?? 0
                    return UpcomingEvaluation(label: gradeLabel(grade), daysUntil: max(0, days))
                }

            return DisciplineSummary(
                id: discipline.id,
                code: displayCode(for: discipline),
                name: discipline.name,
                teacherName: enrollments
                    .firstNonNil { teacherIdByClass[$0.group.id].flatMap { teachersById[$0]?.name } },
                hours: discipline.hours ?? enrollments.reduce(0) { $0 + $1.group.hours },
                // totalFaltas is replicated per group row — first, never summed.
                missedHours: enrollments.firstNonNil(\.studentClass.missedClasses) ?? 0,
                groupsLabel: groupsLabel(for: enrollments.map(\.group)),
                grades: grades.map {
                    DisciplineGrade(
                        id: $0.platformId ?? $0.id,
                        label: gradeLabel($0),
                        value: $0.value.flatMap(parseDecimal),
                        date: $0.date
                    )
                },
                partialAverage: partialAverage(of: grades),
                finalGrade: enrollments.firstNonNil(\.studentClass.finalGrade).flatMap(parseDecimal),
                approved: enrollments.firstNonNil(\.studentClass.approved),
                wentToFinals: enrollments.contains { $0.studentClass.wentToFinals == true },
                nextEvaluation: nextEvaluation,
                colorIndex: position
            )
        }
    }

    /// Weighted mean of the released grades; upstream always sends weights,
    /// but fall back to the plain mean rather than dropping the average when
    /// they come through malformed.
    private func partialAverage(of grades: [StudentGradeRecord]) -> Double? {
        let released = grades.compactMap { grade in
            grade.value.flatMap(parseDecimal).map { (value: $0, weight: grade.weight.flatMap(parseDecimal)) }
        }
        guard !released.isEmpty else { return nil }

        let weighted = released.compactMap { entry in entry.weight.map { (entry.value, $0) } }
        let weightSum = weighted.reduce(0) { $0 + $1.1 }
        guard weighted.count == released.count, weightSum > 0 else {
            return released.reduce(0) { $0 + $1.value } / Double(released.count)
        }
        return weighted.reduce(0) { $0 + $1.0 * $1.1 } / weightSum
    }

    private func gradeLabel(_ grade: StudentGradeRecord) -> String {
        [grade.nameShort, grade.name]
            .compactMap { $0?.trimmingCharacters(in: .whitespaces) }
            .first { !$0.isEmpty } ?? "AV\(grade.ordinal)"
    }

    /// "Te · Pr" when the enrollment spans more than one group kind.
    private func groupsLabel(for groups: [ClassRecord]) -> String? {
        guard groups.count > 1 else { return nil }
        var slugs: [String] = []
        for kind in groups.compactMap({ $0.type?.trimmingCharacters(in: .whitespaces) }) where !kind.isEmpty {
            let slug = String(kind.prefix(2))
            if !slugs.contains(slug) { slugs.append(slug) }
        }
        guard slugs.count > 1 else { return nil }
        return slugs.joined(separator: " · ")
    }

    private func displayCode(for discipline: DisciplineRecord) -> String {
        if let code = discipline.code?.trimmingCharacters(in: .whitespaces), !code.isEmpty {
            return code
        }
        return String(discipline.name.prefix(4)).uppercased()
    }

    /// Decimal strings arrive dot-separated; tolerate a comma just in case.
    private func parseDecimal(_ value: String) -> Double? {
        Double(value.replacingOccurrences(of: ",", with: "."))
    }

    private func parseDayStamp(_ stamp: String, calendar: Calendar) -> Date? {
        let parts = stamp.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        return calendar.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2]))
    }
}

extension Sequence {
    func firstNonNil<T>(_ transform: (Element) -> T?) -> T? {
        for element in self {
            if let value = transform(element) { return value }
        }
        return nil
    }
}
