import SwiftUI

/// One evaluation row in the calculator sandbox. `scoreText` holds exactly
/// what the student typed (comma decimals); `score` is the parsed grade —
/// nil while the field is blank, which marks the row as "still to come".
struct FCRow: Equatable, Sendable, Identifiable {
    let id: String
    var label: String
    var scoreText: String = ""
    var weight: Int = 1

    var score: Double? { Self.parseScore(scoreText) }

    static func parseScore(_ text: String) -> Double? {
        guard let value = Double(text.replacingOccurrences(of: ",", with: ".")) else { return nil }
        return min(max(value, 0), 10)
    }

    /// Keeps digits and a single decimal comma, capping the value at 10 —
    /// the field never holds an out-of-range grade.
    static func sanitizeScoreText(_ raw: String) -> String {
        var seenSeparator = false
        var clean = ""
        for character in raw {
            if character.isNumber {
                clean.append(character)
            } else if character == "," || character == ".", !seenSeparator {
                seenSeparator = true
                clean.append(",")
            }
        }
        clean = String(clean.prefix(5))
        if let raw = Double(clean.replacingOccurrences(of: ",", with: ".")), raw > 10 {
            return "10"
        }
        return clean
    }

    /// Seed text for a released grade — "8,3", trailing zeros dropped ("8").
    static func text(for value: Double?) -> String {
        guard let value else { return "" }
        return String(format: "%g", value).replacingOccurrences(of: ".", with: ",")
    }
}

/// The discipline the sandbox was seeded from — context row + picker state.
struct FCDiscipline: Equatable, Sendable {
    var id: String
    var name: String
    var teacherName: String?
    var colorIndex: Int
    /// Raw semester code ("20261") — display through
    /// `DisciplinesFormat.semesterLabel`. Nil until the overview lands.
    var semesterCode: String?

    /// "2026.2" after "20261" — the semester a failed discipline returns in.
    /// Non-standard codes yield nil and the copy falls back.
    var nextSemesterLabel: String? {
        guard let code = semesterCode, code.count == 5, let year = Int(code.prefix(4)) else { return nil }
        switch code.last {
        case "1": return "\(year).2"
        case "2": return "\(year + 1).1"
        default: return nil
        }
    }

    /// Up-to-three initials for the color badge — "Cálculo Diferencial II"
    /// becomes "CDI"; connector words don't count.
    var shortLabel: String {
        let connectors: Set<String> = ["a", "à", "as", "às", "e", "em", "de", "da", "das", "do", "dos", "para", "o", "os"]
        let initials = name
            .split(separator: " ")
            .filter { !connectors.contains($0.lowercased()) }
            .prefix(3)
            .compactMap(\.first)
        return String(initials).uppercased()
    }
}

/// The outcomes the calculator surfaces. Mirrors `fcv2Verdict` in
/// `screens-fc-v2-core.jsx`.
///
/// - `passed`: all rows filled with avg ≥ 7, or worst case already ≥ 7.
/// - `ontrack`: several rows missing, not failing, not guaranteed either.
/// - `borderline`: one row missing; needs ≥ X on it to skip the Prova Final.
/// - `borderlineFinal`: even a 10 on the missing row won't reach 7 — Prova
///   Final inevitable but still winnable.
/// - `final`: all filled, 3 ≤ avg < 7; needs `need` on the Prova Final.
/// - `impossible`: all filled, Prova Final math needs > 10.
/// - `failed`: all filled, avg < 3, no right to the Prova Final.
/// - `failingTrack`: rows missing, even best case stays below 3.
/// - `empty`: nothing to compute yet.
enum FCVerdictKind: Equatable, Sendable {
    case passed, ontrack, borderline, borderlineFinal, final, impossible, failed, failingTrack, empty
}

struct FCVerdict: Equatable, Sendable {
    var kind: FCVerdictKind
    var avg: Double?
    var best: Double?
    var worst: Double?
    /// Grade required on the single missing row to close at 7.
    var wildcardNeeded: Double?
    /// Grade required on the Prova Final to close at 5 (`final` state).
    var need: Double?
}

// MARK: - Presentation

/// Everything the hero renders for one verdict — tone (mesh + on-dark hue),
/// icon, and the resolved copy. Ported from `FCV2_VERDICT`.
struct FCVerdictStyle: Equatable {
    var eyebrow: String
    var mesh: MeshView.Variant
    /// Bright accent used on the dark hero: ring fill, icon square, stat value.
    var hue: Color
    var icon: String
    var title: String
    var lead: String
    var statLabel: String
    var statValue: String
    var detail: String
}

extension FCVerdict {
    /// `nextSemesterLabel` feeds the `failed` lead ("Volta em 2026.2.").
    func style(nextSemesterLabel: String? = nil) -> FCVerdictStyle {
        let avgText = FinalCountdownMath.formatGrade(avg)
        switch kind {
        case .passed:
            return FCVerdictStyle(
                eyebrow: "aprovação direta", mesh: .fresh, hue: Color(hex: 0x4FD69C), icon: "trophy.fill",
                title: "Fechou.", lead: "Média acima de 7,0. Sem Prova Final.",
                statLabel: "média final", statValue: avgText,
                detail: "A média de \(avgText) já garante aprovação direta. Pode respirar."
            )
        case .ontrack:
            let needed = wildcardNeeded.map(FinalCountdownMath.formatGrade)
            return FCVerdictStyle(
                eyebrow: "no caminho", mesh: .cool, hue: Color(hex: 0x5AD1E0), icon: "checkmark",
                title: "Tá firme.", lead: needed != nil ? "Só não entregue zerada." : "Termine as avaliações.",
                statLabel: needed != nil ? "precisa na próxima" : "projeção",
                statValue: needed.map { "≥ \($0)" } ?? "OK",
                detail: needed.map {
                    "Qualquer coisa acima de \($0) na última avaliação te dá aprovação direta."
                } ?? "A média provável leva à aprovação direta. Continue assim."
            )
        case .borderline:
            let needed = FinalCountdownMath.formatGrade(wildcardNeeded)
            return FCVerdictStyle(
                eyebrow: "dá pra passar", mesh: .sun, hue: Color(hex: 0xF4B54C), icon: "bolt.fill",
                title: "Dá pra passar.", lead: "Apertado, mas possível.",
                statLabel: "precisa na próxima", statValue: needed,
                detail: "Você precisa de \(needed) ou mais na última avaliação pra evitar a Prova Final."
            )
        case .borderlineFinal:
            return FCVerdictStyle(
                eyebrow: "rumo à final", mesh: .warm, hue: Color(hex: 0xF0805E), icon: "flag.fill",
                title: "Vai ter Prova Final.", lead: "Sem tragédia. Ainda é passável.",
                statLabel: "nem com 10", statValue: "> 10",
                detail: "Mesmo com 10 na última avaliação a média não chega a 7,0. A Prova Final vai acontecer."
            )
        case .final:
            let needText = FinalCountdownMath.formatGrade(need)
            return FCVerdictStyle(
                eyebrow: "indo pra final", mesh: .warm, hue: Color(hex: 0xF0805E), icon: "flag.fill",
                title: "Calma.", lead: "Passa na Prova Final.",
                statLabel: "precisa na final", statValue: needText,
                detail: "Com média \(avgText), você precisa de \(needText) na Prova Final. "
                    + "Fórmula: 0,6·média + 0,4·final ≥ 5."
            )
        case .impossible:
            let needText = FinalCountdownMath.formatGrade(need)
            return FCVerdictStyle(
                eyebrow: "matemática perdida", mesh: .rose, hue: Color(hex: 0xC97BD6), icon: "xmark.seal.fill",
                title: "Não rola.", lead: "Nem com 10 na final.",
                statLabel: "necessário na final", statValue: needText,
                detail: "Com média \(avgText), seria preciso \(needText) na Prova Final. "
                    + "O máximo é 10, então não passa pela final."
            )
        case .failed:
            return FCVerdictStyle(
                eyebrow: "reprovada", mesh: .rose, hue: Color(hex: 0xC97BD6), icon: "xmark.seal.fill",
                title: "Ciclo encerrado.", lead: nextSemesterLabel.map { "Volta em \($0)." } ?? "Fica pra próxima.",
                statLabel: "média · sem final", statValue: avgText,
                detail: "Média abaixo do piso de 3,0, sem direito à Prova Final. "
                    + "Não é o fim do mundo. Na próxima."
            )
        case .failingTrack:
            let bestText = FinalCountdownMath.formatGrade(best)
            return FCVerdictStyle(
                eyebrow: "caminho difícil", mesh: .warm, hue: Color(hex: 0xF0805E), icon: "flag.fill",
                title: "Complicado.", lead: "Acerte a próxima.",
                statLabel: "melhor cenário", statValue: bestText,
                detail: "Mesmo tirando 10 na avaliação restante, a média fica em \(bestText). "
                    + "Dá pra ir pra final, mas caprichando."
            )
        case .empty:
            return FCVerdictStyle(
                eyebrow: "comece a preencher", mesh: .cool, hue: Color(hex: 0x5AD1E0), icon: "sparkles",
                title: "Preencha as notas.", lead: "Pra ver o veredicto.",
                statLabel: "média atual", statValue: "—",
                detail: "Toque nos campos abaixo e preencha as avaliações que já aconteceram. "
                    + "Deixe em branco a que ainda vem."
            )
        }
    }
}
