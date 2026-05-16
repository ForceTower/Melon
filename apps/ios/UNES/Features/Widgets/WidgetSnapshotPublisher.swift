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

/// Subscribes to the KMP schedule flows (current week for today's classes,
/// `ObserveNextClassDayUseCase` for the first future day with classes) and
/// writes a JSON snapshot to the shared App Group container, then asks
/// WidgetKit to refresh the "Próxima aula" widget.
///
/// Two flows on purpose: the week flow drives today's strip, while the
/// next-class-day flow looks past the current week so Friday → Tuesday
/// (with no Sat/Sun classes in between) still surfaces real data.
///
/// The widget recomputes time-derived state (running / upcoming / dayDone,
/// countdowns) on every timeline tick, so a stale snapshot still renders
/// correctly. Lives for the duration of the authenticated session —
/// `ConnectedView` mounts it via `.task`.
@MainActor
final class WidgetSnapshotPublisher {
    static let appGroup = "group.dev.forcetower.unes.ios"
    static let fileName = "next-class-snapshot.json"
    static let widgetKind = "dev.forcetower.unes.ios.widgets.nextClass"

    private let week: ScheduleObserveScheduleWeekUseCase
    private let nextDay: ScheduleObserveNextClassDayUseCase
    private let log = Log.scoped("WidgetSnapshotPublisher")

    private var latestWeek: ScheduleScheduleWeek?
    private var latestNextDay: ScheduleNextClassDay?
    private var didStart = false

    init(
        week: ScheduleObserveScheduleWeekUseCase,
        nextDay: ScheduleObserveNextClassDayUseCase
    ) {
        self.week = week
        self.nextDay = nextDay
    }

    func start() async {
        guard !didStart else { return }
        didStart = true
        log.info("subscribing to widget data flows")

        async let w: Void = observeWeek()
        async let n: Void = observeNextDay()
        _ = await (w, n)
    }

    private func observeWeek() async {
        for await value in week.invoke() {
            latestWeek = value
            publish(reason: "week")
        }
    }

    private func observeNextDay() async {
        for await value in nextDay.invoke() {
            latestNextDay = value
            publish(reason: "nextDay")
        }
    }

    private func publish(reason: String) {
        guard let week = latestWeek else { return }

        #if DEBUG
        // Skip the write when the widget debug clock override is active so
        // hand-edited snapshots used for widget visual testing survive the
        // app relaunch. Still reload timelines so the widget picks up the
        // disk state. Compiles out in Release.
        if let defaults = UserDefaults(suiteName: Self.appGroup),
           defaults.string(forKey: "widget.debug.now") != nil {
            WidgetCenter.shared.reloadTimelines(ofKind: Self.widgetKind)
            log.debug("widget snapshot write skipped — debug clock override active (reason=\(reason))")
            return
        }
        #endif

        let snapshot = Self.buildSnapshot(week: week, nextDay: latestNextDay)
        do {
            try Self.write(snapshot: snapshot)
            WidgetCenter.shared.reloadTimelines(ofKind: Self.widgetKind)
            log.debug("widget snapshot published reason=\(reason) classes=\(snapshot.today.count) nextDay=\(snapshot.nextDay?.dateIso ?? "-")")
        } catch {
            log.warn("widget snapshot write failed reason=\(reason)", error: error)
        }
    }

    private static func buildSnapshot(
        week: ScheduleScheduleWeek,
        nextDay: ScheduleNextClassDay?
    ) -> HostWidgetSnapshot {
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
            nextDay: nextDay.map { day in
                HostWidgetSnapshot.HostNextDay(
                    dateIso: day.dateIso,
                    daysAway: Int(day.daysAway),
                    first: convert(day.first)
                )
            }
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
