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

    // MARK: Prova Final math
    //
    // UEFS-style rules, shared by the detail screen, the FinalCountdown
    // calculator, and the Siri intents:
    // - média ≥ 7,0        → aprovação direta
    // - 3,0 ≤ média < 7,0  → Prova Final; precisa de F tal que 0,6·m + 0,4·F ≥ 5
    // - média < 3,0        → reprovação direta, sem direito à Prova Final

    static let passThreshold: Double = 7
    static let failThreshold: Double = 3
    static let finalCutoff: Double = 5
    static let finalAvgWeight: Double = 0.6
    static let finalExamWeight: Double = 0.4

    /// Grade required on the Prova Final to close at `finalCutoff` (5).
    /// Solved from 0,6·m + 0,4·F ≥ 5 → F ≥ (5 − 0,6·m) / 0,4.
    static func neededFinal(avg: Double) -> Double {
        (finalCutoff - finalAvgWeight * avg) / finalExamWeight
    }

    /// Truncate to one decimal. Matches how the university records grades —
    /// a raw 6,95 becomes 6,9, not 7,0. The epsilon absorbs float noise so a
    /// mathematically exact 8,3 arriving as 8.2999…97 doesn't lose a tenth.
    static func floorToTenth(_ value: Double) -> Double {
        (value * 10 + 1e-9).rounded(.down) / 10
    }

    /// Round up to one decimal. Used for "grade needed" values so a raw
    /// requirement of 6,47 surfaces as 6,5 — scoring exactly the displayed
    /// value still clears the cutoff after truncation. The epsilon absorbs
    /// float noise so an exact 4,7 arriving as 4.700…01 doesn't gain a tenth.
    static func ceilToTenth(_ value: Double) -> Double {
        (value * 10 - 1e-9).rounded(.up) / 10
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
