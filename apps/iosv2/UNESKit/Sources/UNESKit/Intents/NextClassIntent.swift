import AppIntents
import SwiftUI

private let log = Log.scoped("AppIntents")

/// "Qual minha próxima aula?" — answers from the widget snapshot with the
/// exact status logic the Next Class widget uses, so Siri and the widget
/// never disagree on what "next" means. Runs headless; never opens the app.
public struct NextClassIntent: AppIntent {
    public static let title: LocalizedStringResource = "intent.nextClass.title"

    public init() {}

    public func perform() async throws -> some IntentResult & ProvidesDialog & ShowsSnippetView {
        guard let schedule = WidgetSnapshotStore.load() else {
            log.info("next-class answered state=signedOut")
            return .result(dialog: IntentDialog(.intentDialogSignedOut), view: EmptyView())
        }
        let now = Date()
        let calendar = Calendar.current
        let occurrences = schedule.occurrences(from: now, days: 9, calendar: calendar)
        let (status, _) = NextClassStatus.compute(at: now, occurrences: occurrences, calendar: calendar)

        switch status {
        case let .inClass(occurrence):
            log.info("next-class answered state=inClass")
            return .result(
                dialog: Self.dialog(inClass: occurrence),
                view: IntentClassCardView(occurrence: occurrence)
            )
        case let .upcoming(occurrence):
            log.info("next-class answered state=upcoming")
            return .result(
                dialog: Self.dialog(upcoming: occurrence, now: now, calendar: calendar),
                view: IntentClassCardView(occurrence: occurrence)
            )
        case let .dayDone(_, next?):
            log.info("next-class answered state=dayDone")
            return .result(
                dialog: Self.dialog(upcoming: next, now: now, calendar: calendar),
                view: IntentClassCardView(occurrence: next)
            )
        case .dayDone(_, next: nil):
            // Weekly patterns repeat within the 9-day horizon, so an empty
            // one means the semester has run out.
            log.info("next-class answered state=none")
            return .result(dialog: IntentDialog(.intentNextClassDialogNone), view: EmptyView())
        case .signedOut:
            log.info("next-class answered state=signedOut")
            return .result(dialog: IntentDialog(.intentDialogSignedOut), view: EmptyView())
        }
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
