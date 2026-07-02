import Foundation

/// pt-BR display strings for the Turmas screen.
enum DisciplinesFormat {
    /// "2026.1" from the raw five-digit SAGRES code ("20261"). Other shapes
    /// ("2010.2", "2010", suffixed variants) pass through untouched.
    static func semesterLabel(_ code: String) -> String {
        let trimmed = code.trimmingCharacters(in: .whitespaces)
        guard trimmed.count == 5, trimmed.allSatisfy(\.isNumber) else { return trimmed }
        return "\(trimmed.prefix(4)).\(trimmed.suffix(1))"
    }

    /// "+0,2" / "−1,3" — one decimal, truncated toward zero like every other
    /// grade display.
    static func signedDelta(_ value: Double) -> String {
        let truncated = (abs(value) * 10).rounded(.down) / 10
        let body = String(format: "%.1f", truncated).replacingOccurrences(of: ".", with: ",")
        return (value < 0 ? "−" : "+") + body
    }

    /// "hoje" / "3d" for the next-evaluation countdown.
    static func countdownLabel(daysUntil: Int) -> String {
        daysUntil == 0 ? "hoje" : "\(daysUntil)d"
    }

    static func disciplineCountLabel(_ count: Int) -> String {
        count == 1 ? "1 disciplina" : "\(count) disciplinas"
    }
}
