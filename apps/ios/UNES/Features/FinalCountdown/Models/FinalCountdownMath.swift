import Foundation

/// Grade-calculator math ported from `screens-final-countdown.jsx`.
///
/// UEFS-style rules:
/// - média ≥ 7,0           → aprovação direta
/// - 3,0 ≤ média < 7,0     → Final; precisa de F tal que 0,6·m + 0,4·F ≥ 5
/// - média < 3,0           → reprovação direta, sem direito a Final
enum FinalCountdownMath {
    static let passThreshold: Double = 7
    static let failThreshold: Double = 3
    static let finalCutoff: Double = 5
    static let finalAvgWeight: Double = 0.6
    static let finalExamWeight: Double = 0.4

    /// Simple or weighted mean of the rows whose score is set. Returns nil
    /// when nothing's been filled.
    static func average(_ rows: [FCRow], weighted: Bool) -> Double? {
        let known = rows.filter { $0.score != nil }
        guard !known.isEmpty else { return nil }
        if !weighted {
            return known.reduce(0) { $0 + ($1.score ?? 0) } / Double(known.count)
        }
        let wsum = known.reduce(0) { $0 + $1.weight }
        guard wsum > 0 else { return nil }
        return known.reduce(0) { $0 + ($1.score ?? 0) * Double($1.weight) } / Double(wsum)
    }

    /// What would the average be if every missing score came back as
    /// `wildcardValue`? Used for best- and worst-case projections.
    static func projectAverage(_ rows: [FCRow], weighted: Bool, wildcardValue: Double) -> Double? {
        let projected = rows.map { row -> FCRow in
            guard row.score == nil else { return row }
            var next = row
            next.score = wildcardValue
            return next
        }
        return average(projected, weighted: weighted)
    }

    /// Grade required on the Final to close at `finalCutoff` (5). Solved from
    /// 0,6·m + 0,4·F ≥ 5 → F ≥ (5 − 0,6·m) / 0,4.
    static func neededFinal(avg: Double) -> Double {
        (finalCutoff - finalAvgWeight * avg) / finalExamWeight
    }

    /// Truncate to one decimal. Matches how the university records grades —
    /// a raw 6,95 becomes 6,9, not 7,0.
    static func floorToTenth(_ value: Double) -> Double {
        (value * 10).rounded(.down) / 10
    }

    /// Round up to one decimal. Used for "grade needed" values so that a raw
    /// requirement of 6,47 surfaces as 6,5 — the student scoring exactly the
    /// displayed value still clears the cutoff after truncation.
    static func ceilToTenth(_ value: Double) -> Double {
        (value * 10).rounded(.up) / 10
    }

    /// If exactly one row is missing, the score it would need to hit `target`
    /// overall. Returns nil when the count doesn't match the single-empty
    /// shape, since the UI doesn't solve for multi-empty cases (that branch
    /// shows a projection range instead).
    static func neededForPass(_ rows: [FCRow], weighted: Bool, target: Double = passThreshold) -> Double? {
        let empties = rows.filter { $0.score == nil }
        guard empties.count == 1 else { return nil }
        if !weighted {
            let known = rows.filter { $0.score != nil }
            let n = Double(rows.count)
            let sumKnown = known.reduce(0) { $0 + ($1.score ?? 0) }
            return target * n - sumKnown
        }
        let wsum = rows.reduce(0) { $0 + $1.weight }
        let sumKnown = rows.reduce(0.0) {
            guard let s = $1.score else { return $0 }
            return $0 + s * Double($1.weight)
        }
        let emptyWeight = Double(empties[0].weight)
        return (target * Double(wsum) - sumKnown) / emptyWeight
    }

    static func verdict(for rows: [FCRow], weighted: Bool) -> FCVerdict {
        let allFilled = rows.allSatisfy { $0.score != nil }

        guard let rawAvg = average(rows, weighted: weighted) else {
            return FCVerdict(kind: .empty, avg: nil, best: nil, worst: nil, wildcardNeeded: nil, need: nil)
        }
        // Everything compared or displayed uses the truncated grade — a raw
        // 6,95 never gets rounded up to 7,0 and sneaks past the cutoff.
        let avg = floorToTenth(rawAvg)

        if allFilled {
            if avg >= passThreshold {
                return FCVerdict(kind: .passed, avg: avg, best: nil, worst: nil, wildcardNeeded: nil, need: nil)
            }
            if avg < failThreshold {
                return FCVerdict(kind: .failed, avg: avg, best: nil, worst: nil, wildcardNeeded: nil, need: nil)
            }
            let need = ceilToTenth(neededFinal(avg: avg))
            if need > 10 {
                return FCVerdict(kind: .impossible, avg: avg, best: nil, worst: nil, wildcardNeeded: nil, need: need)
            }
            return FCVerdict(kind: .final, avg: avg, best: nil, worst: nil, wildcardNeeded: nil, need: need)
        }

        // Partial: project best/worst and solve for the single-empty case.
        let best = projectAverage(rows, weighted: weighted, wildcardValue: 10).map(floorToTenth)
        let worst = projectAverage(rows, weighted: weighted, wildcardValue: 0).map(floorToTenth)
        let wildcardNeeded = neededForPass(rows, weighted: weighted).map(ceilToTenth)

        if let best, best < failThreshold {
            return FCVerdict(kind: .failingTrack, avg: avg, best: best, worst: worst, wildcardNeeded: nil, need: nil)
        }
        if let worst, worst >= passThreshold {
            return FCVerdict(kind: .passed, avg: worst, best: best, worst: worst, wildcardNeeded: nil, need: nil)
        }
        guard let wildcardNeeded else {
            return FCVerdict(kind: .ontrack, avg: avg, best: best, worst: worst, wildcardNeeded: nil, need: nil)
        }
        if wildcardNeeded <= 10 {
            return FCVerdict(kind: .borderline, avg: avg, best: best, worst: worst, wildcardNeeded: wildcardNeeded, need: nil)
        }
        return FCVerdict(kind: .borderlineFinal, avg: avg, best: best, worst: worst, wildcardNeeded: wildcardNeeded, need: nil)
    }

    /// pt-BR grade formatting — one decimal, comma separator, `—` for nil.
    static func formatGrade(_ value: Double?) -> String {
        guard let value, !value.isNaN else { return "—" }
        return String(format: "%.1f", value).replacingOccurrences(of: ".", with: ",")
    }
}

/// Verdict copy, ported from `VERDICT_COPY` in the prototype. Kept near the
/// math so the two stay in lockstep when rules or tone change.
enum FinalCountdownCopy {
    static func copy(for verdict: FCVerdict) -> FCVerdictCopy {
        switch verdict.kind {
        case .passed:
            return FCVerdictCopy(
                eyebrow: "aprovada",
                titleLines: ["Fechou!", "Você passou.", "Sem drama."],
                headline: FinalCountdownMath.formatGrade(verdict.avg),
                sub: "média final · acima de 7,0",
                message: "Pode respirar. A média de \(FinalCountdownMath.formatGrade(verdict.avg)) já garante aprovação direta sem ir para a Final.",
                tone: .green, icon: "trophy.fill"
            )
        case .ontrack:
            let needed = verdict.wildcardNeeded
            return FCVerdictCopy(
                eyebrow: "no caminho",
                titleLines: ["Tá firme.", "Só não entregue zerada.", ""],
                headline: "OK",
                sub: needed.map { "mande ≥ \(FinalCountdownMath.formatGrade($0)) e passa em 7,0" } ?? "termine as avaliações",
                message: needed.map {
                    "Com a média atual, qualquer coisa acima de \(FinalCountdownMath.formatGrade($0)) na última avaliação te dá aprovação direta."
                } ?? "A média provável vai te levar pra aprovação direta. Continue assim.",
                tone: .teal, icon: "checkmark"
            )
        case .borderline:
            let need = FinalCountdownMath.formatGrade(verdict.wildcardNeeded)
            return FCVerdictCopy(
                eyebrow: "dá pra passar",
                titleLines: ["Dá pra passar!", "Precisa focar no trabalho.", ""],
                headline: need,
                sub: "necessário no curinga pra evitar a Final",
                message: "Tá apertado mas é possível. Você precisa de \(need) ou mais na última avaliação pra não ir pra final.",
                tone: .amber, icon: "bolt.fill"
            )
        case .borderlineFinal:
            return FCVerdictCopy(
                eyebrow: "rumo à final",
                titleLines: ["Vai ter Final.", "Sem tragédia.", ""],
                headline: "> 10",
                sub: "curinga não resolve sozinho",
                message: "Mesmo com 10 na última avaliação a média não chega em 7,0. A Final vai acontecer — mas ainda é passável.",
                tone: .coral, icon: "flag.fill"
            )
        case .final:
            let avg = FinalCountdownMath.formatGrade(verdict.avg)
            let need = FinalCountdownMath.formatGrade(verdict.need)
            return FCVerdictCopy(
                eyebrow: "indo pra final",
                titleLines: ["Calma.", "Dá pra passar na Final.", ""],
                headline: need,
                sub: "necessário na final pra passar",
                message: "Com média \(avg), você precisa de \(need) na Final. A fórmula é 0,6·média + 0,4·final ≥ 5.",
                tone: .coral, icon: "flag.fill"
            )
        case .impossible:
            let avg = FinalCountdownMath.formatGrade(verdict.avg)
            let need = FinalCountdownMath.formatGrade(verdict.need)
            return FCVerdictCopy(
                eyebrow: "matemática perdida",
                titleLines: ["Não rola.", "Nem com 10 na final.", ""],
                headline: "10",
                sub: "insuficiente — reprovação mesmo com nota máxima",
                message: "Com média \(avg), seria necessário \(need) na Final. Como o máximo é 10, não é mais possível passar pela Final.",
                tone: .plum, icon: "skull.fill"
            )
        case .failed:
            let avg = FinalCountdownMath.formatGrade(verdict.avg)
            return FCVerdictCopy(
                eyebrow: "reprovada",
                titleLines: ["Ciclo encerrado.", "Volta em 2026.2.", ""],
                headline: avg,
                sub: "abaixo de 3,0 · sem Final",
                message: "Média abaixo do piso de 3,0 — não há direito à Final. Disciplina reprovada, mas não é o fim do mundo. Na próxima.",
                tone: .plum, icon: "skull.fill"
            )
        case .failingTrack:
            let best = FinalCountdownMath.formatGrade(verdict.best)
            return FCVerdictCopy(
                eyebrow: "caminho difícil",
                titleLines: ["Complicado.", "Acerte o curinga.", ""],
                headline: best,
                sub: "melhor cenário possível",
                message: "Mesmo tirando 10 no curinga, a média ficaria em \(best). Dá pra ir pra Final, mas é preciso caprichar.",
                tone: .coral, icon: "flag.fill"
            )
        case .empty:
            return FCVerdictCopy(
                eyebrow: "comece a preencher",
                titleLines: ["Preencha as notas", "pra ver o veredicto.", ""],
                headline: "—",
                sub: "insira pelo menos uma avaliação",
                message: "Toque nos campos abaixo e preencha as avaliações que já aconteceram. O curinga (★) é a avaliação que você pode influenciar.",
                tone: .plum, icon: "sparkles"
            )
        }
    }
}
