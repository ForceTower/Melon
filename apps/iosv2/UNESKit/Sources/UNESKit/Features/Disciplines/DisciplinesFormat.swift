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
        daysUntil == 0 ? String.localized(.disciplinesToday) : "\(daysUntil)d"
    }

    /// "hoje" / "em 1 dia" / "em 5 dias" for the detail grade rows.
    static func inDaysLabel(_ days: Int) -> String {
        days == 0 ? String.localized(.disciplinesToday) : String.localized(.disciplinesInDays(days))
    }

    static func disciplineCountLabel(_ count: Int) -> String {
        .localized(.disciplinesCourseCount(count))
    }

    /// "31/03/2026" from a yyyy-MM-dd stamp; other shapes pass through.
    static func longDate(_ stamp: String) -> String {
        let parts = stamp.split(separator: "-")
        guard parts.count == 3 else { return stamp }
        return "\(parts[2])/\(parts[1])/\(parts[0])"
    }

    /// "24/03" from a yyyy-MM-dd stamp; other shapes pass through.
    static func shortDate(_ stamp: String) -> String {
        let parts = stamp.split(separator: "-")
        guard parts.count == 3 else { return stamp }
        return "\(parts[2])/\(parts[1])"
    }

    /// "Dep. de Ciências Exatas" from the upstream "Departamento de …";
    /// unexpected shapes render verbatim.
    static func departmentLabel(_ raw: String) -> String {
        let prefix = "departamento de "
        guard raw.lowercased().hasPrefix(prefix) else { return raw }
        return "Dep. de " + raw.dropFirst(prefix.count)
    }

    /// Grades needed to reach a target round *up* — telling a student 6,9
    /// when 6,95 is required would let them fall short. Display truncates.
    static func neededGrade(_ value: Double) -> String {
        let raised = (value * 10).rounded(.up) / 10
        return String(format: "%.1f", raised).replacingOccurrences(of: ".", with: ",")
    }
}

extension DisciplineStatus {
    /// The status pill label, shared by the list and the detail hero.
    var label: String {
        switch self {
        case .approved: .localized(.disciplinesStatusApproved)
        case .failed: .localized(.disciplinesStatusFailed)
        case .finals: .localized(.disciplinesStatusFinals)
        case .lowGrade: .localized(.disciplinesStatusLowGrade)
        case .noGrades: .localized(.disciplinesStatusNoGrades)
        case .ongoing: .localized(.disciplinesStatusOngoing)
        }
    }
}
