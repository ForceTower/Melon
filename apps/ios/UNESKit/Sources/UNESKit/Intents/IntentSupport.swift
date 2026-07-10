import AppIntents
import ComposableArchitecture
import Foundation

private let log = Log.scoped("AppIntents")

/// The tab-routing currency between the app-target intents and the package —
/// `AppFeature.Tab` itself stays internal.
public enum IntentTab: String, CaseIterable, Sendable {
    case home, schedule, classes, messages, me

    var appTab: AppFeature.Tab {
        switch self {
        case .home: .home
        case .schedule: .schedule
        case .classes: .classes
        case .messages: .messages
        case .me: .me
        }
    }
}

/// Everything the Siri intents need from the package. The intent structs
/// themselves live in the UNES app target: the system fails to localize App
/// Intents display strings defined in SPM packages (raw keys show in the
/// Shortcuts app), so the thin shells sit next to the app's catalog and
/// delegate here. Dialogs are built in-process, so they resolve from this
/// module's catalog just fine.
public enum IntentSupport {
    public struct NextClassAnswer {
        public let dialog: IntentDialog
        public let card: IntentClassCardView?
    }

    public struct TodayAnswer {
        public let dialog: IntentDialog
        public let list: IntentDayListView?
    }

    public struct ScoreAnswer {
        public let dialog: IntentDialog
        public let card: IntentScoreCardView?
    }

    public struct UnreadMessagesAnswer {
        public let dialog: IntentDialog
        public let card: IntentUnreadListView?
    }

    public struct FinalExamAnswer {
        public let dialog: IntentDialog
        public let card: IntentVerdictCardView?
    }

    /// How many classes the spoken "today" summary names before "e mais N".
    private static let spokenCap = 3

    /// Answers from the widget snapshot with the exact status logic the
    /// Next Class widget uses, so Siri and the widget never disagree on
    /// what "next" means.
    public static func nextClass(now: Date = Date(), calendar: Calendar = .current) -> NextClassAnswer {
        guard let schedule = WidgetSnapshotStore.load() else {
            log.info("next-class answered state=signedOut")
            return NextClassAnswer(dialog: IntentDialog(.intentDialogSignedOut), card: nil)
        }
        let occurrences = schedule.occurrences(from: now, days: 9, calendar: calendar)
        let (status, _) = NextClassStatus.compute(at: now, occurrences: occurrences, calendar: calendar)

        switch status {
        case let .inClass(occurrence):
            log.info("next-class answered state=inClass")
            return NextClassAnswer(
                dialog: dialog(inClass: occurrence),
                card: IntentClassCardView(occurrence: occurrence)
            )
        case let .upcoming(occurrence):
            log.info("next-class answered state=upcoming")
            return NextClassAnswer(
                dialog: dialog(upcoming: occurrence, now: now, calendar: calendar),
                card: IntentClassCardView(occurrence: occurrence)
            )
        case let .dayDone(_, next?):
            log.info("next-class answered state=dayDone")
            return NextClassAnswer(
                dialog: dialog(upcoming: next, now: now, calendar: calendar),
                card: IntentClassCardView(occurrence: next)
            )
        case .dayDone(_, next: nil):
            // Weekly patterns repeat within the 9-day horizon, so an empty
            // one means the semester has run out.
            log.info("next-class answered state=none")
            return NextClassAnswer(dialog: IntentDialog(.intentNextClassDialogNone), card: nil)
        case .signedOut:
            log.info("next-class answered state=signedOut")
            return NextClassAnswer(dialog: IntentDialog(.intentDialogSignedOut), card: nil)
        }
    }

    /// The day's map: a spoken summary capped at three classes, a snippet
    /// listing all of them (past included, like the widget's "Seu dia").
    public static func today(now: Date = Date(), calendar: Calendar = .current) -> TodayAnswer {
        guard let schedule = WidgetSnapshotStore.load() else {
            log.info("today answered state=signedOut")
            return TodayAnswer(dialog: IntentDialog(.intentDialogSignedOut), list: nil)
        }
        let occurrences = schedule.occurrences(from: now, days: 1, calendar: calendar)
        let today = NextClassStatus.compute(at: now, occurrences: occurrences, calendar: calendar).today

        guard !today.isEmpty else {
            log.info("today answered classes=0")
            return TodayAnswer(dialog: IntentDialog(.intentTodayDialogNone), list: nil)
        }

        var items = today.prefix(spokenCap).map {
            "\($0.title) \(String.localized(.widgetAtTime(IntentFormat.spokenTime($0.start))))"
        }
        if today.count > spokenCap {
            items.append(String.localized(.intentTodayDialogAndMore(today.count - spokenCap)))
        }
        let list = items.formatted(.list(type: .and, width: .standard).locale(.autoupdatingCurrent))

        log.info("today answered classes=\(today.count)")
        return TodayAnswer(
            dialog: IntentDialog(.intentTodayDialogClasses(today.count, list)),
            list: IntentDayListView(occurrences: today, now: now)
        )
    }

    /// Answers "qual meu score?" from the mirrored Eu snapshot — the exact
    /// value the Me tab shows, truncation included.
    public static func score() async -> ScoreAnswer {
        @ComposableArchitecture.Dependency(\.meRepository) var meRepository
        @ComposableArchitecture.Dependency(\.date) var date

        guard let cached = try? await meRepository.cached(now: date.now) else {
            log.info("score answered state=signedOut")
            return ScoreAnswer(dialog: IntentDialog(.intentDialogSignedOut), card: nil)
        }
        guard let coefficient = cached.overview.coefficient else {
            log.info("score answered state=noCoefficient")
            return ScoreAnswer(dialog: IntentDialog(.intentScoreDialogNone), card: nil)
        }
        log.info("score answered state=value")
        return ScoreAnswer(
            dialog: IntentDialog(.intentScoreDialogValue(formatGrade(coefficient.value))),
            card: IntentScoreCardView(coefficient: coefficient)
        )
    }

    /// Answers "tenho mensagem nova?" from the mirrored inbox; the unread
    /// count already respects the local read overlay.
    public static func unreadMessages() async -> UnreadMessagesAnswer {
        @ComposableArchitecture.Dependency(\.messagesRepository) var messagesRepository
        @ComposableArchitecture.Dependency(\.sessionStore) var sessionStore
        @ComposableArchitecture.Dependency(\.intentRouter) var intentRouter
        @ComposableArchitecture.Dependency(\.date) var date

        guard let overview = try? await messagesRepository.cached(now: date.now) else {
            log.info("unread answered state=signedOut")
            return UnreadMessagesAnswer(dialog: IntentDialog(.intentDialogSignedOut), card: nil)
        }
        let unread = overview.messages.filter(\.unread)
        guard !unread.isEmpty else {
            log.info("unread answered count=0")
            return UnreadMessagesAnswer(dialog: IntentDialog(.intentUnreadDialogNone), card: nil)
        }
        // A snippet tap can only open the app, so pre-post the Mensagens tab
        // through the router — the tap-through (or the next open) lands on
        // the inbox, with `openTab`'s buffering semantics.
        if sessionStore.current() != nil {
            intentRouter.open(.tab(.messages))
        }
        log.info("unread answered count=\(unread.count)")
        return UnreadMessagesAnswer(
            dialog: IntentDialog(.intentUnreadDialogCount(unread.count)),
            card: IntentUnreadListView(items: Array(unread.prefix(3)))
        )
    }

    /// Answers "quanto preciso na Prova Final de X?" — and every other
    /// standing — from the mirrored detail feed, with the exact rules the
    /// detail screen and FinalCountdown render.
    public static func finalExam(semesterId: String, disciplineId: String) async -> FinalExamAnswer {
        @ComposableArchitecture.Dependency(\.disciplinesRepository) var disciplinesRepository
        @ComposableArchitecture.Dependency(\.date) var date

        let detail = try? await disciplinesRepository.detailCached(
            semesterId: semesterId,
            disciplineId: disciplineId,
            now: date.now
        )
        let verdict = finalExamVerdict(detail: detail)
        log.info("final-exam answered state=\(verdict.kindLabel)")

        var card: IntentVerdictCardView?
        if let detail {
            switch verdict {
            case .stale, .noGrades: break
            default: card = IntentVerdictCardView(code: detail.code, name: detail.name, verdict: verdict)
            }
        }
        return FinalExamAnswer(dialog: dialog(finalExam: verdict, discipline: detail?.name ?? ""), card: card)
    }

    /// The Prova Final standing, computed from the same rules the detail
    /// screen renders — Siri must never disagree with the app. Values stay
    /// raw; formatting rounds at the edge (floor for averages, ceil for
    /// needed grades).
    static func finalExamVerdict(detail: DisciplineDetail?) -> FinalExamVerdict {
        guard let detail else { return .stale }
        let grades = detail.grades(forGroup: nil)
        let average = DisciplineDetail.partialAverage(of: grades)

        switch detail.status {
        case .approved:
            return .approved(average: detail.finalGrade ?? average)
        case .failed:
            return .lost(average: detail.finalGrade ?? average)
        case .finals:
            guard let average else { return .lost(average: nil) }
            // FinalCountdown's exact sequence: truncate the mean first, then
            // round the requirement up — 0,6·m + 0,4·F ≥ 5.
            let needed = DisciplineRules.ceilToTenth(
                DisciplineRules.neededFinal(avg: DisciplineRules.floorToTenth(average))
            )
            guard needed <= 10 else { return .lost(average: average) }
            return .finals(average: average, needed: needed)
        case .noGrades:
            return .noGrades
        case .lowGrade, .ongoing:
            guard let average else { return .noGrades }
            guard let needed = DisciplineDetail.neededOnPending(of: grades) else {
                return .partial(average: average)
            }
            // The detail hero's reachability rule: the raw value against 10.
            guard needed <= 10 else { return .finalsPath(average: average) }
            return .directClose(average: average, needed: needed, next: nextScheduledEvaluation(in: grades))
        }
    }

    private static func nextScheduledEvaluation(in grades: [DisciplineDetailGrade]) -> FinalExamVerdict.NextEvaluation? {
        grades
            .compactMap { grade -> (title: String, stamp: String)? in
                // `daysUntil` is non-nil exactly for scheduled, pending,
                // not-yet-passed evaluations.
                guard grade.daysUntil != nil, let stamp = grade.date else { return nil }
                return (grade.title, stamp)
            }
            .min { $0.stamp < $1.stamp }
            .map { FinalExamVerdict.NextEvaluation(title: $0.title, dateStamp: $0.stamp) }
    }

    private static func dialog(finalExam verdict: FinalExamVerdict, discipline name: String) -> IntentDialog {
        switch verdict {
        case .stale:
            IntentDialog(.intentFinalExamDialogStale)
        case .approved:
            IntentDialog(.intentFinalExamDialogApproved(name))
        case let .finals(_, needed):
            IntentDialog(.intentFinalExamDialogNeeded(DisciplinesFormat.neededGrade(needed), name))
        case .lost:
            IntentDialog(.intentFinalExamDialogImpossible(name))
        case let .directClose(_, needed, next):
            if let next, let day = IntentFormat.spokenDay(next.dateStamp) {
                IntentDialog(.intentFinalExamDialogPendingDirectNext(
                    DisciplinesFormat.neededGrade(needed), next.title, day
                ))
            } else {
                IntentDialog(.intentFinalExamDialogPendingDirect(DisciplinesFormat.neededGrade(needed)))
            }
        case .finalsPath:
            IntentDialog(.intentFinalExamDialogPendingFinalOnly)
        case let .partial(average):
            IntentDialog(.intentFinalExamDialogPartial(name, formatGrade(average)))
        case .noGrades:
            IntentDialog(.intentFinalExamDialogNoGrades(name))
        }
    }

    /// Posts the tab through `IntentRouter`; `AppFeature` consumes it once
    /// the connected shell renders.
    public static func openTab(_ tab: IntentTab) {
        // Module-qualified: AppIntents aliases its own `Dependency` wrapper,
        // and a bare `@Dependency` in a file importing it is ambiguous.
        @ComposableArchitecture.Dependency(\.sessionStore) var sessionStore
        @ComposableArchitecture.Dependency(\.intentRouter) var intentRouter

        // Signed out, nothing subscribes — a buffered route would replay
        // after a later login, so don't post at all. The app still
        // foregrounds onto onboarding, which is correct.
        guard sessionStore.current() != nil else {
            log.info("open-tab route ignored signed-out")
            return
        }
        intentRouter.open(.tab(tab.appTab))
        log.info("open-tab route posted tab=\(tab.rawValue)")
    }

    /// A Spotlight result tap: parses the entity identifier and posts the
    /// route; `AppFeature` resolves it against the mirror at delivery time.
    public static func openEntity(identifier: String) {
        @ComposableArchitecture.Dependency(\.sessionStore) var sessionStore
        @ComposableArchitecture.Dependency(\.intentRouter) var intentRouter

        guard let route = SpotlightEntityID.parse(identifier) else {
            log.warn("entity route unparseable")
            return
        }
        // Signed out, nothing subscribes — a buffered route would replay
        // after a later login, so don't post at all. The app still opens
        // onto onboarding, which is correct.
        guard sessionStore.current() != nil else {
            log.info("entity route ignored signed-out")
            return
        }
        intentRouter.open(route)
        log.info("entity route posted kind=\(route.kindLabel)")
    }

    private static func dialog(inClass occurrence: ClassOccurrence) -> IntentDialog {
        let end = IntentFormat.spokenTime(occurrence.endOrEstimate)
        if let room = occurrence.room {
            return IntentDialog(.intentNextClassDialogInClass(occurrence.title, room, end))
        }
        return IntentDialog(.intentNextClassDialogInClassNoRoom(occurrence.title, end))
    }

    private static func dialog(upcoming occurrence: ClassOccurrence, now: Date, calendar: Calendar) -> IntentDialog {
        let time = IntentFormat.spokenTime(occurrence.start)
        if calendar.isDate(occurrence.start, inSameDayAs: now) {
            if let room = occurrence.room {
                return IntentDialog(.intentNextClassDialogToday(occurrence.title, time, room))
            }
            return IntentDialog(.intentNextClassDialogTodayNoRoom(occurrence.title, time))
        }
        if let tomorrow = calendar.date(byAdding: .day, value: 1, to: now),
           calendar.isDate(occurrence.start, inSameDayAs: tomorrow) {
            return IntentDialog(.intentNextClassDialogTomorrow(occurrence.title, time))
        }
        // Apposition ("… é segunda-feira: …") dodges the pt-BR preposition
        // gender ("na segunda" vs "no sábado") with one format string.
        let weekday = occurrence.start.formatted(.dateTime.weekday(.wide).locale(.autoupdatingCurrent))
        return IntentDialog(.intentNextClassDialogWeekday(weekday, occurrence.title, time))
    }
}

/// One discipline's Prova Final standing, mapped 1:1 onto the intent's
/// dialog matrix. Averages and needed grades are raw values — the dialog
/// and card format them (floor for averages, ceil for needed grades).
enum FinalExamVerdict: Equatable, Sendable {
    struct NextEvaluation: Equatable, Sendable {
        var title: String
        /// yyyy-MM-dd, always set — only scheduled evaluations qualify.
        var dateStamp: String
    }

    /// The discipline isn't in the current mirror (stale Siri entity).
    case stale
    case approved(average: Double?)
    /// In the Prova Final with a reachable needed grade.
    case finals(average: Double, needed: Double)
    /// Failed, or the mean is out of reach even with a 10.
    case lost(average: Double?)
    /// Can still close at 7 on the pending evaluations.
    case directClose(average: Double, needed: Double, next: NextEvaluation?)
    /// The pending evaluations can't reach 7 — the Prova Final is the path.
    case finalsPath(average: Double)
    /// Grades released but nothing pending to solve for.
    case partial(average: Double)
    case noGrades

    /// Log-safe outcome kind — never the values.
    var kindLabel: String {
        switch self {
        case .stale: "stale"
        case .approved: "approved"
        case .finals: "finals"
        case .lost: "lost"
        case .directClose: "directClose"
        case .finalsPath: "finalsPath"
        case .partial: "partial"
        case .noGrades: "noGrades"
        }
    }
}

/// Spoken/date strings for intent dialogs — locale-aware, unlike the
/// snapshot's fixed "HH:mm" labels that the snippets keep for widget parity.
enum IntentFormat {
    /// "10:50" (pt-BR) / "10:50 AM" (en) — how Siri should say a time.
    static func spokenTime(_ date: Date) -> String {
        date.formatted(Date.FormatStyle(date: .omitted, time: .shortened, locale: .autoupdatingCurrent))
    }

    /// "15 de agosto" (pt-BR) / "August 15" (en) from a yyyy-MM-dd stamp —
    /// how Siri should say an evaluation date.
    static func spokenDay(_ stamp: String, calendar: Calendar = .current) -> String? {
        let parts = stamp.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3,
              let date = calendar.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2]))
        else { return nil }
        return date.formatted(.dateTime.day().month(.wide).locale(.autoupdatingCurrent))
    }
}
