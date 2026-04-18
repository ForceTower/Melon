import SwiftUI

/// 3-cell summary strip rendered at the top of the list. Mirrors
/// `CurrentSemesterSummary` from the prototype.
struct CurrentSemesterSummary: View {
    let disciplines: [Discipline]

    private var averages: [Double] {
        disciplines.compactMap(\.partialAverage)
    }

    private var mean: Double? {
        guard !averages.isEmpty else { return nil }
        return averages.reduce(0, +) / Double(averages.count)
    }

    private var atRiskCount: Int {
        disciplines.filter { $0.absenceRisk != .ok }.count
    }

    private var lowGradeCount: Int {
        disciplines.filter {
            guard let avg = $0.partialAverage else { return false }
            return avg < 6
        }.count
    }

    private var attention: Int { atRiskCount + lowGradeCount }

    var body: some View {
        // `alignment: .top` + `.fixedSize(...vertical: false)` off and
        // `.frame(maxHeight: .infinity)` per cell lets each cell expand
        // to match the tallest sibling, so the subtitle on "Atenção"
        // doesn't make it taller than the others.
        HStack(alignment: .top, spacing: 8) {
            Cell(label: "Média parcial",
                 value: mean.map { String(format: "%.1f", $0) } ?? "—",
                 color: mean.map { DisciplineScoreColor.color(for: $0) } ?? UNESColor.ink3,
                 subtitle: nil)

            Cell(label: "Disciplinas",
                 value: "\(disciplines.count)",
                 color: UNESColor.ink,
                 subtitle: nil)

            Cell(label: "Atenção",
                 value: "\(attention)",
                 color: attention > 0 ? DisciplineScoreColor.caution : UNESColor.ink,
                 subtitle: attention > 0 ? "itens" : "nada")
        }
        .fixedSize(horizontal: false, vertical: true)
        .padding(.horizontal, 16)
    }

    private struct Cell: View {
        let label: String
        let value: String
        let color: Color
        let subtitle: String?

        var body: some View {
            VStack(alignment: .leading, spacing: 4) {
                Text(label)
                    .font(UNESFont.mono(8.5, weight: .semibold))
                    .tracking(0.85)
                    .textCase(.uppercase)
                    .foregroundStyle(UNESColor.ink4)
                Text(value)
                    .font(UNESFont.serif(22))
                    .tracking(-0.44)
                    .foregroundStyle(color)
                if let subtitle {
                    Text(subtitle)
                        .font(UNESFont.sans(10))
                        .foregroundStyle(UNESColor.ink4)
                        .padding(.top, 2)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .padding(.horizontal, 12)
            .padding(.vertical, 11)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(UNESColor.card)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .strokeBorder(UNESColor.cardLine, lineWidth: 1)
                    )
            )
        }
    }
}
