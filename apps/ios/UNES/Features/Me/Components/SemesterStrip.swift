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
                Text(identity.semesterEnd)
            }
            .font(UNESFont.mono(9))
            .tracking(0.72)
            .textCase(.uppercase)
            .foregroundStyle(UNESColor.ink4)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .cardSurface(RoundedRectangle(cornerRadius: 22, style: .continuous))
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

    private var phase: Phase {
        if index < currentWeek - 1 { return .done }
        if index == currentWeek - 1 { return .current }
        return .upcoming
    }

    enum Phase { case done, current, upcoming }

    /// Past weeks fade from dim → recent, so the eye reads a gradient
    /// tracking how close we are to "now" (same trick as the CSS prototype).
    private var doneOpacity: Double {
        0.35 + (Double(index) / Double(totalWeeks)) * 0.55
    }

    var body: some View {
        RoundedRectangle(cornerRadius: 4, style: .continuous)
            .fill(fillColor)
            .opacity(phase == .done ? doneOpacity : 1)
            .frame(maxWidth: .infinity)
            .frame(height: 22)
            .overlay {
                if phase == .current { currentRing }
            }
    }

    /// Pulse driven by `TimelineView(.animation)` so opacity and scale are
    /// both computed explicitly per frame instead of going through SwiftUI's
    /// implicit `.animation(value:)` + `repeatForever` path. The scale only
    /// reads as diagonal when its anchor disagrees with the rendered center
    /// (which happened earlier with `.padding(-3)` on a non-square block);
    /// here the ring's size is pinned via `.frame(w+6, h+6)` and the scale
    /// applies inside that known frame with an explicit `.center` anchor.
    private var currentRing: some View {
        GeometryReader { proxy in
            TimelineView(.animation) { timeline in
                let t = timeline.date.timeIntervalSinceReferenceDate
                let cycle = 1.6
                let phase = t.truncatingRemainder(dividingBy: cycle) / cycle
                let ease = (1 - cos(phase * 2 * .pi)) / 2
                let alpha = 0.55 - 0.3 * ease
                let scale = 1.0 - 0.05 * ease

                RoundedRectangle(cornerRadius: 6, style: .continuous)
                    .strokeBorder(UNESColor.accent.opacity(alpha), lineWidth: 1.5)
                    .frame(width: proxy.size.width + 6, height: proxy.size.height + 6)
                    .scaleEffect(scale, anchor: .center)
                    .position(x: proxy.size.width / 2, y: proxy.size.height / 2)
            }
        }
    }

    private var fillColor: Color {
        switch phase {
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
