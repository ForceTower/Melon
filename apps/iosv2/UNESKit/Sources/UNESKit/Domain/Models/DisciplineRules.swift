import Foundation

/// Academic rules shared by the Turmas list and the discipline detail, so the
/// two screens can never disagree on a verdict.
enum DisciplineRules {
    /// SAGRES 75% attendance rule: the most class-hours a student can miss
    /// while keeping attendance ≥ 75% — always the floor; rounding up would
    /// admit a miss count that already fails the rule.
    static func allowedMissedHours(ofTotal hours: Int) -> Int {
        hours / 4
    }

    static func absenceRisk(missed: Int, allowed: Int) -> AbsenceRisk {
        guard allowed > 0 else { return missed > 0 ? .critical : .ok }
        let ratio = Double(missed) / Double(allowed)
        if ratio >= 0.75 { return .critical }
        if ratio >= 0.5 { return .warning }
        return .ok
    }

    static func status(
        approved: Bool?,
        wentToFinals: Bool,
        finalGrade: Double?,
        partialAverage: Double?
    ) -> DisciplineStatus {
        // Upstream's `approved` is authoritative: a finals-passer closes with
        // a 5–7 mean, so any grade-threshold fallback would misclassify them.
        if let approved {
            return approved ? .approved : .failed
        }
        if wentToFinals { return .finals }
        if let finalGrade {
            // Result mean posted but no verdict yet — infer from the cutoffs.
            if finalGrade >= 7 { return .approved }
            if finalGrade >= 5 { return .finals }
            return .failed
        }
        guard let partialAverage else { return .noGrades }
        return partialAverage < 5.5 ? .lowGrade : .ongoing
    }

    /// Weighted mean of the released grades; upstream always sends weights,
    /// but fall back to the plain mean rather than dropping the average when
    /// they come through malformed.
    static func partialAverage(of released: [(value: Double, weight: Double?)]) -> Double? {
        guard !released.isEmpty else { return nil }
        let weighted = released.compactMap { entry in entry.weight.map { (entry.value, $0) } }
        let weightSum = weighted.reduce(0) { $0 + $1.1 }
        guard weighted.count == released.count, weightSum > 0 else {
            return released.reduce(0) { $0 + $1.value } / Double(released.count)
        }
        return weighted.reduce(0) { $0 + $1.0 * $1.1 } / weightSum
    }
}
