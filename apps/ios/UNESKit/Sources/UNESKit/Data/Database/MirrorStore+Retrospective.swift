import Foundation
import GRDB

// MARK: - Mirror → RetrospectiveDeck (one closed semester distilled)

extension MirrorStore {
    /// How long a semester's Retrospectiva window stays open after its end
    /// date — a moment, not a season.
    static let retrospectiveWindowDays = 14

    /// The semester whose window is open right now — the most recently
    /// ended one, still inside its window, with at least half its results
    /// decided (a story of pending grades isn't worth telling). Nil keeps
    /// every entry point hidden; no per-semester flag flipping needed.
    func retrospectiveWindowCode(now: Date) async throws -> String? {
        try await writer.read { db in try Self.retrospectiveWindowCode(db: db, now: now) }
    }

    static func retrospectiveWindowCode(
        db: Database,
        now: Date,
        calendar: Calendar = .current
    ) throws -> String? {
        let today = now.dayStamp
        let ended = try SemesterRecord
            .filter(Column("endDate") < today)
            .order(Column("endDate").desc)
            .fetchOne(db)
        guard let ended else { return nil }

        let parts = ended.endDate.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3,
              let end = calendar.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2])),
              let closes = calendar.date(byAdding: .day, value: retrospectiveWindowDays, to: end),
              now <= closes
        else { return nil }

        let summaries = try snapshot(for: ended, db: db).disciplineSummaries(now: now)
        let decided = summaries.count { $0.approved != nil }
        guard !summaries.isEmpty, decided * 2 >= summaries.count else { return nil }
        return ended.code
    }

    /// Nil when the semester isn't mirrored or has no enrollments — the
    /// entry points hide themselves then, even with the flag on.
    func retrospective(semesterCode: String, now: Date) async throws -> RetrospectiveDeck? {
        try await writer.read { db in try Self.retrospective(semesterCode: semesterCode, db: db, now: now) }
    }

    static func retrospective(semesterCode: String, db: Database, now: Date) throws -> RetrospectiveDeck? {
        guard let semester = try SemesterRecord.filter(Column("code") == semesterCode).fetchOne(db)
        else { return nil }
        let snapshot = try snapshot(for: semester, db: db)
        let summaries = snapshot.disciplineSummaries(now: now)
        guard !summaries.isEmpty else { return nil }

        let semesters = try SemesterRecord.order(Column("startDate").desc).fetchAll(db)
        let checkpoints = try coefficientHistory(semesters: semesters, db: db).checkpoints()

        let totalHours = summaries.reduce(0) { $0 + $1.hours }
        let missedHours = summaries.reduce(0) { $0 + $1.missedHours }
        let graded = summaries.filter { $0.finalGrade != nil }

        return RetrospectiveDeck(
            semesterCode: semesterCode,
            semesterLabel: semesterLabel(code: semesterCode),
            nextLabel: nextSemesterLabel(code: semesterCode),
            glance: RetrospectiveDeck.Glance(
                disciplines: summaries.count,
                classHours: totalHours,
                weeks: weekSpan(semester: semester)
            ),
            grades: grades(from: graded, totalHours: totalHours),
            attendance: totalHours > 0
                ? RetrospectiveDeck.Attendance(
                    percent: max(0, min(100, Int((100 * Double(totalHours - missedHours) / Double(totalHours)).rounded()))),
                    missedHours: missedHours
                )
                : nil,
            victory: victory(from: graded),
            score: score(checkpoints: checkpoints, semesterId: semester.id),
            failures: summaries.filter { $0.approved == false }.map(\.name)
        )
    }

    /// "20261" → "2026.1"; codes outside the pattern pass through unchanged.
    static func semesterLabel(code: String) -> String {
        guard code.count == 5, code.allSatisfy(\.isNumber) else { return code }
        return "\(code.prefix(4)).\(code.suffix(1))"
    }

    /// The closing card's hand-off: "20261" → "2026.2", "20252" → "2026.1".
    static func nextSemesterLabel(code: String) -> String {
        guard code.count == 5, code.allSatisfy(\.isNumber),
              let year = Int(code.prefix(4)), let half = Int(code.suffix(1))
        else { return code }
        return half == 1 ? "\(year).2" : "\(year + 1).1"
    }

    private static func grades(
        from graded: [DisciplineSummary],
        totalHours: Int
    ) -> RetrospectiveDeck.Grades? {
        guard let best = graded.max(by: { ($0.finalGrade ?? 0) < ($1.finalGrade ?? 0) }),
              let bestGrade = best.finalGrade
        else { return nil }
        let weighted = graded.reduce(into: (grades: 0.0, hours: 0.0)) { sums, summary in
            guard let grade = summary.finalGrade, summary.hours > 0 else { return }
            sums.grades += grade * Double(summary.hours)
            sums.hours += Double(summary.hours)
        }
        let media = weighted.hours > 0
            ? weighted.grades / weighted.hours
            : graded.compactMap(\.finalGrade).reduce(0, +) / Double(graded.count)
        return RetrospectiveDeck.Grades(media: media, bestGrade: bestGrade, bestDiscipline: best.name)
    }

    /// A survived Prova Final outranks everything; ties (and the no-finals
    /// semester) resolve to the lowest passing grade — the one that made the
    /// student sweat.
    private static func victory(from graded: [DisciplineSummary]) -> RetrospectiveDeck.Victory? {
        let approved = graded.filter { $0.approved == true && $0.finalGrade != nil }
        let pick = approved.filter(\.wentToFinals).min { ($0.finalGrade ?? 11) < ($1.finalGrade ?? 11) }
            ?? approved.min { ($0.finalGrade ?? 11) < ($1.finalGrade ?? 11) }
        guard let pick, let grade = pick.finalGrade else { return nil }
        return RetrospectiveDeck.Victory(discipline: pick.name, grade: grade, viaFinal: pick.wentToFinals)
    }

    private static func score(
        checkpoints: [CoefficientHistory.CoefficientCheckpoint],
        semesterId: String
    ) -> RetrospectiveDeck.ScoreCard? {
        guard let index = checkpoints.firstIndex(where: { $0.semesterId == semesterId }) else { return nil }
        let series = checkpoints[max(0, index - 4)...index].map(\.value)
        return RetrospectiveDeck.ScoreCard(
            value: checkpoints[index].value,
            previous: index > 0 ? checkpoints[index - 1].value : nil,
            series: series
        )
    }

    private static func weekSpan(semester: SemesterRecord, calendar: Calendar = .current) -> Int {
        func parse(_ stamp: String) -> Date? {
            let parts = stamp.split(separator: "-").compactMap { Int($0) }
            guard parts.count == 3 else { return nil }
            return calendar.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2]))
        }
        guard let start = parse(semester.startDate), let end = parse(semester.endDate),
              let days = calendar.dateComponents([.day], from: start, to: end).day, days > 0
        else { return 0 }
        return (days + 6) / 7
    }
}
