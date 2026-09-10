import AppIntents
import ComposableArchitecture
import Foundation

private let log = Log.scoped("AppIntents")

/// How many grades the spoken summary names before "e mais N".
private let spokenGradeCap = 3

// MARK: - Discipline-scoped answers (Phase 4)

extension IntentSupport {
    public struct DisciplineClassAnswer {
        public let dialog: IntentDialog
        public let card: IntentClassCardView?
    }

    public struct DisciplineGradesAnswer {
        public let dialog: IntentDialog
        public let card: IntentGradesCardView?
    }

    /// Answers "quando / onde é minha aula de X?" — and "qual o professor?",
    /// since the same sentence names them all. The occurrence set is the
    /// widget snapshot filtered to the discipline, run through the exact
    /// `NextClassStatus` rules, so Siri, the widget, and Hoje agree on what
    /// "next" means.
    public static func disciplineClass(
        semesterId: String,
        disciplineId: String,
        calendar: Calendar = .current
    ) async -> DisciplineClassAnswer {
        @ComposableArchitecture.Dependency(\.disciplinesRepository) var disciplinesRepository
        @ComposableArchitecture.Dependency(\.date) var date

        guard let schedule = WidgetSnapshotStore.load() else {
            log.info("discipline-class answered state=signedOut")
            return DisciplineClassAnswer(dialog: IntentDialog(.intentDialogSignedOut), card: nil)
        }
        let now = date.now
        let detail = try? await disciplinesRepository.detailCached(semesterId, disciplineId, now)
        let occurrences = schedule.occurrences(from: now, days: 9, calendar: calendar).filter { occurrence in
            if let id = occurrence.disciplineId { return id == disciplineId }
            // Snapshots published before the id existed: match on the code.
            return detail.map { $0.code == occurrence.code } ?? false
        }
        guard detail != nil || !occurrences.isEmpty else {
            log.info("discipline-class answered state=stale")
            return DisciplineClassAnswer(dialog: IntentDialog(.intentFinalExamDialogStale), card: nil)
        }

        let verdict = disciplineClassVerdict(now: now, occurrences: occurrences, calendar: calendar)
        let name = detail?.name ?? occurrences.first?.title ?? ""
        let teacher = detail?.teacherName ?? verdict.occurrence?.teacherName
        log.info("discipline-class answered state=\(verdict.kindLabel)")
        return DisciplineClassAnswer(
            dialog: dialog(disciplineClass: verdict, name: name, teacher: teacher),
            card: verdict.occurrence.map { IntentClassCardView(occurrence: $0) }
        )
    }

    /// The discipline's next occurrence classified for speech. Pure over
    /// the already-filtered occurrences.
    static func disciplineClassVerdict(
        now: Date,
        occurrences: [ClassOccurrence],
        calendar: Calendar
    ) -> DisciplineClassVerdict {
        func classify(_ occurrence: ClassOccurrence) -> DisciplineClassVerdict {
            if calendar.isDate(occurrence.start, inSameDayAs: now) { return .today(occurrence) }
            if let tomorrow = calendar.date(byAdding: .day, value: 1, to: now),
               calendar.isDate(occurrence.start, inSameDayAs: tomorrow) {
                return .tomorrow(occurrence)
            }
            return .weekday(occurrence)
        }
        let (status, _) = NextClassStatus.compute(at: now, occurrences: occurrences, calendar: calendar)
        switch status {
        case let .inClass(occurrence): return .inClass(occurrence)
        case let .upcoming(occurrence): return classify(occurrence)
        case let .dayDone(_, next?): return classify(next)
        case .dayDone(_, next: nil), .signedOut: return .none
        }
    }

    private static func dialog(
        disciplineClass verdict: DisciplineClassVerdict,
        name: String,
        teacher: String?
    ) -> IntentDialog {
        // Room and teacher ride as optional clauses so one sentence per
        // case covers every combination without four keys each.
        func details(_ occurrence: ClassOccurrence) -> String {
            var clauses = ""
            if let room = occurrence.room {
                clauses += String.localized(.intentDisciplineClassSuffixRoom(room))
            }
            if let teacher {
                clauses += String.localized(.intentDisciplineClassSuffixTeacher(teacher))
            }
            return clauses
        }
        switch verdict {
        case let .inClass(occurrence):
            return IntentDialog(.intentDisciplineClassDialogInClass(
                name, IntentFormat.spokenTime(occurrence.endOrEstimate), details(occurrence)
            ))
        case let .today(occurrence):
            return IntentDialog(.intentDisciplineClassDialogToday(
                name, IntentFormat.spokenTime(occurrence.start), details(occurrence)
            ))
        case let .tomorrow(occurrence):
            return IntentDialog(.intentDisciplineClassDialogTomorrow(
                name, IntentFormat.spokenTime(occurrence.start), details(occurrence)
            ))
        case let .weekday(occurrence):
            let weekday = occurrence.start.formatted(.dateTime.weekday(.wide).locale(.autoupdatingCurrent))
            return IntentDialog(.intentDisciplineClassDialogWeekday(
                name, weekday, IntentFormat.spokenTime(occurrence.start), details(occurrence)
            ))
        case .none:
            if let teacher {
                return IntentDialog(.intentDisciplineClassDialogNoneTeacher(name, teacher))
            }
            return IntentDialog(.intentDisciplineClassDialogNone(name))
        }
    }

    /// Answers "qual minha nota em X?" from the mirrored detail feed —
    /// the released grades, then the mean the detail screen shows (the
    /// closing mean once upstream closed the discipline, the partial mean
    /// otherwise).
    public static func disciplineGrades(semesterId: String, disciplineId: String) async -> DisciplineGradesAnswer {
        @ComposableArchitecture.Dependency(\.disciplinesRepository) var disciplinesRepository
        @ComposableArchitecture.Dependency(\.date) var date

        guard let detail = try? await disciplinesRepository.detailCached(semesterId, disciplineId, date.now) else {
            log.info("discipline-grades answered state=stale")
            return DisciplineGradesAnswer(dialog: IntentDialog(.intentFinalExamDialogStale), card: nil)
        }
        let summary = gradesSummary(detail: detail)
        guard !summary.released.isEmpty, let average = summary.average else {
            log.info("discipline-grades answered state=noGrades")
            return DisciplineGradesAnswer(dialog: IntentDialog(.intentFinalExamDialogNoGrades(detail.name)), card: nil)
        }

        var items = summary.released.prefix(spokenGradeCap).map {
            String.localized(.intentGradesItem(formatGrade($0.value), $0.title))
        }
        if summary.released.count > spokenGradeCap {
            items.append(String.localized(.intentTodayDialogAndMore(summary.released.count - spokenGradeCap)))
        }
        let list = items.formatted(.list(type: .and, width: .standard).locale(.autoupdatingCurrent))
        let dialog = summary.closed
            ? IntentDialog(.intentGradesDialogClosed(detail.name, list, formatGrade(average)))
            : IntentDialog(.intentGradesDialogList(detail.name, list, formatGrade(average)))

        log.info("discipline-grades answered state=\(summary.closed ? "closed" : "partial") released=\(summary.released.count)")
        return DisciplineGradesAnswer(
            dialog: dialog,
            card: IntentGradesCardView(
                code: detail.code,
                name: detail.name,
                grades: detail.grades(forGroup: nil),
                finalExam: detail.finalExam,
                average: average,
                closed: summary.closed
            )
        )
    }

    /// What the grades answer speaks: every released grade (the Prova
    /// Final row included once it has a value) and the mean the detail
    /// screen leads with. Pure over the detail.
    static func gradesSummary(detail: DisciplineDetail) -> GradesSummary {
        let grades = detail.grades(forGroup: nil)
        var released = grades.compactMap { grade in
            grade.value.map { GradesSummary.Item(title: grade.title, value: $0) }
        }
        if let finalExam = detail.finalExam, let value = finalExam.value {
            released.append(GradesSummary.Item(title: finalExam.title, value: value))
        }
        // Upstream closed the discipline: its posted mean is the answer.
        // While the finals are still pending `finalGrade` is a live mean
        // (never a verdict), so only `approved` marks the closed state.
        let closed = detail.approved != nil && detail.finalGrade != nil
        return GradesSummary(
            released: released,
            average: closed ? detail.finalGrade : DisciplineDetail.partialAverage(of: grades),
            closed: closed
        )
    }
}

/// The discipline's next class, mapped 1:1 onto the intent's dialog cases.
enum DisciplineClassVerdict: Equatable, Sendable {
    case inClass(ClassOccurrence)
    case today(ClassOccurrence)
    case tomorrow(ClassOccurrence)
    case weekday(ClassOccurrence)
    /// Nothing within the nine-day horizon — the pattern has no session
    /// for this discipline (or the semester ran out).
    case none

    var occurrence: ClassOccurrence? {
        switch self {
        case let .inClass(occurrence), let .today(occurrence),
             let .tomorrow(occurrence), let .weekday(occurrence):
            occurrence
        case .none:
            nil
        }
    }

    /// Log-safe outcome kind.
    var kindLabel: String {
        switch self {
        case .inClass: "inClass"
        case .today: "today"
        case .tomorrow: "tomorrow"
        case .weekday: "weekday"
        case .none: "none"
        }
    }
}

struct GradesSummary: Equatable, Sendable {
    struct Item: Equatable, Sendable {
        var title: String
        var value: Double
    }

    /// Released grades in section order, the Prova Final row last.
    var released: [Item]
    /// Nil until at least one grade is released.
    var average: Double?
    /// Upstream posted the closing mean and verdict.
    var closed: Bool
}
