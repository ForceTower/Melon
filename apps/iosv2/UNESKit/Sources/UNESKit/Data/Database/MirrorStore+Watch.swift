import Foundation
import GRDB

// MARK: - Semester snapshot → watch payload

extension MirrorStore {
    /// Emits the watch payload on subscription and again after every write
    /// that feeds it; nil until the first successful refresh lands (or after
    /// a wipe), so the watch can show its sign-in state.
    func watchSnapshotUpdates(now: @escaping @Sendable () -> Date) -> AsyncValueObservation<WatchSnapshot?> {
        ValueObservation
            .tracking { db in try Self.watchSnapshot(db, now: now()) }
            .removeDuplicates()
            .values(in: writer)
    }

    private static func watchSnapshot(_ db: Database, now: Date) throws -> WatchSnapshot? {
        guard let syncedAt = try lastSyncedAt(db) else { return nil }

        var payload = WatchSnapshot(syncedAt: syncedAt)
        let semesters = try SemesterRecord.order(Column("startDate").desc).fetchAll(db)
        if let active = semesters.map(\.domain).active(today: now.dayStamp),
           let record = semesters.first(where: { $0.id == active.id }) {
            let snapshot = try snapshot(for: record, db: db)
            payload.schedule = snapshot.widgetSchedule(now: now)

            let home = snapshot.homeOverview(now: now)
            payload.attendancePercent = home.attendance?.percent
            payload.remainingAbsences = home.attendance?.remainingAbsences
            payload.nextExam = home.nextExam.map {
                WatchSnapshot.Exam(
                    label: $0.label,
                    disciplineName: $0.disciplineName,
                    date: $0.date,
                    time: $0.time
                )
            }

            payload.disciplines = snapshot.disciplineSummaries(now: now).map { summary in
                WatchSnapshot.Discipline(
                    id: summary.id,
                    code: summary.code,
                    name: summary.name,
                    teacherName: summary.teacherName,
                    hours: summary.hours,
                    missedHours: summary.missedHours,
                    grades: summary.grades.map {
                        WatchSnapshot.Grade(id: $0.id, label: $0.label, value: $0.value, date: $0.date)
                    },
                    partialAverage: summary.partialAverage,
                    colorIndex: summary.colorIndex
                )
            }
        }
        if let coefficient = try coefficientHistory(semesters: semesters, db: db).summary() {
            payload.coefficient = coefficient.value
            payload.coefficientDelta = coefficient.delta
        }
        return payload
    }
}
