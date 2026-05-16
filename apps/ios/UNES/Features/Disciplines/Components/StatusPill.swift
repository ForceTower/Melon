import SwiftUI

/// Tiny uppercase pill showing the discipline's current status
/// (aprovado / em andamento / nota baixa / etc).
struct StatusPill: View {
    let status: DisciplineStatus

    private var palette: (bg: Color, fg: Color) {
        switch status.key {
        case .approved:
            let teal = DisciplineScoreColor.excellent
            return (teal.opacity(0.13), Color(red: 0x2A/255, green: 0x7E/255, blue: 0x8B/255))
        case .ongoing:
            return (UNESColor.surface2, UNESColor.ink3)
        case .low:
            let coral = DisciplineScoreColor.danger
            return (coral.opacity(0.13), Color(red: 0xB8/255, green: 0x46/255, blue: 0x3A/255))
        case .failed:
            let coral = DisciplineScoreColor.danger
            return (coral.opacity(0.20), Color(red: 0xB8/255, green: 0x46/255, blue: 0x3A/255))
        case .final:
            let amber = DisciplineScoreColor.caution
            return (amber.opacity(0.13), Color(red: 0xA0/255, green: 0x64/255, blue: 0x19/255))
        case .pending:
            return (UNESColor.surface2, UNESColor.ink4)
        }
    }

    var body: some View {
        Text(status.label)
            .font(UNESFont.mono(9, weight: .semibold))
            .tracking(0.9)
            .textCase(.uppercase)
            .foregroundStyle(palette.fg)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(
                RoundedRectangle(cornerRadius: 5, style: .continuous)
                    .fill(palette.bg)
            )
    }
}

/// Small chip showing a single evaluation (label + score or placeholder).
/// Used inside the active discipline card to give a glance-level summary of
/// every graded item.
struct EvalChip: View {
    let grade: GradeEntry
    let accent: Color

    private var hasScore: Bool { grade.score != nil }

    private var past: Bool {
        guard let days = DisciplineDate.daysUntil(grade.date) else { return false }
        return days < 0
    }

    var body: some View {
        HStack(spacing: 7) {
            Text(grade.label)
                .font(UNESFont.mono(9, weight: .bold))
                .tracking(0.54)
                .foregroundStyle(hasScore ? accent : UNESColor.ink4)
                .fixedSize()
                .padding(.horizontal, 5)
                .padding(.vertical, 2)
                .background(
                    RoundedRectangle(cornerRadius: 4, style: .continuous)
                        .fill(hasScore ? accent.opacity(0.13) : UNESColor.line)
                )

            Text(scoreText)
                .font(UNESFont.serif(14))
                .tracking(-0.14)
                .fixedSize()
                .foregroundStyle(hasScore
                                 ? DisciplineScoreColor.color(for: grade.score)
                                 : UNESColor.ink4)
        }
        .padding(.leading, 3)
        .padding(.trailing, 9)
        .padding(.vertical, 6)
        .background(
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .fill(hasScore ? UNESColor.surface2 : Color.clear)
                .overlay(
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .strokeBorder(hasScore ? Color.clear : UNESColor.line, lineWidth: 1)
                )
        )
        .fixedSize()
    }

    private var scoreText: String {
        if let score = grade.score { return String(format: "%.1f", score) }
        return past ? "—" : "·"
    }
}

#Preview {
    VStack(spacing: 12) {
        StatusPill(status: .init(key: .ongoing, label: "em andamento"))
        StatusPill(status: .init(key: .approved, label: "aprovado"))
        StatusPill(status: .init(key: .failed, label: "reprovado"))
        HStack(spacing: 6) {
            EvalChip(grade: .init(label: "AV1", title: "Prova 1", date: "31/03/2026", score: 8.3),
                     accent: UNESColor.coral)
            EvalChip(grade: .init(label: "AV2", title: "Prova 2", date: "12/05/2026", score: nil),
                     accent: UNESColor.coral)
        }
    }
    .padding()
    .background(UNESColor.surface)
}
