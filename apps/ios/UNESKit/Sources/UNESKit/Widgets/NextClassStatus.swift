import Foundation

/// What the "Próxima aula" surfaces (widget, Siri intents) show at one moment.
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

extension NextClassStatus {
    /// One moment's read of the schedule — the single source of truth shared
    /// by the Next Class widget and the Siri data intents.
    static func compute(
        at date: Date,
        occurrences: [ClassOccurrence],
        calendar: Calendar
    ) -> (status: NextClassStatus, today: [ClassOccurrence]) {
        let today = occurrences.filter { calendar.isDate($0.start, inSameDayAs: date) }
        let status: NextClassStatus
        if let current = today.first(where: { $0.start <= date && date < $0.endOrEstimate }) {
            // A running class holds the display until halfway through — and
            // the day's last class until it ends — before the next takes over.
            let later = today.first { $0.start > date }
            if let later, date >= current.midpoint {
                status = .upcoming(later)
            } else {
                status = .inClass(current)
            }
        } else if let nextToday = today.first(where: { $0.start > date }) {
            status = .upcoming(nextToday)
        } else {
            status = .dayDone(completed: today.count, next: occurrences.first { $0.start > date })
        }
        return (status, today)
    }
}
