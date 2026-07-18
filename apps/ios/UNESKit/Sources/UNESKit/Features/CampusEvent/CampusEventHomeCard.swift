import SwiftUI

/// The featured event card on Home: always-dark warm mesh with the event
/// identity, a phase-aware right block (countdown / day counter / ended) and
/// a date + call-to-action footer. The whole card is the tap target.
struct CampusEventHomeCard: View {
    let event: CampusEvent
    var onOpen: () -> Void

    var body: some View {
        Button(action: onOpen) {
            ZStack {
                UNESColor.darkBg
                MeshView(variant: .warm)
                LinearGradient.css(
                    stops: [
                        .init(color: UNESColor.scrim.opacity(0.08), location: 0),
                        .init(color: UNESColor.scrim.opacity(0.66), location: 1),
                    ],
                    angle: 155
                )

                TimelineView(.periodic(from: .now, by: 1)) { context in
                    content(now: context.date)
                }
            }
            .environment(\.colorScheme, .dark)
            .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        }
        .buttonStyle(.pressableCard)
        .shadow(color: Color(hex: 0x141020, opacity: 0.34), radius: 23, y: 20)
    }

    private func content(now: Date) -> some View {
        let phase = event.phase(now: now)
        return VStack(spacing: 0) {
            HStack(spacing: 7) {
                if phase != .ended { LiveDot() }
                Text(eyebrow(for: phase))
                    .textCase(.uppercase)
                    .font(.system(size: 11.5, weight: .bold))
                    .tracking(0.92)
            }
            .foregroundStyle(.white.opacity(0.9))
            .frame(maxWidth: .infinity, alignment: .leading)

            VStack(alignment: .leading, spacing: 0) {
                if let edition = event.edition {
                    Text(edition)
                        .font(.system(size: 13, weight: .bold))
                        .tracking(1.82)
                        .foregroundStyle(.white.opacity(0.72))
                }
                Text(event.name)
                    .font(.system(size: 36, weight: .heavy))
                    .tracking(-1.44)
                    .lineLimit(2)
                    .minimumScaleFactor(0.6)
                    .foregroundStyle(.white)
                    .padding(.top, 2)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.top, 16)

            HStack(alignment: .bottom, spacing: 14) {
                VStack(alignment: .leading, spacing: 0) {
                    if let tagline = event.tagline {
                        Text(tagline)
                            .font(.system(size: 13.5, weight: .medium))
                            .foregroundStyle(.white.opacity(0.82))
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                rightBlock(for: phase, now: now)
            }
            .padding(.top, 8)

            footer(for: phase)
                .padding(.top, 18)
        }
        .padding(EdgeInsets(top: 17, leading: 19, bottom: 18, trailing: 19))
    }

    private func eyebrow(for phase: CampusEventPhase) -> LocalizedStringResource {
        switch phase {
        case .upcoming: .campusEventCardUpcoming
        case .live: .campusEventPhaseLive
        case .ended: .campusEventPhaseEnded
        }
    }

    @ViewBuilder
    private func rightBlock(for phase: CampusEventPhase, now: Date) -> some View {
        switch phase {
        case .upcoming:
            let countdown = CampusEventFormat.countdown(until: event.startsAt, now: now)
            rightColumn(label: .campusEventCardStartsIn) {
                HStack(alignment: .firstTextBaseline, spacing: 2) {
                    bigValue("\(countdown.days)")
                    smallUnit("d")
                    bigValue(CampusEventFormat.padded(countdown.hours))
                        .padding(.leading, 3)
                    smallUnit("h")
                }
            }
        case .live:
            rightColumn(label: .campusEventCardDay) {
                HStack(alignment: .firstTextBaseline, spacing: 1) {
                    bigValue("\(event.dayNumber(now: now))")
                    Text("/\(event.dayCount)")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(.white.opacity(0.7))
                }
            }
        case .ended:
            Text(.campusEventCardEnded)
                .font(.system(size: 18, weight: .heavy))
                .tracking(-0.54)
                .foregroundStyle(.white)
        }
    }

    private func rightColumn(label: LocalizedStringResource, @ViewBuilder value: () -> some View) -> some View {
        VStack(alignment: .trailing, spacing: 4) {
            Text(label)
                .textCase(.uppercase)
                .font(.system(size: 10.5, weight: .semibold))
                .tracking(0.63)
                .foregroundStyle(.white.opacity(0.55))
            value()
        }
    }

    private func bigValue(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 30, weight: .bold))
            .tracking(-1.2)
            .monospacedDigit()
            .foregroundStyle(.white)
    }

    private func smallUnit(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 15, weight: .semibold))
            .foregroundStyle(.white.opacity(0.7))
    }

    private func footer(for phase: CampusEventPhase) -> some View {
        HStack(spacing: 10) {
            Image(systemName: "calendar")
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(.white.opacity(0.7))
            Text(CampusEventFormat.dateRange(from: event.startsAt, to: event.endsAt, in: event.timeZone))
                .font(.system(size: 13.5, weight: .semibold))
                .foregroundStyle(.white.opacity(0.9))

            Spacer(minLength: 12)

            HStack(spacing: 6) {
                Text(callToAction(for: phase))
                    .font(.system(size: 13.5, weight: .semibold))
                Image(systemName: "arrow.right")
                    .font(.system(size: 12, weight: .semibold))
                    .frame(width: 26, height: 26)
                    .background(.white.opacity(0.18), in: Circle())
            }
            .foregroundStyle(.white)
        }
        .padding(.top, 14)
        .overlay(alignment: .top) {
            Rectangle()
                .fill(.white.opacity(0.15))
                .frame(height: 1)
        }
    }

    private func callToAction(for phase: CampusEventPhase) -> LocalizedStringResource {
        switch phase {
        case .upcoming: .campusEventCardOpen
        case .live: .campusEventCardFollow
        case .ended: .campusEventCardRelive
        }
    }
}

#Preview("Antes") {
    CampusEventHomeCard(event: .preview(phase: .upcoming), onOpen: {})
        .padding(16)
        .frame(maxHeight: .infinity)
        .background(UNESColor.surface)
}

#Preview("Ao vivo") {
    CampusEventHomeCard(event: .preview(phase: .live), onOpen: {})
        .padding(16)
        .frame(maxHeight: .infinity)
        .background(UNESColor.surface)
}

#Preview("Encerrado") {
    CampusEventHomeCard(event: .preview(phase: .ended), onOpen: {})
        .padding(16)
        .frame(maxHeight: .infinity)
        .background(UNESColor.surface)
}
