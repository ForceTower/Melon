import Foundation
import WidgetKit

/// Timeline provider for the "Próxima aula" widget.
///
/// Emits one entry per state-change boundary — class start, end, end−30
/// handoff to the next class, and the next-day rollover — so WidgetKit
/// transitions between upcoming/inClass/dayDone at exactly the right
/// moments. Inside each entry, `Text(timerInterval:)` and
/// `ProgressView(timerInterval:)` carry the smooth per-second countdown
/// and bar fill without needing extra entries.
///
/// ~5–15 entries per submission, ~10–20 submissions per day driven by
/// the `.atEnd` policy plus the host publisher's explicit
/// `reloadTimelines` on KMP flow changes — well under WidgetKit's daily
/// reload budget.
struct NextClassProvider: TimelineProvider {
    func placeholder(in context: Context) -> NextClassEntry {
        .placeholder
    }

    func getSnapshot(in context: Context, completion: @escaping (NextClassEntry) -> Void) {
        if context.isPreview {
            completion(.placeholder)
            return
        }
        let now = Date()
        guard let snapshot = WidgetSnapshot.load() else {
            completion(.placeholder)
            return
        }
        completion(snapshot.renderEntry(at: now))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<NextClassEntry>) -> Void) {
        let now = WidgetSnapshot.resolvedNow()
        guard let snapshot = WidgetSnapshot.load() else {
            let next = Calendar.current.date(byAdding: .minute, value: 15, to: Date()) ?? Date()
            completion(Timeline(entries: [.placeholder], policy: .after(next)))
            return
        }

        #if DEBUG
        // Debug clock override is on — WidgetKit picks entries by wall
        // clock, so multi-entry timelines anchored on fake times produce
        // confusing results. Emit a single static entry rendered against
        // the override.
        if WidgetSnapshot.isDebugClockOverridden {
            let next = Calendar.current.date(byAdding: .minute, value: 5, to: Date()) ?? Date()
            completion(Timeline(entries: [snapshot.renderEntry(at: now)], policy: .after(next)))
            return
        }
        #endif

        let entries = Self.entries(snapshot: snapshot, now: now)
        completion(Timeline(entries: entries, policy: .atEnd))
    }

    /// Generates one entry at `now` plus one entry at each future state
    /// boundary today (class start, class end, class end − 30 min handoff)
    /// plus the next-day rollover so dayDone copy refreshes overnight.
    /// Each entry is rendered against its own `.date` so countdown values
    /// land correctly at the start of every transition.
    private static func entries(snapshot: WidgetSnapshot, now: Date) -> [NextClassEntry] {
        let calendar = Calendar.current
        let startOfToday = calendar.startOfDay(for: now)

        var points: [Date] = [now]

        for c in snapshot.today {
            if let start = minutesToDate(c.startTime, dayStart: startOfToday, calendar: calendar),
               start > now {
                points.append(start)
            }
            if let end = c.endTime,
               let endDate = minutesToDate(end, dayStart: startOfToday, calendar: calendar) {
                if endDate > now {
                    points.append(endDate)
                }
                if let handoff = calendar.date(byAdding: .minute, value: -30, to: endDate),
                   handoff > now {
                    points.append(handoff)
                }
            }
        }

        let tomorrow = calendar.date(byAdding: .day, value: 1, to: startOfToday) ?? startOfToday
        points.append(tomorrow)

        let unique = Array(Set(points.map { $0.timeIntervalSince1970.rounded() }))
            .sorted()
            .map { Date(timeIntervalSince1970: $0) }

        return unique.map { snapshot.renderEntry(at: $0) }
    }

    private static func minutesToDate(
        _ hhmm: String,
        dayStart: Date,
        calendar: Calendar
    ) -> Date? {
        let parts = hhmm.split(separator: ":")
        guard parts.count >= 2, let h = Int(parts[0]), let m = Int(parts[1]) else { return nil }
        return calendar.date(byAdding: .minute, value: h * 60 + m, to: dayStart)
    }
}
