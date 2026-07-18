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
            let offers = disciplineOffers.filter { $0.disciplineId == discipline.id }
            let enrollments = offers
                .flatMap { enrollmentsByOffer[$0.id] ?? [] }
                // Theory groups ("T01") sort ahead of practice ("T01P01"), so
                // "first" picks (teacher, result flags) land on the main group.
                .sorted { ($0.group.groupName ?? "", $0.studentClass.id) < ($1.group.groupName ?? "", $1.studentClass.id) }
            guard !enrollments.isEmpty else { return nil }

            // The backend replicates the discipline's grade set onto every
            // group row — dedup by upstream id or the chips double up and the
            // average over-weights nothing in particular. The Prova Final row
            // is not a regular evaluation: it renders as the last chip, but
            // stays out of the average and the next-evaluation pick —
            // `wentToFinals` already carries the state.
            var seenGradeKeys: Set<String> = []
            let dedupedGrades = enrollments
                .flatMap { gradesByStudentClass[$0.studentClass.id] ?? [] }
                .sorted { ($0.ordinal, $0.date ?? "", $0.id) < ($1.ordinal, $1.date ?? "", $1.id) }
                .filter { seenGradeKeys.insert($0.platformId ?? $0.id).inserted }
            let grades = dedupedGrades.filter { !isFinalExamRow($0) }
            let finalExam = dedupedGrades.first(where: isFinalExamRow)

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
                offerId: offers.count == 1 ? offers.first?.id : nil,
                code: displayCode(for: discipline),
                name: discipline.name,
                teacherName: enrollments
                    .firstNonNil { teacherIdByClass[$0.group.id].flatMap { teachersById[$0]?.name } },
                // Group hours are per-group slices of the same catalog total,
                // so summing them double-counts multi-group disciplines —
                // fall back through the offer instead.
                hours: discipline.hours ?? offers.firstNonNil(\.hours)
                    ?? enrollments.map(\.group.hours).max() ?? 0,
                // totalFaltas is replicated per group row — first, never summed.
                missedHours: enrollments.firstNonNil(\.studentClass.missedClasses) ?? 0,
                groupsLabel: groupsLabel(for: enrollments.map(\.group)),
                grades: grades.map {
                    DisciplineGrade(
                        id: $0.platformId ?? $0.id,
                        label: gradeLabel($0),
                        name: gradeTitle($0),
                        value: $0.value.flatMap(parseDecimal),
                        date: $0.date
                    )
                } + (finalExam.map {
                    // The SAGRES "Adicional" short label gives way to
                    // localized copy, same as the detail section.
                    [DisciplineGrade(
                        id: $0.platformId ?? $0.id,
                        label: String.localized(.disciplinesDetailFinalExamLabel),
                        name: gradeTitle($0),
                        value: $0.value.flatMap(parseDecimal),
                        date: $0.date
                    )]
                } ?? []),
                partialAverage: partialAverage(of: grades),
                finalGrade: enrollments.firstNonNil(\.studentClass.finalGrade).flatMap(parseDecimal),
                approved: enrollments.firstNonNil(\.studentClass.approved),
                wentToFinals: enrollments.contains { $0.studentClass.wentToFinals == true },
                nextEvaluation: nextEvaluation,
                colorIndex: position
            )
        }
    }

    private func partialAverage(of grades: [StudentGradeRecord]) -> Double? {
        DisciplineRules.partialAverage(
            of: grades.compactMap { grade in
                grade.value.flatMap(parseDecimal).map { ($0, grade.weight.flatMap(parseDecimal)) }
            }
        )
    }

    /// The SAGRES Prova Final row: name "Prova Final" with the "Adicional"
    /// short label, under the "Notas Complementares" evaluation upstream.
    /// Both fields must match — teachers name regular evaluations "prova
    /// final" too, but those carry AV-style short labels.
    func isFinalExamRow(_ grade: StudentGradeRecord) -> Bool {
        func normalized(_ value: String?) -> String? {
            value?.trimmingCharacters(in: .whitespaces).lowercased()
        }
        return normalized(grade.name) == "prova final" && normalized(grade.nameShort) == "adicional"
    }

    // Shared with the discipline-detail mapping.
    func gradeLabel(_ grade: StudentGradeRecord) -> String {
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

    func displayCode(for discipline: DisciplineRecord) -> String {
        if let code = discipline.code?.trimmingCharacters(in: .whitespaces), !code.isEmpty {
            return code
        }
        return String(discipline.name.prefix(4)).uppercased()
    }

}

/// Decimal strings arrive dot-separated; tolerate a comma just in case.
func parseDecimal(_ value: String) -> Double? {
    Double(value.replacingOccurrences(of: ",", with: "."))
}

extension Sequence {
    func firstNonNil<T>(_ transform: (Element) -> T?) -> T? {
        for element in self {
            if let value = transform(element) { return value }
        }
        return nil
    }
}
