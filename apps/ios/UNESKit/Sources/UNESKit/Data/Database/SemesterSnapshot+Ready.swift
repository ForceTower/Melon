import Foundation

// MARK: - Semester snapshot → ReadyOverview

extension SemesterSnapshot {
    func readyOverview(now: Date, calendar: Calendar = .current) -> ReadyOverview {
        let enrolledIds = Set(studentClasses.map(\.classId))
        let enrolledClasses = classes.filter { enrolledIds.contains($0.id) }
        let spark = gradeSpark

        return ReadyOverview(
            semesterCode: semester.code,
            classCount: enrolledClasses.count,
            // 1 credit = 15 class-hours (a 60h course is 4 credits).
            totalCredits: enrolledClasses.reduce(0) { $0 + $1.hours } / 15,
            nextClass: nextClass(enrolledIds: enrolledIds, now: now, calendar: calendar),
            coefficient: spark.isEmpty ? nil : spark.reduce(0, +) / Double(spark.count),
            gradeSpark: spark,
            attendancePercent: attendancePercent(enrolledIds: enrolledIds, now: now)
        )
    }

    /// Posted grade values in (date, ordinal) order. Grade values are decimal
    /// strings with a dot separator.
    private var gradeSpark: [Double] {
        studentGrades
            .filter { $0.value != nil }
            .sorted { ($0.date ?? "", $0.ordinal) < ($1.date ?? "", $1.ordinal) }
            .compactMap { $0.value.flatMap(Double.init) }
    }

    /// Presence over lectures already held (dated up to today).
    private func attendancePercent(enrolledIds: Set<String>, now: Date) -> Int? {
        let today = now.dayStamp
        let held = lectures.count { lecture in
            guard let date = lecture.date else { return false }
            return enrolledIds.contains(lecture.classId) && date <= today
        }
        guard held > 0 else { return nil }
        let missed = totalMissedClassHours
        return max(0, min(100, 100 - (missed * 100 + held / 2) / held))
    }

    /// The allocation with the smallest forward distance in the week from
    /// `now`, using the same week-slot arithmetic as the KMP dashboard.
    private func nextClass(enrolledIds: Set<String>, now: Date, calendar: Calendar) -> NextClassInfo? {
        let minutesInDay = 24 * 60
        let minutesInWeek = 7 * minutesInDay
        let parts = calendar.dateComponents([.weekday, .hour, .minute], from: now)
        // Calendar weekday is 1=Sunday..7; upstream is 0=Sunday..6.
        let nowSlot = (parts.weekday! - 1) * minutesInDay + parts.hour! * 60 + parts.minute!

        var winner: (delta: Int, allocation: AllocationRecord)?
        for allocation in allocations where enrolledIds.contains(allocation.classId) {
            guard let day = allocation.day, let start = parseHhMm(allocation.startTime) else { continue }
            let slot = day * minutesInDay + start
            let delta = ((slot - nowSlot) % minutesInWeek + minutesInWeek) % minutesInWeek
            if winner.map({ delta < $0.delta }) ?? true {
                winner = (delta, allocation)
            }
        }
        guard let winner else { return nil }

        let classesById = Dictionary(classes.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        let offersById = Dictionary(disciplineOffers.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        let disciplinesById = Dictionary(disciplines.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        let spacesById = Dictionary(spaces.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        let teachersById = Dictionary(teachers.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        let teacherIdByClass = Dictionary(classTeachers.map { ($0.classId, $0.teacherId) }, uniquingKeysWith: { first, _ in first })

        let allocation = winner.allocation
        let discipline = classesById[allocation.classId]
            .flatMap { offersById[$0.offerId] }
            .flatMap { disciplinesById[$0.disciplineId] }

        return NextClassInfo(
            disciplineName: discipline?.name ?? "",
            startTime: hhMm(allocation.startTime) ?? "",
            endTime: hhMm(allocation.endTime),
            location: allocation.spaceId.flatMap { spacesById[$0]?.location },
            teacherName: teacherIdByClass[allocation.classId].flatMap { teachersById[$0]?.name },
            startsInMinutes: winner.delta
        )
    }
}
