import SwiftUI

/// Live Activity-style hero: the next class over a drifting cool mesh, with a
/// ticking countdown.
struct HomeHeroCard: View {
    let hero: HomeHeroClass
    var onDetails: () -> Void

    var body: some View {
        ZStack {
            UNESColor.darkBg
            MeshView(variant: .cool)
            LinearGradient.css(
                stops: [
                    .init(color: UNESColor.scrim.opacity(0.15), location: 0),
                    .init(color: UNESColor.scrim.opacity(0.62), location: 1),
                ],
                angle: 155
            )

            VStack(spacing: 0) {
                eyebrowRow
                HStack(alignment: .bottom, spacing: 16) {
                    titleColumn
                    countdown
                }
                .padding(.top, 16)
                footer
                    .padding(.top, 18)
            }
            .padding(EdgeInsets(top: 18, leading: 20, bottom: 20, trailing: 20))
        }
        .environment(\.colorScheme, .dark)
        .clipShape(RoundedRectangle(cornerRadius: 30, style: .continuous))
        .shadow(color: Color(hex: 0x141020, opacity: 0.28), radius: 20, y: 18)
    }

    private var eyebrowRow: some View {
        HStack {
            HStack(spacing: 7) {
                LiveDot()
                Text("Próxima aula")
                    .textCase(.uppercase)
                    .font(.system(size: 12, weight: .semibold))
                    .tracking(0.2)
            }
            .foregroundStyle(.white.opacity(0.9))

            Spacer()

            Text(timeRange)
                .font(.system(size: 13, weight: .medium))
                .monospacedDigit()
                .foregroundStyle(.white.opacity(0.6))
        }
    }

    private var titleColumn: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(hero.disciplineName)
                .font(.system(size: 30, weight: .bold))
                .tracking(-0.9)
                .lineSpacing(1)
                .foregroundStyle(.white)
                .lineLimit(2)
                .minimumScaleFactor(0.8)

            if let topic = hero.topic {
                HStack(spacing: 7) {
                    Image(systemName: "bell")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(.white.opacity(0.6))
                    Text(topic)
                        .font(.system(size: 14))
                        .foregroundStyle(.white.opacity(0.82))
                        .lineLimit(1)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var countdown: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            VStack(alignment: .trailing, spacing: 2) {
                if let countdown = HomeFormat.countdown(until: hero.startsAt, now: context.date) {
                    HStack(alignment: .firstTextBaseline, spacing: 3) {
                        Text(countdown.big)
                            .font(.system(size: 40, weight: .bold))
                            .tracking(-1.6)
                            .monospacedDigit()
                        if let unit = countdown.unit {
                            Text(unit)
                                .font(.system(size: 18, weight: .semibold))
                                .opacity(0.7)
                        }
                    }
                    Text(countdown.sub)
                        .font(.system(size: 11, weight: .semibold))
                        .tracking(0.4)
                        .monospacedDigit()
                        .opacity(0.6)
                } else {
                    // Beyond a day out, the weekday reads better than a timer.
                    Text(HomeFormat.weekdayShort(for: hero.startsAt))
                        .font(.system(size: 34, weight: .bold))
                        .tracking(-1.36)
                    Text(hero.startTime)
                        .font(.system(size: 11, weight: .semibold))
                        .tracking(0.4)
                        .monospacedDigit()
                        .opacity(0.6)
                }
            }
            .foregroundStyle(.white)
        }
    }

    private var footer: some View {
        HStack(spacing: 14) {
            if let room = hero.room {
                meta(icon: "mappin.and.ellipse", label: "Sala \(room)")
            }
            if hero.room != nil && teacherLabel != nil {
                Rectangle()
                    .fill(.white.opacity(0.2))
                    .frame(width: 1, height: 13)
            }
            if let teacherLabel {
                meta(icon: "person", label: teacherLabel)
            }

            Spacer(minLength: 12)

            if hero.disciplineId != nil {
                Button(action: onDetails) {
                    Text("Detalhes")
                        .font(.system(size: 13, weight: .semibold))
                        .tracking(-0.13)
                        .foregroundStyle(.white)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(.white.opacity(0.16), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.top, 14)
        .overlay(alignment: .top) {
            Rectangle()
                .fill(.white.opacity(0.14))
                .frame(height: 1)
        }
    }

    private func meta(icon: String, label: String) -> some View {
        HStack(spacing: 6) {
            Image(systemName: icon)
                .font(.system(size: 11, weight: .medium))
                .opacity(0.62)
            Text(label)
                .font(.system(size: 13, weight: .medium))
                .lineLimit(1)
        }
        .foregroundStyle(.white.opacity(0.88))
    }

    /// Full SAGRES names run long — first two names read naturally.
    private var teacherLabel: String? {
        hero.teacherName.map { $0.split(separator: " ").prefix(2).joined(separator: " ") }
    }

    private var timeRange: String {
        [hero.startTime, hero.endTime]
            .compactMap { $0?.isEmpty == false ? $0 : nil }
            .joined(separator: " – ")
    }
}

#Preview {
    NavigationStack {
        HomeHeroCard(hero: HomeOverview.preview().hero!, onDetails: {})
            .padding(16)
            .frame(maxHeight: .infinity)
            .background(UNESColor.surface)
    }
}
