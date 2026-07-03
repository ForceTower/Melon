import SwiftUI

/// Live Activity-style hero: the next class over a drifting cool mesh, with a
/// ticking countdown. The whole card is the tap target.
struct HomeHeroCard: View {
    let hero: HomeHeroClass
    var onDetails: () -> Void

    var body: some View {
        Button(action: onDetails) {
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

                TimelineView(.periodic(from: .now, by: 1)) { context in
                    VStack(spacing: 0) {
                        eyebrowRow(now: context.date)
                        HStack(alignment: .bottom, spacing: 16) {
                            titleColumn
                            if !inProgress {
                                countdown(now: context.date)
                            }
                        }
                        .padding(.top, 16)
                        if let endsAt = hero.endsAt, inProgress {
                            progressSection(endsAt: endsAt, now: context.date)
                                .padding(.top, 16)
                        }
                        footer
                            .padding(.top, 18)
                    }
                    .padding(EdgeInsets(top: 18, leading: 20, bottom: 20, trailing: 20))
                }
            }
            .environment(\.colorScheme, .dark)
            .clipShape(RoundedRectangle(cornerRadius: 30, style: .continuous))
        }
        .buttonStyle(.pressableCard)
        .disabled(hero.disciplineId == nil)
        .shadow(color: Color(hex: 0x141020, opacity: 0.28), radius: 20, y: 18)
    }

    private var inProgress: Bool {
        hero.isInProgress && hero.endsAt != nil
    }

    private func eyebrowRow(now: Date) -> some View {
        HStack {
            HStack(spacing: 7) {
                LiveDot()
                Text(eyebrowLabel(now: now))
                    .textCase(.uppercase)
                    .font(.system(size: 12, weight: .semibold))
                    .tracking(0.2)
                    .monospacedDigit()
            }
            .foregroundStyle(.white.opacity(0.9))

            Spacer()

            Text(timeRange)
                .font(.system(size: 13, weight: .medium))
                .monospacedDigit()
                .foregroundStyle(.white.opacity(0.6))
        }
    }

    private func eyebrowLabel(now: Date) -> String {
        guard inProgress, let endsAt = hero.endsAt else { return .localized(.homeHeroNextClass) }
        guard let left = HomeFormat.countdown(until: endsAt, now: now) else { return .localized(.commonNow) }
        let remaining = "\(left.big)\(left.unit.map { " \($0)" } ?? "")"
        return .localized(.homeHeroNowEndsIn(remaining))
    }

    /// The live class bar: elapsed fill with the started/ends labels under it.
    private func progressSection(endsAt: Date, now: Date) -> some View {
        let total = endsAt.timeIntervalSince(hero.startsAt)
        let fraction = total > 0 ? min(1, max(0, now.timeIntervalSince(hero.startsAt) / total)) : 0
        return VStack(spacing: 7) {
            GeometryReader { proxy in
                ZStack(alignment: .leading) {
                    Capsule().fill(.white.opacity(0.16))
                    Capsule()
                        .fill(UNESColor.liveGreen)
                        .frame(width: max(6, proxy.size.width * fraction))
                }
            }
            .frame(height: 6)
            HStack {
                Text(.homeHeroStarted(hero.startTime))
                Spacer()
                if let endTime = hero.endTime {
                    Text(.homeHeroEnded(endTime))
                }
            }
            .font(.system(size: 12, weight: .medium))
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
                        .font(.system(size: 13, weight: .medium))
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

    private func countdown(now: Date) -> some View {
        VStack(alignment: .trailing, spacing: 3) {
            if let countdown = HomeFormat.countdown(until: hero.startsAt, now: now) {
                Text(.homeHeroStartsIn)
                    .font(.system(size: 11, weight: .semibold))
                    .tracking(0.4)
                    .opacity(0.6)
                HStack(alignment: .firstTextBaseline, spacing: 3) {
                    Text(countdown.big)
                        .font(.system(size: 34, weight: .bold))
                        .tracking(-1.36)
                        .monospacedDigit()
                    if let unit = countdown.unit {
                        Text(unit)
                            .font(.system(size: 16, weight: .semibold))
                            .opacity(0.7)
                    }
                }
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

    private var footer: some View {
        HStack(spacing: 14) {
            if let room = hero.room {
                meta(icon: "mappin.and.ellipse", label: .localized(.commonRoom(room)))
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
                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 28, height: 28)
                    .background(.white.opacity(0.16), in: Circle())
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

    private var teacherLabel: String? {
        hero.teacherName.map(HomeFormat.teacherShort)
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

#Preview("Em aula") {
    var hero = HomeOverview.preview(now: .now.addingTimeInterval(-45 * 60)).hero!
    hero.isInProgress = true
    return NavigationStack {
        HomeHeroCard(hero: hero, onDetails: {})
            .padding(16)
            .frame(maxHeight: .infinity)
            .background(UNESColor.surface)
    }
}
