import SwiftUI

/// "◦ composição" card — one horizontal bar per row showing the absolute
/// score (0–10), followed by the three rule pills (piso final / aprovação /
/// fórmula final). Unfilled rows show a striped placeholder, wildcards get the
/// amber tint, and bars pick a color from the score band.
struct FCBreakdown: View {
    let rows: [FCRow]
    let weighted: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .firstTextBaseline) {
                Text("◦ COMPOSIÇÃO")
                    .font(UNESFont.mono(10, weight: .medium))
                    .tracking(1.2)
                    .foregroundStyle(UNESColor.ink3)
                Spacer()
                Text("NOTA / PESO")
                    .font(UNESFont.mono(9))
                    .tracking(0.72)
                    .foregroundStyle(UNESColor.ink4)
            }

            VStack(spacing: 8) {
                ForEach(rows) { row in
                    rowBar(row)
                }
            }

            Divider()
                .overlay(UNESColor.line)

            HStack(spacing: 6) {
                rulePill(label: "PISO FINAL", value: "3,0", tone: .coral)
                rulePill(label: "APROVAÇÃO", value: "7,0", tone: .green)
                rulePill(label: "FÓRMULA FINAL", value: "0,6m + 0,4f", tone: .teal)
            }
        }
        .padding(14)
        .cardSurface(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private func rowBar(_ row: FCRow) -> some View {
        HStack(spacing: 10) {
            HStack(spacing: 3) {
                if row.wildcard {
                    Image(systemName: "star.fill")
                        .font(.system(size: 9, weight: .semibold))
                        .foregroundStyle(Color(red: 0xF4/255, green: 0xA2/255, blue: 0x3C/255))
                }
                Text(row.label)
                    .font(UNESFont.mono(10, weight: .semibold))
                    .tracking(0.2)
                    .foregroundStyle(UNESColor.ink2)
            }
            .frame(width: 44, alignment: .leading)

            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 3, style: .continuous)
                        .fill(UNESColor.surface3)
                        .frame(height: 6)
                    if let score = row.score {
                        let pct = max(0, min(10, score)) / 10
                        RoundedRectangle(cornerRadius: 3, style: .continuous)
                            .fill(barColor(for: row))
                            .frame(width: geo.size.width * pct, height: 6)
                            .animation(.spring(response: 0.4, dampingFraction: 0.8), value: row.score)
                    } else {
                        stripedPlaceholder
                    }
                }
                .frame(height: 6)
            }
            .frame(height: 6)

            HStack(spacing: 4) {
                Text(row.score.map { FinalCountdownMath.formatGrade($0) } ?? "—")
                    .font(UNESFont.mono(11, weight: .medium))
                    .foregroundStyle(row.score != nil ? UNESColor.ink : UNESColor.ink4)
                if weighted {
                    Text("×\(row.weight)")
                        .font(UNESFont.mono(9))
                        .foregroundStyle(UNESColor.ink4)
                }
            }
            .frame(width: 56, alignment: .trailing)
        }
    }

    private var stripedPlaceholder: some View {
        Canvas { ctx, size in
            let step: CGFloat = 8
            for x in stride(from: -size.height, to: size.width, by: step) {
                var stripe = Path()
                stripe.move(to: CGPoint(x: x, y: 0))
                stripe.addLine(to: CGPoint(x: x + 4, y: 0))
                stripe.addLine(to: CGPoint(x: x + 4 + size.height, y: size.height))
                stripe.addLine(to: CGPoint(x: x + size.height, y: size.height))
                stripe.closeSubpath()
                ctx.fill(stripe, with: .color(UNESColor.ink4.opacity(0.2)))
            }
        }
        .frame(height: 6)
        .clipShape(RoundedRectangle(cornerRadius: 3, style: .continuous))
    }

    private func rulePill(label: String, value: String, tone: FCTone) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(UNESFont.mono(8))
                .tracking(0.8)
                .foregroundStyle(UNESColor.ink4)
            Text(value)
                .font(UNESFont.serif(13))
                .tracking(-0.13)
                .foregroundStyle(tone.bg)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 9)
        .padding(.vertical, 7)
        .background(
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .fill(tone.bg.opacity(tone == .amber ? 0.14 : tone == .green ? 0.12 : 0.1))
        )
    }

    private func barColor(for row: FCRow) -> Color {
        if row.wildcard { return Color(red: 0xF4/255, green: 0xA2/255, blue: 0x3C/255) }
        guard let score = row.score else { return UNESColor.ink4 }
        if score >= 7 { return FCTone.green.bg }
        if score >= 3 { return FCTone.amber.bg }
        return FCTone.coral.bg
    }
}
