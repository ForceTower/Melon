import SwiftUI

/// "Composição": one bar per evaluation colored by its band, hatched while
/// blank, plus the three rule tiles (piso, aprovação, fórmula).
struct FCBreakdownCard: View {
    let rows: [FCRow]
    let weighted: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
            VStack(spacing: 11) {
                ForEach(rows) { row in
                    barRow(row)
                }
            }
            .padding(.top, 14)

            rulesGrid
                .padding(.top, 13)
                .overlay(alignment: .top) {
                    Rectangle().fill(UNESColor.line).frame(height: 0.5)
                }
                .padding(.top, 14)
        }
        .padding(EdgeInsets(top: 15, leading: 16, bottom: 14, trailing: 16))
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
    }

    private var header: some View {
        HStack {
            Text(.finalCountdownBreakdownTitle)
                .font(.system(size: 15, weight: .bold))
                .tracking(-0.3)
                .foregroundStyle(UNESColor.ink)
            Spacer()
            Text(weighted ? .finalCountdownBreakdownScoreWeightLabel : .finalCountdownBreakdownScoreLabel)
                .textCase(.uppercase)
                .font(.system(size: 11.5, weight: .semibold))
                .tracking(0.4)
                .foregroundStyle(UNESColor.ink4)
        }
    }

    private func barRow(_ row: FCRow) -> some View {
        HStack(spacing: 12) {
            Text(row.label)
                .font(.system(size: 13, weight: .semibold))
                .tracking(-0.13)
                .lineLimit(1)
                .foregroundStyle(UNESColor.ink2)
                .frame(width: 40, alignment: .leading)

            bar(for: row.score)

            HStack(alignment: .lastTextBaseline, spacing: 4) {
                Text(FinalCountdownMath.formatGrade(row.score))
                    .font(.system(size: 15, weight: .semibold))
                    .tracking(-0.15)
                    .monospacedDigit()
                    .foregroundStyle(row.score != nil ? UNESColor.ink : UNESColor.ink4)
                if weighted {
                    Text(verbatim: "×\(row.weight)")
                        .font(.system(size: 11, weight: .semibold))
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink4)
                }
            }
            .frame(minWidth: 52, alignment: .trailing)
        }
    }

    private func bar(for score: Double?) -> some View {
        GeometryReader { geometry in
            ZStack(alignment: .leading) {
                Capsule().fill(UNESColor.surface3)
                if let score {
                    Capsule()
                        .fill(barColor(score))
                        .frame(width: geometry.size.width * score / 10)
                        .animation(UNESMotion.ease(0.4), value: score)
                } else {
                    HatchedStripes()
                        .fill(UNESColor.surface3.opacity(0.7))
                        .clipShape(Capsule())
                }
            }
        }
        .frame(height: 7)
    }

    /// Green at pass, orange in the Prova Final band, coral below the floor.
    private func barColor(_ score: Double) -> Color {
        if score >= FinalCountdownMath.passThreshold { return UNESColor.successGreen }
        if score >= FinalCountdownMath.failThreshold { return UNESColor.tangerine }
        return UNESColor.coral
    }

    private var rulesGrid: some View {
        HStack(spacing: 8) {
            ruleTile(label: String.localized(.finalCountdownBreakdownFloorLabel), value: "3,0", color: UNESColor.coral)
            ruleTile(label: String.localized(.finalCountdownBreakdownPassLabel), value: "7,0", color: UNESColor.successGreen)
            ruleTile(label: String.localized(.finalCountdownBreakdownFormulaLabel), value: "0,6m+0,4f", color: Color(hex: 0x2AA5B8))
        }
    }

    private func ruleTile(label: String, value: String, color: Color) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(label)
                .textCase(.uppercase)
                .font(.system(size: 10, weight: .semibold))
                .tracking(0.3)
                .foregroundStyle(UNESColor.ink4)
            Text(value)
                .font(.system(size: 15, weight: .bold))
                .tracking(-0.3)
                .monospacedDigit()
                .foregroundStyle(color)
        }
        .lineLimit(1)
        .minimumScaleFactor(0.8)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 8, leading: 10, bottom: 8, trailing: 10))
        .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

/// 45° hatching for a bar with no grade yet — the CSS
/// `repeating-linear-gradient` stripes.
private struct HatchedStripes: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        let stripe: CGFloat = 4
        let period = stripe * 2
        var x = -rect.height
        while x < rect.width {
            path.move(to: CGPoint(x: x, y: rect.maxY))
            path.addLine(to: CGPoint(x: x + rect.height, y: rect.minY))
            path.addLine(to: CGPoint(x: x + rect.height + stripe, y: rect.minY))
            path.addLine(to: CGPoint(x: x + stripe, y: rect.maxY))
            path.closeSubpath()
            x += period
        }
        return path
    }
}

#Preview {
    VStack(spacing: 16) {
        FCBreakdownCard(
            rows: [
                FCRow(id: "a", label: "VA1", scoreText: "8,5"),
                FCRow(id: "b", label: "VA2", scoreText: "5,2"),
                FCRow(id: "c", label: "Trab", scoreText: "2"),
                FCRow(id: "d", label: "VA4"),
            ],
            weighted: false
        )
        FCBreakdownCard(
            rows: [
                FCRow(id: "a", label: "VA1", scoreText: "8,5", weight: 2),
                FCRow(id: "b", label: "VA2", scoreText: "5,2", weight: 3),
            ],
            weighted: true
        )
    }
    .padding(16)
    .frame(maxHeight: .infinity)
    .background(UNESColor.surface)
}
