import Foundation
import WidgetKit

/// Timeline provider for the "Próxima aula" widget.
///
/// Strategy: emit one entry per minute for the next `liveWindowMinutes`
/// minutes plus extra entries at class boundaries (start, end, end−30) and
/// the next-day rollover. Per-minute entries are what actually drives the
/// "EM 1H 30MIN → 1H 29MIN → …" countdown — `TimelineView(.everyMinute)`
/// inside the views is not reliably honored by the widget renderer in
/// practice. Once the timeline's last entry fires, WidgetKit re-asks the
/// provider for a fresh batch (`.atEnd`), and the host's publisher also
/// nudges a reload whenever the underlying schedule changes.
///
/// Cap: ~95 entries per submission. A 60-minute live window plus a handful
/// of boundary entries comfortably fits.
struct NextClassProvider: TimelineProvider {
    /// Length of the per-minute live-update window submitted in each
    /// timeline. WidgetKit calls back at `.atEnd` to extend it.
    private static let liveWindowMinutes = 60

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

    /// Generates the entry set the timeline ships with:
    ///   - one entry every minute for the next `liveWindowMinutes` minutes,
    ///   - one entry at each future class boundary today (start, end,
    ///     end−30 handoff) so state flips line up exactly even if those
    ///     boundaries fall outside the live window,
    ///   - one at the next-day rollover so dayDone copy refreshes overnight.
    /// Sorted and deduped to the minute.
    private static func entries(snapshot: WidgetSnapshot, now: Date) -> [NextClassEntry] {
        let calendar = Calendar.current
        let startOfToday = calendar.startOfDay(for: now)

        var points: [Date] = []

        for i in 0..<liveWindowMinutes {
            if let t = calendar.date(byAdding: .minute, value: i, to: now) {
                points.append(t)
            }
        }

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
