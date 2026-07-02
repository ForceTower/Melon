import SwiftUI

/// The dark mesh centerpiece: eyebrow + mode chip, verdict headline beside
/// the grade ring, and the stat + explainer footer. The `final` verdict adds
/// the 0–10 difficulty strip with a marker at the needed grade.
struct FCVerdictHero: View {
    let verdict: FCVerdict
    let style: FCVerdictStyle
    let weighted: Bool

    var body: some View {
        ZStack {
            UNESColor.darkBg
            MeshView(variant: style.mesh)
            LinearGradient.css(
                stops: [
                    .init(color: UNESColor.scrim.opacity(0.12), location: 0),
                    .init(color: UNESColor.scrim.opacity(0.64), location: 1),
                ],
                angle: 155
            )

            VStack(alignment: .leading, spacing: 0) {
                eyebrowRow
                headlineRow
                    .padding(.top, 16)
                statFooter
                    .padding(.top, 16)

                if verdict.kind == .final, let need = verdict.need {
                    difficultyStrip(need: need)
                        .padding(.top, 14)
                }
            }
            .padding(EdgeInsets(top: 18, leading: 20, bottom: 18, trailing: 20))
        }
        .environment(\.colorScheme, .dark)
        .clipShape(RoundedRectangle(cornerRadius: 30, style: .continuous))
        .shadow(color: Color(hex: 0x141020, opacity: 0.28), radius: 20, y: 18)
    }

    // MARK: Rows

    private var eyebrowRow: some View {
        HStack {
            HStack(spacing: 8) {
                Image(systemName: style.icon)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(UNESColor.darkBg)
                    .frame(width: 24, height: 24)
                    .background(style.hue, in: RoundedRectangle(cornerRadius: 7, style: .continuous))

                Text(style.eyebrow)
                    .textCase(.uppercase)
                    .font(.system(size: 12, weight: .semibold))
                    .tracking(0.5)
                    .foregroundStyle(.white.opacity(0.92))
            }

            Spacer()

            Text(weighted ? .finalCountdownHeroWeightedBadge : .finalCountdownHeroSimpleBadge)
                .font(.system(size: 11.5, weight: .semibold))
                .foregroundStyle(.white.opacity(0.55))
                .padding(.horizontal, 10)
                .padding(.vertical, 4)
                .background(.white.opacity(0.1), in: Capsule())
        }
    }

    private var headlineRow: some View {
        HStack(spacing: 16) {
            VStack(alignment: .leading, spacing: 6) {
                // fixedSize lets the titles wrap beside the ring — the HStack
                // would otherwise compress them into an ellipsis.
                Text(style.title)
                    .font(.system(size: 30, weight: .bold))
                    .tracking(-1.05)
                    .foregroundStyle(.white)
                    .fixedSize(horizontal: false, vertical: true)
                Text(style.lead)
                    .font(.system(size: 14, weight: .medium))
                    .tracking(-0.14)
                    .foregroundStyle(.white.opacity(0.72))
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            FCVerdictRing(avg: verdict.avg, hue: style.hue)
        }
    }

    private var statFooter: some View {
        HStack(alignment: .center, spacing: 14) {
            VStack(alignment: .leading, spacing: 3) {
                Text(style.statLabel)
                    .textCase(.uppercase)
                    .font(.system(size: 10.5, weight: .semibold))
                    .tracking(0.5)
                    .foregroundStyle(.white.opacity(0.52))
                Text(style.statValue)
                    .font(.system(size: 26, weight: .bold))
                    .tracking(-0.78)
                    .monospacedDigit()
                    .foregroundStyle(style.hue)
            }
            .layoutPriority(1)

            Text(style.detail)
                .font(.system(size: 12.5, weight: .medium))
                .lineSpacing(3)
                .foregroundStyle(.white.opacity(0.8))
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.top, 14)
        .overlay(alignment: .top) {
            Rectangle().fill(.white.opacity(0.14)).frame(height: 1)
        }
    }

    /// "fácil · 0 — cruel · 5 — brutal · 10" with the needed grade marked.
    private func difficultyStrip(need: Double) -> some View {
        VStack(spacing: 6) {
            GeometryReader { geometry in
                let fraction = min(max(need / 10, 0), 1)
                LinearGradient(
                    stops: [
                        .init(color: Color(hex: 0x4FD69C, opacity: 0.55), location: 0),
                        .init(color: Color(hex: 0xF4B54C, opacity: 0.75), location: 0.5),
                        .init(color: Color(hex: 0xF0805E, opacity: 0.9), location: 1),
                    ],
                    startPoint: .leading,
                    endPoint: .trailing
                )
                .clipShape(Capsule())
                .overlay {
                    RoundedRectangle(cornerRadius: 2, style: .continuous)
                        .fill(.white)
                        .frame(width: 3, height: 14)
                        .background {
                            RoundedRectangle(cornerRadius: 4, style: .continuous)
                                .fill(.white.opacity(0.25))
                                .frame(width: 9, height: 20)
                        }
                        .position(x: fraction * geometry.size.width, y: geometry.size.height / 2)
                }
            }
            .frame(height: 6)

            HStack {
                Text(.finalCountdownHeroEasyMark)
                Spacer()
                Text(.finalCountdownHeroCruelMark)
                Spacer()
                Text(.finalCountdownHeroBrutalMark)
            }
            .textCase(.uppercase)
            .font(.system(size: 10, weight: .semibold))
            .tracking(0.4)
            .foregroundStyle(.white.opacity(0.5))
        }
        .padding(.top, 4)
    }
}

// MARK: - Ring

/// The 0–10 verdict arc: a track opening at the bottom, the average filling
/// it in the verdict hue, and a fixed tick at the 7,0 pass mark.
struct FCVerdictRing: View {
    var avg: Double?
    var hue: Color
    var size: CGFloat = 98
    var stroke: CGFloat = 8

    /// Fraction of the circle left open at the bottom.
    private let gap = 0.26
    private var arcFraction: Double { 1 - gap }
    /// Degrees rotating the arc start so the opening faces down.
    private var rotation: Double { 90 + gap / 2 * 360 }

    var body: some View {
        let radius = (size - stroke) / 2
        let fraction = avg.map { min(max($0, 0), 10) / 10 } ?? 0

        ZStack {
            Circle()
                .trim(from: 0, to: arcFraction)
                .stroke(.white.opacity(0.14), style: StrokeStyle(lineWidth: stroke, lineCap: .round))
                .rotationEffect(.degrees(rotation))
                .padding(stroke / 2)

            if avg != nil {
                Circle()
                    .trim(from: 0, to: arcFraction * fraction)
                    .stroke(hue, style: StrokeStyle(lineWidth: stroke, lineCap: .round))
                    .rotationEffect(.degrees(rotation))
                    .padding(stroke / 2)
                    .animation(UNESMotion.ease(0.7), value: fraction)
            }

            // Pass tick at 7,0.
            Capsule()
                .fill(.white.opacity(0.7))
                .frame(width: stroke + 6, height: 2)
                .offset(x: radius)
                .rotationEffect(.degrees(rotation + 0.7 * arcFraction * 360))

            VStack(spacing: 3) {
                Text(FinalCountdownMath.formatGrade(avg))
                    .font(.system(size: 30, weight: .bold))
                    .tracking(-1.2)
                    .monospacedDigit()
                    .foregroundStyle(.white)
                Text(.finalCountdownHeroAverageLabel)
                    .font(.system(size: 9.5, weight: .semibold))
                    .tracking(0.4)
                    .foregroundStyle(.white.opacity(0.62))
            }
        }
        .frame(width: size, height: size)
    }
}

#Preview {
    let rows: [FCRow] = [
        FCRow(id: "a", label: "VA1", scoreText: "6,5"),
        FCRow(id: "b", label: "VA2", scoreText: "5,2"),
        FCRow(id: "c", label: "Trab"),
    ]
    let verdict = FinalCountdownMath.verdict(for: rows, weighted: false)
    let finalVerdict = FinalCountdownMath.verdict(
        for: [
            FCRow(id: "a", label: "VA1", scoreText: "5,5"),
            FCRow(id: "b", label: "VA2", scoreText: "4"),
            FCRow(id: "c", label: "Trab", scoreText: "6,2"),
        ],
        weighted: false
    )
    ScrollView {
        VStack(spacing: 16) {
            FCVerdictHero(verdict: verdict, style: verdict.style(), weighted: false)
            FCVerdictHero(verdict: finalVerdict, style: finalVerdict.style(), weighted: true)
        }
        .padding(16)
    }
    .background(UNESColor.surface)
}
