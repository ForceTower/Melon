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

/// Spoken/date strings for intent dialogs — locale-aware, unlike the
/// snapshot's fixed "HH:mm" labels that the snippets keep for widget parity.
enum IntentFormat {
    /// "10:50" (pt-BR) / "10:50 AM" (en) — how Siri should say a time.
    static func spokenTime(_ date: Date) -> String {
        date.formatted(Date.FormatStyle(date: .omitted, time: .shortened, locale: .autoupdatingCurrent))
    }
}
