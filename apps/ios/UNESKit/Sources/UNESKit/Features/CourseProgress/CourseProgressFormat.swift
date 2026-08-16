import Foundation

enum CourseProgressFormat {
    /// Grouped integer hours with the unit — "1.500 h" (pt-BR) / "1,500 h".
    static func hours(_ value: Int, locale: Locale = .autoupdatingCurrent) -> String {
        .localized(.courseProgressHours(count(value, locale: locale)))
    }

    /// Grouped integer — "4.040" / "4,040".
    static func count(_ value: Int, locale: Locale = .autoupdatingCurrent) -> String {
        value.formatted(.number.grouping(.automatic).locale(locale))
    }

    /// `value` is 0…100. Whole numbers print bare ("53%"), the rest keep up
    /// to `fractionDigits` ("53,19%").
    static func percent(_ value: Double, fractionDigits: Int = 2, locale: Locale = .autoupdatingCurrent) -> String {
        (value / 100).formatted(
            .percent.precision(.fractionLength(0...fractionDigits)).locale(locale)
        )
    }

    /// The headline percent — always one decimal so 37,1% never rounds up
    /// to a round 40 in the reader's head.
    static func headlinePercent(_ value: Double, locale: Locale = .autoupdatingCurrent) -> String {
        (value / 100).formatted(.percent.precision(.fractionLength(1)).locale(locale))
    }

    /// Ordinal período — "3º" (pt-BR) / "3rd".
    static func ordinal(_ period: Int, locale: Locale = .autoupdatingCurrent) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .ordinal
        formatter.locale = locale
        return formatter.string(from: NSNumber(value: period)) ?? String(period)
    }

    /// "3º semestre" / "3rd semester".
    static func semester(_ period: Int, locale: Locale = .autoupdatingCurrent) -> String {
        .localized(.courseProgressSemester(ordinal(period, locale: locale)))
    }

    /// "16 ago 2026, 07:12" — when the mirror last heard from the portal.
    static func syncedAt(_ date: Date, locale: Locale = .autoupdatingCurrent) -> String {
        date.formatted(
            Date.FormatStyle(date: .abbreviated, time: .shortened).locale(locale)
        )
    }

    /// The source-document date — "4 mar 2024".
    static func asOf(_ date: Date, locale: Locale = .autoupdatingCurrent) -> String {
        date.formatted(Date.FormatStyle(date: .abbreviated, time: .omitted).locale(locale))
    }
}
