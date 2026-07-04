import Foundation

/// Grade-calculator math over the manual what-if rows. The academic rules
/// themselves (thresholds, needed-final formula, tenth truncation) live in
/// `DisciplineRules` so the calculator, the detail screen, and the Siri
/// intents can never disagree.
enum FinalCountdownMath {
    static let passThreshold = DisciplineRules.passThreshold
    static let failThreshold = DisciplineRules.failThreshold
    static let finalCutoff = DisciplineRules.finalCutoff

    /// Simple or weighted mean of the rows whose score is set. Nil when
    /// nothing's been filled.
    static func average(_ rows: [FCRow], weighted: Bool) -> Double? {
        let known: [(score: Double, weight: Int)] = rows.compactMap { row in
            row.score.map { ($0, row.weight) }
        }
        guard !known.isEmpty else { return nil }
        if !weighted {
            return known.reduce(0.0) { $0 + $1.score } / Double(known.count)
        }
        let weightSum = known.reduce(0) { $0 + $1.weight }
        guard weightSum > 0 else { return nil }
        return known.reduce(0.0) { $0 + $1.score * Double($1.weight) } / Double(weightSum)
    }

    /// The average if every missing score came back as `wildcardValue` —
    /// best- and worst-case projections.
    static func projectAverage(_ rows: [FCRow], weighted: Bool, wildcardValue: Double) -> Double? {
        let projected = rows.map { row -> FCRow in
            guard row.score == nil else { return row }
            var next = row
            next.scoreText = FCRow.text(for: wildcardValue)
            return next
        }
        return average(projected, weighted: weighted)
    }

    static func neededFinal(avg: Double) -> Double {
        DisciplineRules.neededFinal(avg: avg)
    }

    static func floorToTenth(_ value: Double) -> Double {
        DisciplineRules.floorToTenth(value)
    }

    static func ceilToTenth(_ value: Double) -> Double {
        DisciplineRules.ceilToTenth(value)
    }

    /// If exactly one row is missing, the score it needs for the mean to hit
    /// `target`. Nil otherwise — the multi-empty branch shows a projection
    /// range instead of solving.
    static func neededForPass(_ rows: [FCRow], weighted: Bool, target: Double = passThreshold) -> Double? {
        let empties = rows.filter { $0.score == nil }
        guard empties.count == 1 else { return nil }
        if !weighted {
            let knownSum = rows.reduce(0.0) { $0 + ($1.score ?? 0) }
            return target * Double(rows.count) - knownSum
        }
        let weightSum = rows.reduce(0) { $0 + $1.weight }
        let knownSum = rows.reduce(0.0) {
            guard let score = $1.score else { return $0 }
            return $0 + score * Double($1.weight)
        }
        return (target * Double(weightSum) - knownSum) / Double(empties[0].weight)
    }

    static func verdict(for rows: [FCRow], weighted: Bool) -> FCVerdict {
        let allFilled = rows.allSatisfy { $0.score != nil }

        guard let rawAvg = average(rows, weighted: weighted) else {
            return FCVerdict(kind: .empty)
        }
        // Everything compared or displayed uses the truncated grade — a raw
        // 6,95 never gets rounded up to 7,0 and sneaks past the cutoff.
        let avg = floorToTenth(rawAvg)

        if allFilled {
            if avg >= passThreshold {
                return FCVerdict(kind: .passed, avg: avg)
            }
            if avg < failThreshold {
                return FCVerdict(kind: .failed, avg: avg)
            }
            let need = ceilToTenth(neededFinal(avg: avg))
            if need > 10 {
                return FCVerdict(kind: .impossible, avg: avg, need: need)
            }
            return FCVerdict(kind: .final, avg: avg, need: need)
        }

        // Partial: project best/worst and solve for the single-empty case.
        let best = projectAverage(rows, weighted: weighted, wildcardValue: 10).map(floorToTenth)
        let worst = projectAverage(rows, weighted: weighted, wildcardValue: 0).map(floorToTenth)
        let wildcardNeeded = neededForPass(rows, weighted: weighted).map(ceilToTenth)

        if let best, best < failThreshold {
            return FCVerdict(kind: .failingTrack, avg: avg, best: best, worst: worst)
        }
        if let worst, worst >= passThreshold {
            return FCVerdict(kind: .passed, avg: worst, best: best, worst: worst)
        }
        guard let wildcardNeeded else {
            return FCVerdict(kind: .ontrack, avg: avg, best: best, worst: worst)
        }
        if wildcardNeeded <= 10 {
            return FCVerdict(kind: .borderline, avg: avg, best: best, worst: worst, wildcardNeeded: wildcardNeeded)
        }
        return FCVerdict(kind: .borderlineFinal, avg: avg, best: best, worst: worst, wildcardNeeded: wildcardNeeded)
    }

    /// Locale-aware grade formatting — one decimal, "—" for nil.
    static func formatGrade(_ value: Double?) -> String {
        guard let value, !value.isNaN else { return "—" }
        return value.formatted(.number.precision(.fractionLength(1)))
    }
}
