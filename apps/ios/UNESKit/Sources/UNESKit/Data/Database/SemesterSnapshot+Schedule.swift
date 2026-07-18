import Foundation

// MARK: - Semester snapshot → ScheduleOverview

extension SemesterSnapshot {
    /// The weekly timetable for the calendar week containing `now`,
    /// Monday-first, with lecture topics resolved against each day's date.
    func scheduleOverview(now: Date, calendar: Calendar = .current) -> ScheduleOverview {
        let index = SnapshotIndex(snapshot: self)
        let days = ScheduleWeek.dates(containing: now, calendar: calendar).enumerated().map { position, date in
            // Monday-first strip position → upstream day (0 = Sunday).
            let upstreamDay = (position + 1) % 7
            let stamp = date.dayStamp
            return ScheduleDay(
                dayStamp: stamp,
                dayNumber: calendar.component(.day, from: date),
                classes: mergedSessions(on: upstreamDay, index: index).map { session in
                    scheduleClass(for: session, on: stamp, index: index)
                }
            )
        }
        return ScheduleOverview(
            semesterId: semester.id,
            semesterCode: semester.code,
            weekOfYear: ScheduleWeek.weekOfYear(containing: now, calendar: calendar),
            days: days
        )
    }

    private func scheduleClass(for session: DaySession, on dayStamp: String, index: SnapshotIndex) -> ScheduleClass {
        let space = session.spaceId.flatMap { index.spacesById[$0] }
        return ScheduleClass(
            id: session.allocationId,
            classId: session.classId,
            disciplineId: index.discipline(forClass: session.classId)?.id ?? session.classId,
            offerId: index.offerId(forClass: session.classId),
            code: index.displayCode(forClass: session.classId),
            title: index.discipline(forClass: session.classId)?.name ?? "",
            startMinute: session.startMinute,
            endMinute: session.endMinute,
            teacherName: index.teacherName(forClass: session.classId),
            topic: index.topic(forClass: session.classId, on: dayStamp),
            modulo: nonEmpty(space?.modulo),
            room: nonEmpty(space?.location),
            campus: nonEmpty(space?.campus),
            colorIndex: index.colorIndex(forClass: session.classId)
        )
    }
}

/// Upstream coalesces absent location fields to "" — treat those as missing.
private func nonEmpty(_ value: String?) -> String? {
    guard let value = value?.trimmingCharacters(in: .whitespaces), !value.isEmpty else { return nil }
    return value
}
