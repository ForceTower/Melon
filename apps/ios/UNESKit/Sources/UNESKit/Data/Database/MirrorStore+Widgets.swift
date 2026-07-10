import Foundation
import GRDB

// MARK: - Semester snapshot → widget schedule pattern

extension MirrorStore {
    /// Emits the widget's weekly pattern on subscription and again after
    /// every write that feeds it; nil until the first successful refresh
    /// lands (or after a full wipe), so the widget can show its sign-in state.
    func widgetScheduleUpdates(now: @escaping @Sendable () -> Date) -> AsyncValueObservation<WidgetScheduleSnapshot?> {
        ValueObservation
            .tracking { db in try Self.widgetSchedule(db, now: now()) }
            .removeDuplicates()
            .values(in: writer)
    }

    private static func widgetSchedule(_ db: Database, now: Date) throws -> WidgetScheduleSnapshot? {
        guard try lastSyncedAt(db) != nil else { return nil }
        let semesters = try SemesterRecord.order(Column("startDate").desc).fetchAll(db)
        guard let active = semesters.map(\.domain).active(today: now.dayStamp),
              let record = semesters.first(where: { $0.id == active.id })
        else { return WidgetScheduleSnapshot() }
        return try snapshot(for: record, db: db).widgetSchedule(now: now)
    }
}

extension SemesterSnapshot {
    func widgetSchedule(now: Date, calendar: Calendar = .current) -> WidgetScheduleSnapshot {
        let index = SnapshotIndex(snapshot: self)

        var sessions: [WidgetScheduleSnapshot.Session] = []
        for day in 0..<7 {
            for session in mergedSessions(on: day, index: index) {
                sessions.append(WidgetScheduleSnapshot.Session(
                    classId: session.classId,
                    day: day,
                    startMinute: session.startMinute,
                    endMinute: session.endMinute,
                    code: index.displayCode(forClass: session.classId),
                    title: index.discipline(forClass: session.classId)?.name ?? "",
                    room: nonEmpty(session.spaceId.flatMap { index.spacesById[$0]?.location }),
                    teacherName: index.teacherName(forClass: session.classId),
                    colorIndex: index.colorIndex(forClass: session.classId),
                    disciplineId: index.discipline(forClass: session.classId)?.id
                ))
            }
        }

        // Lecture subjects for the next two weeks — enough for any timeline
        // the widget builds between two app sessions. A split class-day posts
        // one lecture per slot; every surface shows a single subject per
        // class-day (and the watch store keys on it), so keep the first.
        let today = now.dayStamp
        let horizon = calendar.date(byAdding: .day, value: 14, to: now)?.dayStamp ?? today
        var topics: [WidgetScheduleSnapshot.Topic] = []
        var coveredDays: Set<String> = []
        for lecture in lectures {
            guard index.enrolledClassIds.contains(lecture.classId),
                  let date = lecture.date, date >= today, date <= horizon,
                  let subject = nonEmpty(lecture.subject),
                  coveredDays.insert("\(lecture.classId)|\(date)").inserted
            else { continue }
            topics.append(WidgetScheduleSnapshot.Topic(classId: lecture.classId, dayStamp: date, subject: subject))
        }

        return WidgetScheduleSnapshot(
            semesterCode: semester.code,
            sessions: sessions,
            topics: topics
        )
    }

    /// Upstream coalesces absent text fields to "" — treat those as missing.
    private func nonEmpty(_ value: String?) -> String? {
        guard let value = value?.trimmingCharacters(in: .whitespaces), !value.isEmpty else { return nil }
        return value
    }
}
