import Foundation

// MARK: - Semester snapshot → MeOverview

extension SemesterSnapshot {
    func meOverview(now: Date, calendar: Calendar = .current) -> MeOverview {
        let index = SnapshotIndex(snapshot: self)
        return MeOverview(
            semesterCode: semester.code,
            campus: campusLabel(index: index),
            coefficient: coefficientSummary,
            attendancePercent: attendanceSummary(index: index, today: now.dayStamp)?.percent,
            progress: semesterProgress(now: now, calendar: calendar),
            countdown: semesterCountdown(index: index, now: now, calendar: calendar)
        )
    }

    // MARK: Campus — where the student's classes most often meet

    private func campusLabel(index: SnapshotIndex) -> String? {
        let spaces = allocations
            .filter { index.enrolledClassIds.contains($0.classId) }
            .compactMap { $0.spaceId.flatMap { index.spacesById[$0] } }
        let campus = mode(of: spaces.compactMap { nonEmpty($0.campus) })
        let modulo = mode(of: spaces.compactMap { nonEmpty($0.modulo) })
        let parts = [campus, modulo].compactMap(\.self)
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }

    private func mode(of values: [String]) -> String? {
        let counts = Dictionary(values.map { ($0, 1) }, uniquingKeysWith: +)
        // Highest count wins; name breaks ties so the label is stable.
        return counts.min { left, right in
            if left.value != right.value { return left.value > right.value }
            return left.key < right.key
        }?.key
    }

    private func nonEmpty(_ value: String?) -> String? {
        guard let value = value?.trimmingCharacters(in: .whitespaces), !value.isEmpty else { return nil }
        return value
    }

    // MARK: Semester progress — week counter between the semester's dates

    private func semesterProgress(now: Date, calendar: Calendar) -> SemesterProgress? {
        guard let start = parseDayStamp(semester.startDate, calendar: calendar),
              let end = parseDayStamp(semester.endDate, calendar: calendar),
              start <= end
        else { return nil }

        let today = calendar.startOfDay(for: now)
        let totalDays = (calendar.dateComponents([.day], from: start, to: end).day ?? 0) + 1
        let elapsedDays = max(0, calendar.dateComponents([.day], from: start, to: today).day ?? 0)
        let totalWeeks = (totalDays + 6) / 7
        return SemesterProgress(
            week: min(totalWeeks, elapsedDays / 7 + 1),
            totalWeeks: totalWeeks,
            percent: min(100, Int((Double(elapsedDays) / Double(totalDays) * 100).rounded())),
            startStamp: semester.startDate,
            endStamp: semester.endDate
        )
    }

    // MARK: Final Countdown teaser — the semester's remainder, counted

    private func semesterCountdown(index: SnapshotIndex, now: Date, calendar: Calendar) -> SemesterCountdown? {
        guard let end = parseDayStamp(semester.endDate, calendar: calendar) else { return nil }
        let today = calendar.startOfDay(for: now)
        let daysLeft = max(0, calendar.dateComponents([.day], from: today, to: end).day ?? 0)

        let sessionsByWeekday = Dictionary(
            uniqueKeysWithValues: (0..<7).map { ($0, mergedSessions(on: $0, index: index)) }
        )
        let nowMinute = calendar.component(.hour, from: now) * 60 + calendar.component(.minute, from: now)
        var classesLeft = 0
        var weekendsLeft = 0
        for offset in 0...daysLeft {
            guard let day = calendar.date(byAdding: .day, value: offset, to: today) else { continue }
            let sessions = sessionsByWeekday[calendar.component(.weekday, from: day) - 1] ?? []
            // Today only counts the sessions that haven't started yet.
            classesLeft += offset == 0 ? sessions.count { $0.startMinute > nowMinute } : sessions.count
            if offset > 0, calendar.component(.weekday, from: day) == 7 {
                weekendsLeft += 1
            }
        }

        let todayStamp = now.dayStamp
        return SemesterCountdown(
            daysLeft: daysLeft,
            weeksLeft: daysLeft / 7,
            hoursLeft: daysLeft * 24,
            classesLeft: classesLeft,
            weekendsLeft: weekendsLeft,
            scheduledExams: studentGrades.count { $0.value == nil && ($0.date ?? "") >= todayStamp },
            disciplineCount: disciplines.count
        )
    }
}
