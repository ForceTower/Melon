import Foundation

enum ParadoxoFormat {
    /// Grouped integer — "6.868" (pt-BR) / "6,868" (en).
    static func count(_ value: Int, locale: Locale = .autoupdatingCurrent) -> String {
        value.formatted(.number.grouping(.automatic).locale(locale))
    }

    /// Whole percent — "86%".
    static func percent(_ value: Int, locale: Locale = .autoupdatingCurrent) -> String {
        (Double(value) / 100).formatted(.percent.precision(.fractionLength(0)).locale(locale))
    }

    /// Signed one-decimal delta — "+1,8" / "−2,3" (true minus sign).
    static func signedGrade(_ value: Double, locale: Locale = .autoupdatingCurrent) -> String {
        let sign = value < 0 ? "−" : "+"
        return sign + formatGrade(abs(value), locale: locale)
    }

    /// The hero headline: trend deltas are signed, everything else reads as
    /// a grade.
    static func metric(of fact: ParadoxoPulseFact, locale: Locale = .autoupdatingCurrent) -> String {
        switch fact.kind {
        case .trend, .rising:
            signedGrade(fact.metric, locale: locale)
        case .brutal, .kind, .gap, .surprise, .signature:
            formatGrade(fact.metric, locale: locale)
        }
    }
}
