import Foundation
import SwiftUI

/// Geometry shared by the week grid's header, canvas, and now line: which
/// day columns exist and how minutes map to points.
struct ScheduleGridLayout: Equatable {
    /// Monday-first indices into the overview's days: weekdays always,
    /// Saturday/Sunday only when they hold classes.
    var dayIndices: [Int]
    var startHour: Int
    var endHour: Int

    /// The design's compact density.
    static let hourHeight: CGFloat = 44
    static let railWidth: CGFloat = 44

    init(days: [ScheduleDay]) {
        var indices = Array(days.indices.prefix(5))
        for weekend in 5..<7 where days.indices.contains(weekend) && !days[weekend].classes.isEmpty {
            indices.append(weekend)
        }
        dayIndices = indices

        let classes = indices.flatMap { days[$0].classes }
        let earliest = classes.map(\.startMinute).min() ?? 7 * 60
        let latest = classes.map { $0.endMinute ?? $0.startMinute + 60 }.max() ?? 18 * 60
        startHour = earliest / 60
        endHour = max(startHour + 1, (latest + 59) / 60)
    }

    /// Rail labels and hairlines, one per hour including the closing edge.
    var hours: [Int] { Array(startHour...endHour) }

    var totalHeight: CGFloat { CGFloat(endHour - startHour) * Self.hourHeight }

    /// Columns tighten once a weekend column joins the five weekdays.
    var isCompact: Bool { dayIndices.count > 5 }

    func y(ofMinute minute: Int) -> CGFloat {
        CGFloat(minute - startHour * 60) / 60 * Self.hourHeight
    }

    func blockHeight(for scheduleClass: ScheduleClass) -> CGFloat {
        let end = scheduleClass.endMinute ?? scheduleClass.startMinute + 60
        return y(ofMinute: end) - y(ofMinute: scheduleClass.startMinute) - 2
    }

    func containsNow(_ nowMinutes: Int) -> Bool {
        (startHour * 60...endHour * 60).contains(nowMinutes)
    }
}

/// Display strings specific to the week-grid Horário.
enum ScheduleGridFormat {
    /// Three-letter column labels (SEG, TER, …) derived from the localized
    /// full names, which truncate and uppercase cleanly in both languages.
    static var dayAbbreviations: [String] {
        ScheduleFormat.dayNames.map { $0.prefix(3).uppercased() }
    }

    /// The single location line used by blocks and agenda rows: room first,
    /// module as fallback.
    static func locationLine(_ scheduleClass: ScheduleClass) -> String {
        if scheduleClass.isOnline { return .localized(.scheduleLocationOnline) }
        if let room = scheduleClass.room { return room }
        if let modulo = scheduleClass.modulo { return modulo }
        return .localized(.scheduleGridLocationTbd)
    }

    /// Short label for the tight grid blocks; long lines fall back to the
    /// module code or their first word.
    static func locationShort(_ scheduleClass: ScheduleClass) -> String {
        let line = locationLine(scheduleClass)
        guard line.count > 9 else { return line }
        if let modulo = scheduleClass.modulo { return modulo }
        let firstWord = line.split { $0 == "," || $0.isWhitespace }.first
        return firstWord.map(String.init) ?? line
    }

    /// The agenda row's full location: "Módulo 5 · MT58 · UEFS".
    static func agendaLocationLine(_ scheduleClass: ScheduleClass) -> String {
        if scheduleClass.isOnline { return .localized(.scheduleLocationOnline) }
        let parts = [scheduleClass.modulo, scheduleClass.room, scheduleClass.campus].compactMap(\.self)
        guard !parts.isEmpty else { return .localized(.scheduleLocationUnset) }
        return parts.joined(separator: " · ")
    }

    /// The sheet's Local row keeps both halves: "MT · LC-03".
    static func sheetLocation(_ scheduleClass: ScheduleClass) -> String {
        if scheduleClass.isOnline { return .localized(.scheduleLocationOnline) }
        let parts = [scheduleClass.modulo, scheduleClass.room].compactMap(\.self)
        guard !parts.isEmpty else { return .localized(.scheduleLocationUnset) }
        return parts.joined(separator: " · ")
    }

    /// "10:20–12:00".
    static func timeRange(_ scheduleClass: ScheduleClass) -> String {
        let start = ScheduleFormat.timeLabel(scheduleClass.startMinute)
        guard let end = scheduleClass.endMinute else { return start }
        return "\(start)–\(ScheduleFormat.timeLabel(end))"
    }

    /// "3 aulas hoje" / "sem aulas hoje" for the navigation subtitle.
    static func todaySummary(count: Int) -> String {
        count == 0
            ? .localized(.scheduleGridNoClassesToday)
            : .localized(.scheduleGridClassesToday(count))
    }
}
