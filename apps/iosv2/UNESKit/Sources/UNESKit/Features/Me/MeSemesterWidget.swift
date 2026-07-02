import SwiftUI

/// The semester progress strip: one block per week, the current one pulsing.
struct MeSemesterWidget: View {
    var progress: SemesterProgress

    @State private var pulsing = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
                .padding(.bottom, 12)
            blocks
            footer
                .padding(.top, 10)
        }
        .padding(EdgeInsets(top: 15, leading: 16, bottom: 15, trailing: 16))
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
    }

    private var header: some View {
        HStack(alignment: .lastTextBaseline) {
            (
                Text(.meSemesterWeekPrefix)
                    + Text("\(progress.week)").foregroundStyle(UNESColor.accent)
                    + Text(.meSemesterWeekOfSuffix(progress.totalWeeks))
            )
            .font(.system(size: 16, weight: .bold))
            .tracking(-0.32)
            .foregroundStyle(UNESColor.ink)

            Spacer()

            Text("\(progress.percent)%")
                .font(.system(size: 13, weight: .semibold))
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink3)
        }
    }

    private var blocks: some View {
        HStack(spacing: 3) {
            ForEach(0..<progress.totalWeeks, id: \.self) { week in
                block(week: week)
            }
        }
        .onAppear {
            guard !reduceMotion else { return }
            withAnimation(.easeInOut(duration: 0.9).repeatForever(autoreverses: true)) {
                pulsing = true
            }
        }
    }

    @ViewBuilder
    private func block(week: Int) -> some View {
        let done = week < progress.week - 1
        let current = week == progress.week - 1
        RoundedRectangle(cornerRadius: 5, style: .continuous)
            .fill(done ? UNESColor.ink : current ? UNESColor.accent : UNESColor.surface3)
            .opacity(done ? 0.32 + Double(week) / Double(progress.totalWeeks) * 0.5 : 1)
            .frame(height: 22)
            .overlay {
                if current {
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .strokeBorder(UNESColor.accent, lineWidth: 1.5)
                        .padding(-3)
                        .opacity(pulsing ? 0.15 : 0.5)
                }
            }
    }

    private var footer: some View {
        HStack {
            Text(dateLabel(.meSemesterStart, stamp: progress.startStamp))
            Spacer()
            Text(dateLabel(.meSemesterEnd, stamp: progress.endStamp))
        }
        .font(.system(size: 11, weight: .medium))
        .foregroundStyle(UNESColor.ink4)
    }

    private func dateLabel(_ prefix: LocalizedStringResource, stamp: String) -> String {
        let label = String.localized(prefix)
        guard let date = HomeFormat.shortDate(fromDayStamp: stamp) else { return label }
        return "\(label) · \(date)"
    }
}

#Preview {
    MeSemesterWidget(progress: MeOverview.preview.progress!)
        .padding(16)
        .frame(maxHeight: .infinity)
        .background(UNESColor.surface)
}
