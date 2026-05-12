import Foundation
import WidgetKit

/// Timeline provider for the "Próxima aula" widget.
///
/// Loads `WidgetSnapshot` from the App Group container (written by the
/// host app via `WidgetSnapshotPublisher`) and generates a multi-entry
/// timeline anchored on today's class boundaries. State + countdowns are
/// recomputed at every transition point, so WidgetKit can flip from
/// upcoming → inClass → dayDone without another reload — until the host
/// writes a fresher snapshot or the safety-net `.after(...)` policy fires.
struct NextClassProvider: TimelineProvider {
    func placeholder(in context: Context) -> NextClassEntry {
        .placeholder
    }

    func getSnapshot(in context: Context, completion: @escaping (NextClassEntry) -> Void) {
        if context.isPreview {
            completion(.placeholder)
            return
        }
        let now = WidgetSnapshot.resolvedNow()
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
        // Debug clock override is on — WidgetKit picks entries by wall clock,
        // so multi-entry timelines anchored on fake times produce confusing
        // results. Emit a single static entry rendered against the override.
        if WidgetSnapshot.isDebugClockOverridden {
            let next = Calendar.current.date(byAdding: .minute, value: 5, to: Date()) ?? Date()
            completion(Timeline(entries: [snapshot.renderEntry(at: now)], policy: .after(next)))
            return
        }
        #endif

        let transitions = Self.transitionPoints(snapshot: snapshot, now: now)
        let entries = transitions.map { snapshot.renderEntry(at: $0) }

        // Safety-net reload: ~02:00 tomorrow, well past any reasonable class
        // boundary. The publisher will write a fresher snapshot well before
        // this, but if the app stays backgrounded WidgetKit still refreshes
        // overnight so the dayDone copy stays correct.
        let calendar = Calendar.current
        let tomorrow = calendar.startOfDay(for: now.addingTimeInterval(24 * 3600))
        let safetyNet = calendar.date(byAdding: .hour, value: 2, to: tomorrow) ?? tomorrow

        completion(Timeline(entries: entries, policy: .after(safetyNet)))
    }

    /// Generates the time points where the rendered entry would change.
    /// Always includes `now` plus every future class boundary on today and
    /// the start of tomorrow (so dayDone copy flips at midnight).
    private static func transitionPoints(snapshot: WidgetSnapshot, now: Date) -> [Date] {
        var points: [Date] = [now]
        let calendar = Calendar.current
        let startOfToday = calendar.startOfDay(for: now)

        for c in snapshot.today {
            if let start = Self.minutesToDate(c.startTime, dayStart: startOfToday, calendar: calendar),
               start > now {
                points.append(start)
            }
            if let end = c.endTime,
               let endDate = Self.minutesToDate(end, dayStart: startOfToday, calendar: calendar) {
                if endDate > now {
                    points.append(endDate)
                }
                // 30 minutes before each class ends, renderEntry hands off to
                // the next class if there is one (see inClassSwitchThreshold
                // in WidgetSnapshot.renderEntry). Surface that boundary so
                // WidgetKit picks up the entry at the right moment.
                if let handoff = calendar.date(byAdding: .minute, value: -30, to: endDate),
                   handoff > now {
                    points.append(handoff)
                }
            }
        }

        let tomorrow = calendar.date(byAdding: .day, value: 1, to: startOfToday) ?? startOfToday
        points.append(tomorrow)

        // WidgetKit accepts up to ~96 entries per kind; we'll easily stay
        // well under that even on a packed day. Sort + dedupe by minute so
        // back-to-back classes don't produce duplicate transitions.
        let unique = Array(Set(points.map { $0.timeIntervalSince1970.rounded() }))
            .sorted()
            .map { Date(timeIntervalSince1970: $0) }
        return unique
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
