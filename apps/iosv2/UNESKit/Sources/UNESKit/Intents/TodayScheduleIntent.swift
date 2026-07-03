import AppIntents
import SwiftUI

private let log = Log.scoped("AppIntents")

/// "Aulas de hoje" — the day's map from the widget snapshot: a spoken
/// summary capped at three classes, a snippet listing all of them (past
/// included, like the widget's "Seu dia" strip). Runs headless.
public struct TodayScheduleIntent: AppIntent {
    public static let title: LocalizedStringResource = "intent.today.title"

    /// How many classes the spoken summary names before "e mais N".
    private static let spokenCap = 3

    public init() {}

    public func perform() async throws -> some IntentResult & ProvidesDialog & ShowsSnippetView {
        guard let schedule = WidgetSnapshotStore.load() else {
            log.info("today answered state=signedOut")
            return .result(dialog: IntentDialog(.intentDialogSignedOut), view: EmptyView())
        }
        let now = Date()
        let calendar = Calendar.current
        let occurrences = schedule.occurrences(from: now, days: 1, calendar: calendar)
        let today = NextClassStatus.compute(at: now, occurrences: occurrences, calendar: calendar).today

        guard !today.isEmpty else {
            log.info("today answered classes=0")
            return .result(dialog: IntentDialog(.intentTodayDialogNone), view: EmptyView())
        }

        var items = today.prefix(Self.spokenCap).map {
            "\($0.title) \(String.localized(.widgetAtTime(IntentFormat.spokenTime($0.start))))"
        }
        if today.count > Self.spokenCap {
            items.append(String.localized(.intentTodayDialogAndMore(today.count - Self.spokenCap)))
        }
        let list = items.formatted(.list(type: .and, width: .standard).locale(.autoupdatingCurrent))

        log.info("today answered classes=\(today.count)")
        return .result(
            dialog: IntentDialog(.intentTodayDialogClasses(today.count, list)),
            view: IntentDayListView(occurrences: today, now: now)
        )
    }
}
