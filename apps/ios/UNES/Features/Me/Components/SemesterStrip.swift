import SwiftUI

/// Semester progress card. Renders the total number of weeks as thin blocks;
/// completed weeks fade up as done, the current week pulses in the accent
/// color. Mirrors `SemesterStrip` in `screens-me.jsx`.
struct SemesterStrip: View {
    let identity: ProfileIdentity

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            header

            HStack(spacing: 3) {
                ForEach(0..<identity.semesterTotalWeeks, id: \.self) { index in
                    WeekBlock(
                        index: index,
                        totalWeeks: identity.semesterTotalWeeks,
                        currentWeek: identity.semesterWeek
                    )
                }
            }

            HStack {
                Text(identity.semesterStart)
                Spacer()
                Text(identity.finalExam)
            }
            .font(UNESFont.mono(9))
            .tracking(0.72)
            .textCase(.uppercase)
            .foregroundStyle(UNESColor.ink4)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(UNESColor.card)
                .overlay(
                    RoundedRectangle(cornerRadius: 22, style: .continuous)
                        .strokeBorder(UNESColor.cardLine, lineWidth: 1)
                )
        )
    }

    private var header: some View {
        HStack(alignment: .firstTextBaseline) {
            VStack(alignment: .leading, spacing: 2) {
                Text("◦ SEMESTRE \(identity.semester)")
                    .font(UNESFont.mono(10, weight: .medium))
                    .tracking(1.2)
                    .foregroundStyle(UNESColor.ink3)

                Text("\(Text("Semana ").foregroundStyle(UNESColor.ink))\(Text("\(identity.semesterWeek)").italic().foregroundStyle(UNESColor.accent))\(Text(" de \(identity.semesterTotalWeeks)").foregroundStyle(UNESColor.ink))")
                    .font(UNESFont.serif(19))
                    .tracking(-0.19)
            }

            Spacer()

            Text("\(identity.progressPct)%")
                .font(UNESFont.mono(10))
                .tracking(0.6)
                .foregroundStyle(UNESColor.ink3)
        }
    }
}

private struct WeekBlock: View {
    let index: Int
    let totalWeeks: Int
    let currentWeek: Int

    private var state: State {
        if index < currentWeek - 1 { return .done }
        if index == currentWeek - 1 { return .current }
        return .upcoming
    }

    enum State { case done, current, upcoming }

    /// Past weeks fade from dim → recent, so the eye reads a gradient
    /// tracking how close we are to "now" (same trick as the CSS prototype).
    private var doneOpacity: Double {
        0.35 + (Double(index) / Double(totalWeeks)) * 0.55
    }

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 4, style: .continuous)
                .fill(fillColor)
                .opacity(state == .done ? doneOpacity : 1)

            if state == .current {
                RoundedRectangle(cornerRadius: 6, style: .continuous)
                    .strokeBorder(UNESColor.accent.opacity(0.5), lineWidth: 1.5)
                    .padding(-3)
                    .pulseForever()
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: 22)
    }

    private var fillColor: Color {
        switch state {
        case .done:     return UNESColor.ink
        case .current:  return UNESColor.accent
        case .upcoming: return UNESColor.surface3
        }
    }
}

#Preview {
    ZStack {
        UNESColor.surface.ignoresSafeArea()
        SemesterStrip(identity: MeFixtures.identity)
            .padding(14)
    }
}
