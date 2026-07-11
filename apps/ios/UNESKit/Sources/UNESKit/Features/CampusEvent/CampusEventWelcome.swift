import SwiftUI

/// Fullscreen always-dark reveal shown the first time an event edition is
/// opened: staggered identity lockup over the warm mesh, then a phase-aware
/// middle block (countdown / live day / thanks) and the enter button.
struct CampusEventWelcomeView: View {
    let event: CampusEvent
    var onEnter: () -> Void

    var body: some View {
        ZStack(alignment: .topTrailing) {
            UNESColor.darkBg.ignoresSafeArea()
            MeshView(variant: .warm)
                .ignoresSafeArea()
            RadialGradient(
                colors: [.clear, UNESColor.scrim.opacity(0.72)],
                center: UnitPoint(x: 0.5, y: 0.08),
                startRadius: 120,
                endRadius: 620
            )
            .ignoresSafeArea()

            TimelineView(.periodic(from: .now, by: 1)) { context in
                content(now: context.date)
            }

            skipButton
        }
        .environment(\.colorScheme, .dark)
    }

    private var skipButton: some View {
        Button(action: onEnter) {
            Text(.campusEventWelcomeSkip)
                .font(.system(size: 13.5, weight: .semibold))
                .foregroundStyle(.white)
                .padding(EdgeInsets(top: 8, leading: 14, bottom: 8, trailing: 14))
                .background(.white.opacity(0.14), in: Capsule())
        }
        .buttonStyle(.plain)
        .padding(EdgeInsets(top: 12, leading: 0, bottom: 0, trailing: 18))
        .fadeIn(delay: 1.4)
    }

    private func content(now: Date) -> some View {
        let phase = event.phase(now: now)
        return VStack(spacing: 0) {
            Spacer()

            if let institution = event.institution {
                Text(institution)
                    .textCase(.uppercase)
                    .font(.system(size: 13, weight: .semibold))
                    .tracking(2.08)
                    .foregroundStyle(.white.opacity(0.7))
                    .fadeUp(delay: 0.15)
            }

            Text(.campusEventWelcomeGreeting)
                .font(.system(size: 20, weight: .semibold))
                .tracking(0.4)
                .foregroundStyle(.white.opacity(0.86))
                .padding(.top, 20)
                .fadeUp(delay: 0.35)

            VStack(spacing: 4) {
                if let edition = event.edition {
                    Text(edition)
                        .font(.system(size: 16, weight: .bold))
                        .tracking(5.44)
                        .foregroundStyle(.white.opacity(0.62))
                        .fadeUp(delay: 0.5)
                }
                Text(event.name)
                    .font(.system(size: 76, weight: .heavy))
                    .tracking(-3.8)
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)
                    .foregroundStyle(.white)
                    .shadow(color: .black.opacity(0.4), radius: 20, y: 12)
                    .popIn(delay: 0.66, from: 0.94, offsetY: 20, overshoot: 1.1)
            }
            .padding(.top, 10)

            if let tagline = event.tagline {
                Text(tagline)
                    .font(.system(size: 15.5, weight: .medium))
                    .foregroundStyle(.white.opacity(0.82))
                    .multilineTextAlignment(.center)
                    .lineSpacing(3)
                    .frame(maxWidth: 300)
                    .padding(.top, 16)
                    .fadeUp(delay: 0.86)
            }

            VStack(spacing: 12) {
                Text(midLabel(for: phase))
                    .textCase(.uppercase)
                    .font(.system(size: 11.5, weight: .semibold))
                    .tracking(1.15)
                    .foregroundStyle(.white.opacity(0.6))

                midBlock(for: phase, now: now)

                HStack(spacing: 7) {
                    Image(systemName: "calendar")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(.white.opacity(0.7))
                    Text(CampusEventFormat.dateRangeWithYear(from: event.startsAt, to: event.endsAt, in: event.timeZone))
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(.white.opacity(0.88))
                }
                .padding(.top, 2)
            }
            .padding(.top, 30)
            .fadeUp(delay: 1.06)

            Button(action: onEnter) {
                HStack(spacing: 9) {
                    Text(enterLabel(for: phase))
                        .font(.system(size: 16, weight: .bold))
                        .tracking(-0.16)
                    Image(systemName: "arrow.right")
                        .font(.system(size: 15, weight: .semibold))
                }
                .foregroundStyle(UNESColor.darkBg)
                .padding(EdgeInsets(top: 15, leading: 30, bottom: 15, trailing: 30))
                .background(.white, in: Capsule())
                .shadow(color: .black.opacity(0.35), radius: 20, y: 16)
            }
            .buttonStyle(.pressableCard)
            .padding(.top, 30)
            .fadeUp(delay: 1.28)

            Spacer()
        }
        .padding(.horizontal, 30)
        .frame(maxWidth: .infinity)
    }

    private func midLabel(for phase: CampusEventPhase) -> LocalizedStringResource {
        switch phase {
        case .upcoming: .campusEventWelcomeStartsIn
        case .live: .campusEventWelcomeHappening
        case .ended: .campusEventPhaseEnded
        }
    }

    @ViewBuilder
    private func midBlock(for phase: CampusEventPhase, now: Date) -> some View {
        switch phase {
        case .upcoming:
            glassPanel {
                CampusEventCountdownRow(target: event.startsAt, now: now)
            }
        case .live:
            glassPanel {
                HStack(spacing: 11) {
                    LiveDot(size: 9)
                    Text(.campusEventDayOf(event.dayNumber(now: now), event.dayCount))
                        .font(.system(size: 22, weight: .heavy))
                        .tracking(-0.66)
                        .foregroundStyle(.white)
                }
                .padding(.horizontal, 6)
            }
        case .ended:
            glassPanel {
                HStack(spacing: 11) {
                    Image(systemName: "checkmark")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundStyle(UNESColor.successOnDark)
                    Text(.campusEventWelcomeThanks)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(.white.opacity(0.9))
                }
                .padding(.horizontal, 2)
            }
        }
    }

    private func glassPanel(@ViewBuilder content: () -> some View) -> some View {
        content()
            .padding(EdgeInsets(top: 15, leading: 20, bottom: 15, trailing: 20))
            .background(.white.opacity(0.08), in: RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .strokeBorder(.white.opacity(0.14))
            }
    }

    private func enterLabel(for phase: CampusEventPhase) -> LocalizedStringResource {
        switch phase {
        case .upcoming: .campusEventWelcomeEnterUpcoming
        case .live: .campusEventWelcomeEnterLive
        case .ended: .campusEventWelcomeEnterEnded
        }
    }
}

#Preview("Antes") {
    CampusEventWelcomeView(event: .preview(phase: .upcoming), onEnter: {})
}

#Preview("Ao vivo") {
    CampusEventWelcomeView(event: .preview(phase: .live), onEnter: {})
}

#Preview("Encerrado") {
    CampusEventWelcomeView(event: .preview(phase: .ended), onEnter: {})
}
