#if os(iOS)
import Foundation
import WidgetKit

/// What the "Próxima aula" widget shows at one timeline moment.
enum NextClassStatus: Equatable {
    /// Before a class: the mesh hero counting down (absolute time farther out).
    case upcoming(ClassOccurrence)
    /// While a class runs: live progress toward its end.
    case inClass(ClassOccurrence)
    /// After (or without) today's classes: the calm theme-aware card.
    case dayDone(completed: Int, next: ClassOccurrence?)
    /// Nothing published — signed out or before the first sync.
    case signedOut
}

struct NextClassEntry: TimelineEntry {
    var date: Date
    var status: NextClassStatus
    /// Today's occurrences, for the Large "Seu dia" strip.
    var today: [ClassOccurrence] = []
}

struct NextClassProvider: TimelineProvider {
    func placeholder(in context: Context) -> NextClassEntry {
        .preview
    }

    func getSnapshot(in context: Context, completion: @escaping (NextClassEntry) -> Void) {
        let timeline = Self.timeline(now: Date(), schedule: WidgetSnapshotStore.load())
        if let entry = timeline.entries.first, !context.isPreview || entry.isShowable {
            completion(entry)
        } else {
            completion(.preview)
        }
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<NextClassEntry>) -> Void) {
        completion(Self.timeline(now: Date(), schedule: WidgetSnapshotStore.load()))
    }

    /// Entries cover from `now` until the end of the current (or next) class;
    /// `.atEnd` then rebuilds around the following one. Minute entries run
    /// only through the final stretch before a class, where the hero counts
    /// down; everything faster (seconds, progress bars) is self-updating text.
    static func timeline(
        now: Date,
        schedule: WidgetScheduleSnapshot?,
        calendar: Calendar = .current
    ) -> Timeline<NextClassEntry> {
        guard let schedule, !schedule.sessions.isEmpty else {
            let status: NextClassStatus = schedule == nil ? .signedOut : .dayDone(completed: 0, next: nil)
            return Timeline(
                entries: [NextClassEntry(date: now, status: status)],
                policy: .after(now.addingTimeInterval(6 * 3600))
            )
        }

        let occurrences = schedule.occurrences(from: now, days: 9, calendar: calendar)
        // The class bounding this timeline window — in progress or next up.
        guard let anchor = occurrences.first(where: { now < $0.endOrEstimate }) else {
            // Weekly patterns repeat within 7 days, so an empty horizon means
            // the semester has run out.
            return Timeline(
                entries: [entry(at: now, occurrences: occurrences, calendar: calendar)],
                policy: .after(now.addingTimeInterval(6 * 3600))
            )
        }

        var moments: Set<Date> = [now]
        let windowEnd = anchor.endOrEstimate

        // Midnights flip "hoje"/"amanhã" wording and roll the day-done card over.
        var day = calendar.startOfDay(for: now)
        while let midnight = calendar.date(byAdding: .day, value: 1, to: day), midnight <= windowEnd {
            if midnight > now { moments.insert(midnight) }
            day = midnight
        }

        if anchor.start > now {
            moments.insert(anchor.start)
            // The countdown band: one entry per minute over the final stretch.
            let bandStart = anchor.start.addingTimeInterval(-Self.countdownBand)
            if bandStart > now { moments.insert(bandStart) }
            for minute in 1...Int(Self.countdownBand / 60) {
                let tick = anchor.start.addingTimeInterval(TimeInterval(-60 * minute))
                guard tick > now else { break }
                moments.insert(tick)
            }
        }
        moments.insert(windowEnd)

        let entries = moments.sorted().map { entry(at: $0, occurrences: occurrences, calendar: calendar) }
        return Timeline(entries: entries, policy: .atEnd)
    }

    /// How close a class must be before the hero switches from absolute time
    /// to the live minute countdown ("1h10" and below).
    static let countdownBand: TimeInterval = 70 * 60

    private static func entry(at date: Date, occurrences: [ClassOccurrence], calendar: Calendar) -> NextClassEntry {
        let today = occurrences.filter { calendar.isDate($0.start, inSameDayAs: date) }
        let status: NextClassStatus
        if let current = today.first(where: { $0.start <= date && date < $0.endOrEstimate }) {
            status = .inClass(current)
        } else if let nextToday = today.first(where: { $0.start > date }) {
            status = .upcoming(nextToday)
        } else {
            status = .dayDone(completed: today.count, next: occurrences.first { $0.start > date })
        }
        return NextClassEntry(date: date, status: status, today: today)
    }
}

extension NextClassEntry {
    /// Whether the entry carries real content worth putting in the gallery
    /// preview — the placeholder mock reads better than a sign-in prompt.
    var isShowable: Bool {
        switch status {
        case .signedOut: false
        case .dayDone(_, let next): next != nil
        case .upcoming, .inClass: true
        }
    }
}

// MARK: - Gallery / preview fixtures (the design's mock day)

extension ClassOccurrence {
    static func mock(
        classId: String,
        code: String,
        title: String,
        room: String?,
        teacher: String? = nil,
        topic: String? = nil,
        startsIn: TimeInterval,
        duration: TimeInterval = 100 * 60,
        now: Date = .now
    ) -> ClassOccurrence {
        let start = now.addingTimeInterval(startsIn)
        let minutes = Calendar.current.component(.hour, from: start) * 60
            + Calendar.current.component(.minute, from: start)
        let label = { (m: Int) in String(format: "%02d:%02d", m / 60, m % 60) }
        return ClassOccurrence(
            classId: classId,
            code: code,
            title: title,
            room: room,
            teacherName: teacher,
            topic: topic,
            colorIndex: 1,
            start: start,
            end: start.addingTimeInterval(duration),
            startTime: label(minutes),
            endTime: label(minutes + Int(duration / 60))
        )
    }

    static func previewDay(now: Date = .now) -> [ClassOccurrence] {
        [
            .mock(classId: "c1", code: "ALGI", title: "Algoritmos I", room: "LC-03", startsIn: -140 * 60, now: now),
            .mock(
                classId: "c2",
                code: "CALC II",
                title: "Cálculo II",
                room: "MT-14",
                teacher: "Adriana Matos",
                topic: "Integrais por partes",
                startsIn: 39 * 60,
                now: now
            ),
            .mock(classId: "c3", code: "LPOO", title: "POO", room: "LC-01", startsIn: 4 * 3600, now: now),
            .mock(classId: "c4", code: "FIS2", title: "Física II", room: "PV-22", startsIn: 6 * 3600 + 20 * 60, now: now),
        ]
    }

    static func preview(now: Date = .now) -> ClassOccurrence {
        previewDay(now: now)[1]
    }
}

extension NextClassEntry {
    static var preview: NextClassEntry {
        let now = Date.now
        return NextClassEntry(
            date: now,
            status: .upcoming(.preview(now: now)),
            today: ClassOccurrence.previewDay(now: now)
        )
    }

    static var previewInClass: NextClassEntry {
        let now = Date.now
        let current = ClassOccurrence.mock(
            classId: "c2",
            code: "CALC II",
            title: "Cálculo II",
            room: "MT-14",
            teacher: "Adriana Matos",
            topic: "Integrais por partes",
            startsIn: -62 * 60,
            now: now
        )
        var today = ClassOccurrence.previewDay(now: now)
        today[1] = current
        return NextClassEntry(date: now, status: .inClass(current), today: today)
    }

    static var previewDayDone: NextClassEntry {
        let now = Date.now
        let next = ClassOccurrence.mock(
            classId: "c1",
            code: "ALGI",
            title: "Algoritmos I",
            room: "Lab LC-03",
            startsIn: 14 * 3600,
            now: now
        )
        return NextClassEntry(date: now, status: .dayDone(completed: 4, next: next))
    }
}
#endif
