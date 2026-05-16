import SwiftUI

/// Segmented bar showing absences used vs. total allowed. Color escalates
/// from ink-3 → amber → coral as the ratio crosses 50% and 75%.
struct AbsenceBar: View {
    let used: Int
    let allowed: Int

    private var ratio: Double {
        guard allowed > 0 else { return 0 }
        return Double(used) / Double(allowed)
    }

    private var tone: Color {
        if ratio >= 0.75 { return DisciplineScoreColor.danger }
        if ratio >= 0.50 { return DisciplineScoreColor.caution }
        return UNESColor.ink3
    }

    private var remaining: Int { max(0, allowed - used) }

    var body: some View {
        HStack(spacing: 8) {
            HStack(spacing: 2.5) {
                ForEach(0..<allowed, id: \.self) { i in
                    RoundedRectangle(cornerRadius: 2, style: .continuous)
                        .fill(i < used ? tone : UNESColor.line)
                        .opacity(i < used ? 1 : 0.5)
                        .frame(height: 5)
                }
            }

            Text("\(remaining) restantes")
                .font(UNESFont.mono(10, weight: .semibold))
                .foregroundStyle(tone)
                .fixedSize(horizontal: true, vertical: false)
        }
    }
}

#Preview {
    VStack(spacing: 12) {
        AbsenceBar(used: 2, allowed: 15)
        AbsenceBar(used: 8, allowed: 15)
        AbsenceBar(used: 12, allowed: 15)
    }
    .padding()
    .background(UNESColor.surface)
}
