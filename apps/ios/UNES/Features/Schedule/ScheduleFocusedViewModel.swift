import Foundation
import Observation
import SwiftUI
@preconcurrency import Umbrella

// KMP models surface in the Umbrella framework with the module-prefixed
// names. Alias them locally so the mapper reads naturally alongside the
// iOS `ScheduleClass` presentation struct defined in `ScheduleModels.swift`.
private typealias KmpScheduleWeek = ScheduleScheduleWeek
private typealias KmpScheduleDay = ScheduleScheduleDay
private typealias KmpScheduleClass = ScheduleScheduleClass

// Drives `ScheduleFocusedView`. One KMP flow emits the whole Mon–Sun week
// snapshot; a 30s local clock ticks `nowMin` so the `DONE / NOW / NEXT /
// LATER` state on each class flips at the right minute without forcing the
// flow to re-emit.
@MainActor
@Observable
final class ScheduleFocusedViewModel {
    private(set) var week: [[ScheduleClass]] = Array(repeating: [], count: 7)
    private(set) var dates: [Int] = Array(repeating: 0, count: 7)
    private(set) var todayIdx: Int = -1
    private(set) var weekNumber: Int = 0
    private(set) var weekRangeLabel: String = ""
    private(set) var nowMin: Int = ScheduleFocusedViewModel.minutesNow()
    private(set) var semesterCode: String?

    private let useCases: ScheduleFocusedUseCases
    private var didStart = false

    init(useCases: ScheduleFocusedUseCases) {
        self.useCases = useCases
    }

    func observe() async {
        guard !didStart else { return }
        didStart = true

        async let w: Void = observeWeek()
        async let c: Void = runClockTicker()
        _ = await (w, c)
    }

    private func observeWeek() async {
        for await value in useCases.scheduleWeek.invoke() {
            apply(week: value)
        }
    }

    // Ticks minute-of-day every 30s so class state transitions land close to
    // real time. Exits when the parent task is cancelled.
    private func runClockTicker() async {
        while !Task.isCancelled {
            nowMin = Self.minutesNow()
            try? await Task.sleep(nanoseconds: 30 * 1_000_000_000)
        }
    }

    private func apply(week raw: KmpScheduleWeek) {
        weekNumber = Int(raw.weekNumber)
        semesterCode = raw.semesterCode
        todayIdx = raw.todayDayIndex?.intValue ?? -1

        var bucket = Array<[ScheduleClass]>(repeating: [], count: 7)
        var dayOfMonth = Array<Int>(repeating: 0, count: 7)
        for day in raw.days {
            let idx = Int(day.dayIndex)
            guard (0..<7).contains(idx) else { continue }
            bucket[idx] = day.classes.map(Self.map(kmpClass:))
            dayOfMonth[idx] = Self.dayOfMonth(iso: day.dateIso)
        }
        week = bucket
        dates = dayOfMonth
        weekRangeLabel = Self.formatWeekRange(
            firstIso: raw.days.first?.dateIso,
            lastIso: raw.days.last?.dateIso
        )
    }

    private static func map(kmpClass raw: KmpScheduleClass) -> ScheduleClass {
        ScheduleClass(
            start: trimTime(raw.startTime),
            end: raw.endTime.map(trimTime) ?? "",
            code: raw.code,
            title: raw.title,
            prof: raw.teacherName ?? "",
            color: ColorFor.discipline(code: raw.code),
            modulo: raw.modulo,
            room: raw.room,
            campus: raw.campus,
            topic: raw.topic
        )
    }

    // Upstream ships HH:mm or HH:mm:ss — trim to five chars so the time rail
    // renders minutes only, matching OverviewViewModel's trim.
    private static func trimTime(_ value: String) -> String {
        String(value.prefix(5))
    }

    private static func minutesNow() -> Int {
        let comps = Calendar.current.dateComponents([.hour, .minute], from: Date())
        return (comps.hour ?? 0) * 60 + (comps.minute ?? 0)
    }

    // Parses the day-of-month from an ISO "yyyy-MM-dd" string. Returns 0 if
    // the shape is malformed — upstream always emits well-formed ISO dates,
    // so the fallback is defensive only.
    private static func dayOfMonth(iso: String) -> Int {
        guard iso.count >= 10 else { return 0 }
        let start = iso.index(iso.startIndex, offsetBy: 8)
        let end = iso.index(iso.startIndex, offsetBy: 10)
        return Int(iso[start..<end]) ?? 0
    }

    // Renders a "14 – 20 abr" (same-month) or "28 abr – 4 mai" (spanning)
    // label, matching the header pattern the fixture-driven view used.
    // Portuguese month abbreviations follow the iOS Intl conventions and are
    // stripped of the trailing "." that `dateFormat: "MMM"` emits for some
    // locales.
    private static func formatWeekRange(firstIso: String?, lastIso: String?) -> String {
        guard let firstIso, let lastIso,
              let first = isoDayFormatter.date(from: firstIso),
              let last = isoDayFormatter.date(from: lastIso) else { return "" }
        let cal = Calendar.current
        let fDay = cal.component(.day, from: first)
        let lDay = cal.component(.day, from: last)
        let fMonth = shortMonth(from: first)
        let lMonth = shortMonth(from: last)
        if fMonth == lMonth {
            return "\(fDay) – \(lDay) \(fMonth)"
        }
        return "\(fDay) \(fMonth) – \(lDay) \(lMonth)"
    }

    private static func shortMonth(from date: Date) -> String {
        shortMonthFormatter.string(from: date)
            .replacingOccurrences(of: ".", with: "")
            .lowercased()
    }

    private static let isoDayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd"
        f.timeZone = TimeZone.current
        return f
    }()

    private static let shortMonthFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "pt_BR")
        f.dateFormat = "MMM"
        return f
    }()
}
