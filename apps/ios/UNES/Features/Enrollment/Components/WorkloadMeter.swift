import SwiftUI

// UNES — running-workload gauge: the selected total against the window's
// min/max band, colored by whether the proposal is under, within, or over the
// allowed range. Ported from `WorkloadMeter` in `screens-matricula-ui.jsx`.
struct WorkloadMeter: View {
    let total: Int
    let min: Int
    let max: Int
    var compact: Bool = false

    private var isUnder: Bool { total < min }
    private var isOver: Bool { total > max }
    private var isOK: Bool { !isUnder && !isOver && total > 0 }

    private var color: Color {
        if isOver { return EnrollmentPalette.danger }
        if isUnder { return EnrollmentPalette.warnSolid }
        if total == 0 { return UNESColor.ink4 }
        return EnrollmentPalette.okSolid
    }

    private var status: String {
        if total == 0 { return "comece a montar" }
        if isOver { return "acima do limite" }
        if isUnder { return "faltam \(min - total)h" }
        return "dentro do limite"
    }

    private var trackMax: Int { Swift.max(max, total) }

    private func fraction(_ value: Int) -> CGFloat {
        guard trackMax > 0 else { return 0 }
        return Swift.min(1, CGFloat(value) / CGFloat(trackMax))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: compact ? 10 : 12) {
            HStack(alignment: .bottom) {
                VStack(alignment: .leading, spacing: 6) {
                    EnrollmentEyebrow(text: "carga horária")
                    HStack(alignment: .firstTextBaseline, spacing: 6) {
                        Text("\(total)")
                            .font(UNESFont.sans(compact ? 28 : 38, weight: .bold))
                            .tracking(compact ? -0.98 : -1.33)
                            .foregroundStyle(color)
                        Text("h selecionadas")
                            .font(UNESFont.sans(13))
                            .foregroundStyle(UNESColor.ink3)
                    }
                }
                Spacer(minLength: 8)
                statusPill
            }
            track
            HStack {
                Text("mín \(min)h")
                Spacer()
                Text("máx \(max)h")
            }
            .font(UNESFont.mono(9))
            .foregroundStyle(UNESColor.ink4)
        }
        .padding(.horizontal, compact ? 14 : 18)
        .padding(.vertical, compact ? 12 : 16)
        .cardSurface(RoundedRectangle(cornerRadius: compact ? 16 : 20, style: .continuous))
    }

    private var statusPill: some View {
        HStack(spacing: 5) {
            if isOK {
                Image(systemName: "checkmark")
                    .font(.system(size: 9, weight: .bold))
            }
            Text(status.uppercased())
                .font(UNESFont.mono(9.5, weight: .semibold))
                .tracking(0.76)
        }
        .foregroundStyle(color)
        .padding(.horizontal, 9)
        .padding(.vertical, 5)
        .background(Capsule().fill(color.opacity(0.10)))
    }

    private var track: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let minX = fraction(min) * w
            let maxX = fraction(max) * w
            ZStack(alignment: .leading) {
                Capsule().fill(UNESColor.surface3)
                // Valid band between min and max.
                Rectangle()
                    .fill(EnrollmentPalette.okSolid.opacity(0.15))
                    .frame(width: Swift.max(0, maxX - minX))
                    .offset(x: minX)
                // Current fill.
                Capsule()
                    .fill(color)
                    .frame(width: fraction(total) * w)
                // Min / max markers.
                marker.offset(x: minX - 0.75)
                marker.offset(x: maxX - 0.75)
            }
            .frame(height: 8)
        }
        .frame(height: 8)
    }

    private var marker: some View {
        Rectangle()
            .fill(UNESColor.ink4)
            .frame(width: 1.5, height: 14)
    }
}
