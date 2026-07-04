import Foundation

/// The hero clock, by distance: a live minute countdown inside the final
/// stretch, absolute times beyond it (the widget cannot tick hour-long
/// countdowns the way the in-app hero does).
enum HeroClock {
    case minutes(big: String, unit: String?)
    case sameDay(time: String)
    case tomorrow(time: String)
    case weekday(name: String, time: String)

    init(for occurrence: ClassOccurrence, at date: Date, calendar: Calendar = .current) {
        let remaining = occurrence.start.timeIntervalSince(date)
        if remaining <= NextClassProvider.countdownBand,
           let countdown = HomeFormat.countdown(until: occurrence.start, now: date) {
            self = .minutes(big: countdown.big, unit: countdown.unit)
        } else if calendar.isDate(occurrence.start, inSameDayAs: date) {
            self = .sameDay(time: occurrence.startTime)
        } else if let tomorrow = calendar.date(byAdding: .day, value: 1, to: date),
                  calendar.isDate(occurrence.start, inSameDayAs: tomorrow) {
            self = .tomorrow(time: occurrence.startTime)
        } else {
            self = .weekday(name: HomeFormat.weekdayShort(for: occurrence.start), time: occurrence.startTime)
        }
    }

    /// "em 39 min" / "às 10:20" / "amanhã · 08:00" / "Seg · 08:00".
    var inlineLabel: String {
        switch self {
        case let .minutes(big, unit): String.localized(.widgetInMinutes("\(big)\(unit.map { " \($0)" } ?? "")"))
        case let .sameDay(time): String.localized(.widgetAtTime(time))
        case let .tomorrow(time): "\(String.localized(.widgetTomorrow)) · \(time)"
        case let .weekday(name, time): "\(name) · \(time)"
        }
    }
}
