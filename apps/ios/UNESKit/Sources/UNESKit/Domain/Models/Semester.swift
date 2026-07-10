import Foundation

struct Semester: Equatable, Sendable, Identifiable {
    let id: String
    var code: String
    var description: String
    /// yyyy-MM-dd, compared lexicographically (matches calendar order).
    var startDate: String
    var endDate: String
}

extension [Semester] {
    /// First semester whose [startDate, endDate] contains today; otherwise the
    /// most recent one. Same rule the KMP dashboard uses.
    func active(today: String) -> Semester? {
        first { $0.startDate <= today && today <= $0.endDate }
            ?? self.max { $0.startDate < $1.startDate }
    }
}

extension Date {
    /// yyyy-MM-dd in the device time zone, for lexicographic comparison
    /// against upstream date stamps.
    var dayStamp: String {
        formatted(Date.ISO8601FormatStyle(timeZone: .current).year().month().day())
    }
}
