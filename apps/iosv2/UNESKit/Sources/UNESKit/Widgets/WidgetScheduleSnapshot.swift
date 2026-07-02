import Foundation

private let log = Log.scoped("WidgetSnapshotStore")

/// Widget kinds registered by the UNESWidgets extension, shared so the app
/// can reload exactly the timelines it feeds.
enum UNESWidgetKind {
    static let nextClass = "NextClassWidget"
}

/// The weekly class pattern the app publishes to the shared App Group
/// container. The widget materializes concrete occurrences from the pattern
/// at timeline time, so it stays correct across days without the app opening.
/// Like the app, the pattern repeats regardless of the semester's dates —
/// `active(today:)` already falls back to the most recent semester, and the
/// widget must show whatever Home shows.
struct WidgetScheduleSnapshot: Codable, Equatable, Sendable {
    var semesterCode: String?
    var sessions: [Session] = []
    var topics: [Topic] = []

    /// One merged class block on a weekday (0 = Sunday, as upstream).
    struct Session: Codable, Equatable, Sendable {
        var classId: String
        var day: Int
        var startMinute: Int
        var endMinute: Int?
        var code: String
        var title: String
        var room: String?
        var teacherName: String?
        var colorIndex: Int
    }

    /// Subject of a lecture posted for a near-term date.
    struct Topic: Codable, Equatable, Sendable {
        var classId: String
        /// yyyy-MM-dd.
        var dayStamp: String
        var subject: String
    }
}

/// One concrete class block on a specific day, ready for a widget timeline.
struct ClassOccurrence: Equatable, Sendable {
    var classId: String
    var code: String
    var title: String
    var room: String?
    var teacherName: String?
    var topic: String?
    var colorIndex: Int
    var start: Date
    var end: Date?
    /// "HH:mm".
    var startTime: String
    var endTime: String?

    /// Upstream occasionally omits (or garbles) the end slot — assume one
    /// class hour then.
    var endOrEstimate: Date {
        guard let end, end > start else { return start.addingTimeInterval(50 * 60) }
        return end
    }

    /// A running class holds displays until here before the day's next class
    /// takes over.
    var midpoint: Date {
        start.addingTimeInterval(endOrEstimate.timeIntervalSince(start) / 2)
    }

    var timeRange: String {
        [startTime, endTime].compactMap { $0 }.joined(separator: " – ")
    }
}

extension WidgetScheduleSnapshot {
    /// Concrete occurrences ordered by start, covering `days` days beginning
    /// at the day containing `from`.
    func occurrences(from: Date, days: Int, calendar: Calendar = .current) -> [ClassOccurrence] {
        var result: [ClassOccurrence] = []
        let firstDay = calendar.startOfDay(for: from)
        for offset in 0..<days {
            guard let dayStart = calendar.date(byAdding: .day, value: offset, to: firstDay) else { continue }
            let stamp = dayStart.dayStamp
            let weekday = calendar.component(.weekday, from: dayStart) - 1
            for session in sessions where session.day == weekday {
                guard let start = calendar.date(byAdding: .minute, value: session.startMinute, to: dayStart)
                else { continue }
                result.append(ClassOccurrence(
                    classId: session.classId,
                    code: session.code,
                    title: session.title,
                    room: session.room,
                    teacherName: session.teacherName,
                    topic: topics.first { $0.classId == session.classId && $0.dayStamp == stamp }?.subject,
                    colorIndex: session.colorIndex,
                    start: start,
                    end: session.endMinute.flatMap { calendar.date(byAdding: .minute, value: $0, to: dayStart) },
                    startTime: Self.minutesLabel(session.startMinute),
                    endTime: session.endMinute.map(Self.minutesLabel)
                ))
            }
        }
        return result.sorted { ($0.start, $0.classId) < ($1.start, $1.classId) }
    }

    private static func minutesLabel(_ minutes: Int) -> String {
        String(format: "%02d:%02d", minutes / 60, minutes % 60)
    }
}

/// Reads and writes the snapshot JSON in the shared App Group container —
/// the app is the only writer, the widget extension the main reader.
enum WidgetSnapshotStore {
    static let appGroupId = "group.dev.forcetower.unes.ios"

    private static var fileURL: URL? {
        FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: appGroupId)?
            .appending(path: "widget-schedule.json")
    }

    static func load() -> WidgetScheduleSnapshot? {
        guard let fileURL, let data = try? Data(contentsOf: fileURL) else { return nil }
        guard let snapshot = try? JSONDecoder().decode(WidgetScheduleSnapshot.self, from: data) else {
            log.warn("widget snapshot decode failed")
            return nil
        }
        return snapshot
    }

    static func save(_ snapshot: WidgetScheduleSnapshot) {
        guard let fileURL, let data = try? JSONEncoder().encode(snapshot) else {
            log.warn("widget snapshot write failed reason=encode")
            return
        }
        do {
            try data.write(to: fileURL, options: .atomic)
            log.info("widget snapshot published sessions=\(snapshot.sessions.count) topics=\(snapshot.topics.count)")
        } catch {
            log.warn("widget snapshot write failed reason=file", error: error)
        }
    }

    static func clear() {
        guard let fileURL else { return }
        try? FileManager.default.removeItem(at: fileURL)
    }
}
