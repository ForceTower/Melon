import Foundation

// MARK: - Semester snapshot → DisciplineDetail

extension SemesterSnapshot {
    /// The full detail feed for one discipline, or nil when the id isn't in
    /// this snapshot. Mirrors `disciplineSummaries`' reading of the flat
    /// tables, then adds what only the detail screen renders: groups,
    /// per-group grade sections, the lecture timeline, and attachments.
    func disciplineDetail(disciplineId: String, now: Date, calendar: Calendar = .current) -> DisciplineDetail? {
        let sortedDisciplines = disciplines
            .sorted { $0.name.localizedStandardCompare($1.name) == .orderedAscending }
        guard let position = sortedDisciplines.firstIndex(where: { $0.id == disciplineId }) else { return nil }
        let discipline = sortedDisciplines[position]

        let teachersById = Dictionary(teachers.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        let teacherIdByClass = Dictionary(classTeachers.map { ($0.classId, $0.teacherId) }, uniquingKeysWith: { first, _ in first })
        let gradesByStudentClass = Dictionary(grouping: studentGrades, by: \.studentClassId)

        let offers = disciplineOffers.filter { $0.disciplineId == discipline.id }
        let offerIds = Set(offers.map(\.id))
        let groupsById = Dictionary(
            classes.filter { offerIds.contains($0.offerId) }.map { ($0.id, $0) },
            uniquingKeysWith: { first, _ in first }
        )
        let enrollments = studentClasses
            .compactMap { studentClass -> (studentClass: StudentClassRecord, group: ClassRecord)? in
                guard let group = groupsById[studentClass.classId] else { return nil }
                return (studentClass, group)
            }
            // Theory groups ("T01") sort ahead of practice ("T01P01"), so
            // "first" picks (teacher, result flags) land on the main group.
            .sorted { ($0.group.groupName ?? "", $0.studentClass.id) < ($1.group.groupName ?? "", $1.studentClass.id) }
        guard !enrollments.isEmpty else { return nil }

        func teacherName(of group: ClassRecord) -> String? {
            teacherIdByClass[group.id].flatMap { teachersById[$0]?.name }
        }

        let today = now.dayStamp
        let enrolledClassIds = Set(enrollments.map(\.group.id))
        let groupCodeByClass = Dictionary(
            enrollments.compactMap { enrollment in enrollment.group.groupName.map { (enrollment.group.id, $0) } },
            uniquingKeysWith: { first, _ in first }
        )

        return DisciplineDetail(
            id: discipline.id,
            semesterId: semester.id,
            code: displayCode(for: discipline),
            name: discipline.name,
            department: discipline.department,
            ementa: discipline.program?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty,
            teacherName: enrollments.firstNonNil { teacherName(of: $0.group) },
            hours: discipline.hours ?? offers.firstNonNil(\.hours)
                ?? enrollments.map(\.group.hours).max() ?? 0,
            // totalFaltas is replicated per group row — first, never summed.
            missedHours: enrollments.firstNonNil(\.studentClass.missedClasses) ?? 0,
            groups: enrollments.map { enrollment in
                DisciplineDetailGroup(
                    id: enrollment.group.id,
                    code: enrollment.group.groupName,
                    kind: enrollment.group.type?.trimmingCharacters(in: .whitespaces).nilIfEmpty,
                    teacherName: teacherName(of: enrollment.group)
                )
            },
            sections: gradeSections(for: enrollments, gradesByStudentClass: gradesByStudentClass, today: today, calendar: calendar),
            lectures: detailLectures(enrolledClassIds: enrolledClassIds, groupCodeByClass: groupCodeByClass, today: today),
            attachments: detailAttachments(enrolledClassIds: enrolledClassIds, groupCodeByClass: groupCodeByClass),
            finalGrade: enrollments.firstNonNil(\.studentClass.finalGrade).flatMap(parseDecimal),
            approved: enrollments.firstNonNil(\.studentClass.approved),
            wentToFinals: enrollments.contains { $0.studentClass.wentToFinals == true },
            colorIndex: position
        )
    }

    /// One section per group that contributes grades of its own. The backend
    /// replicates the discipline-level grade set onto every group row, so
    /// after deduplicating by upstream id most disciplines collapse to a
    /// single section — which is then unpinned from its group (nil
    /// `groupCode`) so it renders under every group filter.
    private func gradeSections(
        for enrollments: [(studentClass: StudentClassRecord, group: ClassRecord)],
        gradesByStudentClass: [String: [StudentGradeRecord]],
        today: String,
        calendar: Calendar
    ) -> [DisciplineGradeSection] {
        var seenGradeKeys: Set<String> = []
        var sections = enrollments.compactMap { enrollment -> DisciplineGradeSection? in
            let grades = (gradesByStudentClass[enrollment.studentClass.id] ?? [])
                .sorted { ($0.ordinal, $0.date ?? "", $0.id) < ($1.ordinal, $1.date ?? "", $1.id) }
                .filter { seenGradeKeys.insert($0.platformId ?? $0.id).inserted }
            guard !grades.isEmpty else { return nil }
            return DisciplineGradeSection(
                id: enrollment.group.id,
                name: enrollment.group.type?.trimmingCharacters(in: .whitespaces).nilIfEmpty,
                groupCode: enrollment.group.groupName,
                grades: grades.map { grade in
                    DisciplineDetailGrade(
                        id: grade.platformId ?? grade.id,
                        label: gradeLabel(grade),
                        title: gradeTitle(grade),
                        value: grade.value.flatMap(parseDecimal),
                        weight: grade.weight.flatMap(parseDecimal),
                        date: grade.date,
                        daysUntil: daysUntil(grade: grade, today: today, calendar: calendar)
                    )
                }
            )
        }
        if sections.count == 1 {
            sections[0].name = nil
            sections[0].groupCode = nil
        }
        return sections
    }

    private func detailLectures(
        enrolledClassIds: Set<String>,
        groupCodeByClass: [String: String],
        today: String
    ) -> [DisciplineLecture] {
        let materialCountByLecture = Dictionary(
            grouping: lectureMaterials.map(\.lectureId), by: \.self
        ).mapValues(\.count)

        return lectures
            .filter { enrolledClassIds.contains($0.classId) }
            .sorted { lhs, rhs in
                (lhs.date ?? "9999-99-99", lhs.ordinal ?? 0, lhs.id)
                    < (rhs.date ?? "9999-99-99", rhs.ordinal ?? 0, rhs.id)
            }
            .map { lecture in
                DisciplineLecture(
                    id: lecture.id,
                    groupCode: groupCodeByClass[lecture.classId],
                    date: lecture.date,
                    subject: lecture.subject?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty,
                    attachmentCount: materialCountByLecture[lecture.id] ?? 0,
                    isPast: lecture.date.map { $0 < today } ?? false
                )
            }
            // Upstream emits placeholder rows for cancelled / TBD slots.
            .filter { $0.subject != nil || $0.attachmentCount > 0 }
    }

    private func detailAttachments(
        enrolledClassIds: Set<String>,
        groupCodeByClass: [String: String]
    ) -> [DisciplineAttachment] {
        let lecturesById = Dictionary(lectures.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        return lectureMaterials
            .compactMap { material -> (material: LectureMaterialRecord, lecture: LectureRecord)? in
                guard let lecture = lecturesById[material.lectureId],
                      enrolledClassIds.contains(lecture.classId) else { return nil }
                return (material, lecture)
            }
            .sorted { lhs, rhs in
                // Newest first, then upstream position within the lecture.
                (rhs.lecture.date ?? "", lhs.material.position ?? 0, lhs.material.id)
                    < (lhs.lecture.date ?? "", rhs.material.position ?? 0, rhs.material.id)
            }
            .map { material, lecture in
                DisciplineAttachment(
                    id: material.id,
                    name: material.caption?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
                        ?? friendlyName(url: material.url),
                    url: material.url,
                    groupCode: groupCodeByClass[lecture.classId],
                    lectureDate: lecture.date
                )
            }
    }

    private func gradeTitle(_ grade: StudentGradeRecord) -> String {
        [grade.name, grade.nameShort]
            .compactMap { $0?.trimmingCharacters(in: .whitespaces) }
            .first { !$0.isEmpty } ?? "Avaliação \(grade.ordinal)"
    }

    private func daysUntil(grade: StudentGradeRecord, today: String, calendar: Calendar) -> Int? {
        guard grade.value == nil,
              let date = grade.date, date >= today,
              let target = parseDayStamp(date, calendar: calendar),
              let start = parseDayStamp(today, calendar: calendar)
        else { return nil }
        return max(0, calendar.dateComponents([.day], from: start, to: target).day ?? 0)
    }

    private func friendlyName(url: String) -> String {
        if let parsed = URL(string: url) {
            let last = parsed.lastPathComponent
            if !last.isEmpty, last != "/" { return last }
        }
        return url
    }
}

extension String {
    var nilIfEmpty: String? { isEmpty ? nil : self }
}
