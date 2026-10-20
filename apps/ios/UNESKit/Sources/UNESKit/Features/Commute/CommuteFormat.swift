import Foundation

/// The display rules for a bus estimate: a minute band while the bus is on
/// its way, a clock band while it waits at its origin, and only an
/// approximate time when there is no bus or it is more than 30 minutes out.
/// Never seconds.
enum CommuteFormat {
    struct Estimate: Equatable {
        var text: String
        /// Rendered with a smaller, dimmer leading "~".
        var isApproximate: Bool
    }

    private static let approximateBeyond: TimeInterval = 30 * 60

    /// "07h02" in pt-BR, "07:02" elsewhere.
    static func clock(_ date: Date, calendar: Calendar = .current) -> String {
        let parts = calendar.dateComponents([.hour, .minute], from: date)
        return .localized(.commuteClock(
            String(format: "%02d", parts.hour ?? 0),
            String(format: "%02d", parts.minute ?? 0)
        ))
    }

    /// "07h02–07h08", collapsing to one time when both fall in the same minute.
    static func clockRange(_ start: Date, _ end: Date) -> String {
        let from = clock(start), to = clock(end)
        return from == to ? from : "\(from)–\(to)"
    }

    static func estimate(for arrival: TransitArrival, now: Date) -> Estimate {
        if arrival.state == .historical || arrival.expected.timeIntervalSince(now) > approximateBeyond {
            return Estimate(text: clock(arrival.expected), isApproximate: true)
        }
        // A bus that stopped reporting keeps its last band on the clock, so
        // it stops counting down instead of pretending to move.
        if arrival.state == .waiting || arrival.signalLost {
            return Estimate(text: clockRange(arrival.earliest, arrival.latest), isApproximate: false)
        }
        if arrival.phase == .atStop {
            return Estimate(text: .localized(.commuteEstimateAtStop), isApproximate: false)
        }
        let low = Int((arrival.earliest.timeIntervalSince(now) / 60).rounded(.down))
        let high = Int((arrival.latest.timeIntervalSince(now) / 60).rounded(.up))
        if high <= 1 {
            return Estimate(text: .localized(.commuteEstimateArriving), isApproximate: false)
        }
        let floor = max(1, low)
        let text: String = floor >= high
            ? .localized(.commuteEstimateMinutes(high))
            : .localized(.commuteEstimateMinuteRange(floor, high))
        return Estimate(text: text, isApproximate: false)
    }

    /// "atualizado há 8 s" — the bus's last new position, not our request.
    static func freshness(since fix: Date, now: Date, short: Bool = false) -> String {
        let seconds = max(0, Int(now.timeIntervalSince(fix)))
        if seconds < 60 {
            return short ? .localized(.commuteFreshSecondsShort(seconds)) : .localized(.commuteFreshSeconds(seconds))
        }
        return short ? .localized(.commuteFreshMinutesShort(seconds / 60)) : .localized(.commuteFreshMinutes(seconds / 60))
    }

    static func signalLost(since fix: Date, now: Date) -> String {
        .localized(.commuteSignalLostFor(max(1, Int(now.timeIntervalSince(fix) / 60))))
    }

    /// "linhas 087 e 088" — natural-language list of line numbers.
    static func lineList(_ numbers: [String]) -> String {
        numbers.formatted(.list(type: .and))
    }

    static func minutesUntil(_ date: Date, now: Date) -> Int {
        max(1, Int((date.timeIntervalSince(now) / 60).rounded()))
    }
}
