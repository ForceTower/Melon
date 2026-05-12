import Foundation
import WidgetKit
@preconcurrency import Umbrella

/// Wire-format snapshot that ends up as JSON in the App Group container.
/// The widget extension carries its own shape-identical Codable type at
/// `apps/ios/UNESWidgets/WidgetSnapshot.swift` — keep field names aligned.
private struct HostWidgetSnapshot: Codable {
    let generatedAt: Date
    let todayDateIso: String
    let today: [HostClass]
    let nextDay: HostNextDay?

    struct HostClass: Codable {
        let classId: String
        let code: String
        let title: String
        let prof: String?
        let room: String?
        let topic: String?
        let startTime: String
        let endTime: String?
    }

    struct HostNextDay: Codable {
        let dateIso: String
        let daysAway: Int
        let first: HostClass
    }
}

/// Subscribes to the KMP schedule-week flow (already drives the focused
/// schedule screen) and writes a JSON snapshot to the shared App Group
/// container, then asks WidgetKit to refresh the "Próxima aula" widget.
///
/// One flow gives us both today's classes and the next populated day,
/// each with teacher / room / topic resolved — the today-timeline use
/// case omits teacher name. The widget recomputes time-derived state
/// (running / upcoming / dayDone, countdowns) on every timeline tick, so
/// a stale snapshot still renders correctly.
///
/// Lives for the duration of the authenticated session — `ConnectedView`
/// mounts it via `.task`.
@MainActor
final class WidgetSnapshotPublisher {
    static let appGroup = "group.dev.forcetower.unes.ios"
    static let fileName = "next-class-snapshot.json"
    static let widgetKind = "dev.forcetower.unes.ios.widgets.nextClass"

    private let week: ScheduleObserveScheduleWeekUseCase
    private let log = Log.scoped("WidgetSnapshotPublisher")
    private var didStart = false

    init(week: ScheduleObserveScheduleWeekUseCase) {
        self.week = week
    }

    func start() async {
        guard !didStart else { return }
        didStart = true
        log.info("subscribing to widget data flow")

        for await value in week.invoke() {
            publish(week: value)
        }
    }

    private func publish(week: ScheduleScheduleWeek) {
        let snapshot = Self.buildSnapshot(week: week)
        do {
            try Self.write(snapshot: snapshot)
            WidgetCenter.shared.reloadTimelines(ofKind: Self.widgetKind)
            log.debug("widget snapshot published classes=\(snapshot.today.count) nextDay=\(snapshot.nextDay?.dateIso ?? "-")")
        } catch {
            log.warn("widget snapshot write failed", error: error)
        }
    }

    private static func buildSnapshot(week: ScheduleScheduleWeek) -> HostWidgetSnapshot {
        let todayIso = isoDayFormatter.string(from: Date())
        let days = week.days.sorted { $0.dayIndex < $1.dayIndex }
        let todayDay = days.first { $0.dateIso == todayIso }
        let todayClasses = (todayDay?.classes ?? [])
            .sorted { $0.startTime < $1.startTime }
            .map(convert)

        return HostWidgetSnapshot(
            generatedAt: Date(),
            todayDateIso: todayIso,
            today: todayClasses,
            nextDay: nextDayPayload(days: days, todayIso: todayIso)
        )
    }

    private static func write(snapshot: HostWidgetSnapshot) throws {
        guard let dir = FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: appGroup) else {
            throw WidgetSnapshotError.containerUnavailable
        }
        let url = dir.appendingPathComponent(fileName)
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let data = try encoder.encode(snapshot)
        try data.write(to: url, options: [.atomic])
    }

    // Scan forward from today until we hit a day with classes. Tomorrow
    // when possible, else the next populated day within the emitted week
    // (Friday → Monday case). Falls back to the first populated day when
    // today isn't present (between semesters; ObserveScheduleWeek anchors
    // on the previous semester's final week in that case).
    private static func nextDayPayload(
        days: [ScheduleScheduleDay],
        todayIso: String
    ) -> HostWidgetSnapshot.HostNextDay? {
        if let todayIdx = days.firstIndex(where: { $0.dateIso == todayIso }) {
            for offset in 1..<(days.count - todayIdx) {
                let candidate = days[todayIdx + offset]
                if let first = candidate.classes.min(by: { $0.startTime < $1.startTime }) {
                    return HostWidgetSnapshot.HostNextDay(
                        dateIso: candidate.dateIso,
                        daysAway: offset,
                        first: convert(first)
                    )
                }
            }
            return nil
        }

        return days
            .first(where: { !$0.classes.isEmpty })
            .flatMap { day in
                day.classes.min(by: { $0.startTime < $1.startTime }).map { first in
                    HostWidgetSnapshot.HostNextDay(
                        dateIso: day.dateIso,
                        daysAway: 0,
                        first: convert(first)
                    )
                }
            }
    }

    private static func convert(_ c: ScheduleScheduleClass) -> HostWidgetSnapshot.HostClass {
        HostWidgetSnapshot.HostClass(
            classId: c.classId,
            code: c.code,
            title: c.title,
            prof: c.teacherName,
            room: c.room,
            topic: c.topic,
            startTime: trim(c.startTime),
            endTime: c.endTime.map(trim)
        )
    }

    private static func trim(_ value: String) -> String {
        String(value.prefix(5))
    }

    private static let isoDayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd"
        f.timeZone = TimeZone.current
        return f
    }()
}

private enum WidgetSnapshotError: Error {
    case containerUnavailable
}
