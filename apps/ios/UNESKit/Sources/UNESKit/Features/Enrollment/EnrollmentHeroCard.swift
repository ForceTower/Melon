import SwiftUI

/// The live-activity hero of the entry screen: window state over the dark
/// mesh, the days-left ring while open, and the open/close bounds.
struct EnrollmentHeroCard: View {
    var window: EnrollmentWindow
    var now: Date

    var body: some View {
        ZStack {
            UNESColor.darkBg
            MeshView(variant: window.state == .closed ? .fresh : .cool)
            LinearGradient.css(
                stops: [
                    .init(color: UNESColor.scrim.opacity(0.12), location: 0),
                    .init(color: UNESColor.scrim.opacity(0.62), location: 1),
                ],
                angle: 155
            )

            VStack(alignment: .leading, spacing: 0) {
                statusRow
                headlineRow
                    .padding(.top, 16)
                boundsFooter
                    .padding(.top, 18)
            }
            .padding(EdgeInsets(top: 18, leading: 20, bottom: 20, trailing: 20))
        }
        .environment(\.colorScheme, .dark)
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .shadow(color: Color(hex: 0x141020, opacity: 0.26), radius: 20, y: 18)
    }

    // MARK: Rows

    private var statusRow: some View {
        HStack {
            HStack(spacing: 7) {
                LiveDot(color: window.state == .upcoming ? UNESColor.amber : UNESColor.liveGreen)
                Text(statusLabel)
                    .textCase(.uppercase)
                    .font(.system(size: 12, weight: .semibold))
                    .tracking(0.3)
                    .foregroundStyle(.white.opacity(0.9))
            }
            Spacer()
            Text(window.semester)
                .font(.system(size: 13, weight: .medium))
                .monospacedDigit()
                .foregroundStyle(.white.opacity(0.62))
        }
    }

    private var headlineRow: some View {
        HStack(alignment: .bottom, spacing: 16) {
            VStack(alignment: .leading, spacing: 8) {
                Text(headline)
                    .font(.system(size: 27, weight: .bold))
                    .tracking(-0.81)
                    .foregroundStyle(.white)
                    .fixedSize(horizontal: false, vertical: true)
                Text(lead)
                    .font(.system(size: 13.5, weight: .medium))
                    .lineSpacing(3)
                    .foregroundStyle(.white.opacity(0.8))
                    .frame(maxWidth: 210, alignment: .leading)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            if window.state == .open {
                EnrollmentRing(fraction: window.remainingFraction(now: now)) {
                    VStack(spacing: 1) {
                        Text(verbatim: "\(window.daysLeft(now: now))")
                            .font(.system(size: 26, weight: .bold))
                            .tracking(-0.78)
                            .monospacedDigit()
                        Text(.commonUnitDays)
                            .font(.system(size: 9.5, weight: .semibold))
                            .tracking(0.4)
                            .opacity(0.72)
                    }
                    .foregroundStyle(.white)
                }
            }
            if window.state == .closed {
                Image(systemName: "checkmark")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(width: 62, height: 62)
                    .background(.white.opacity(0.16), in: Circle())
                    .overlay {
                        Circle().strokeBorder(.white.opacity(0.28))
                    }
            }
        }
    }

    private var boundsFooter: some View {
        HStack(spacing: 0) {
            bound(.localized(.enrollmentHeroOpensLabel), EnrollmentFormat.dayLabel(window.startDate))
            Rectangle()
                .fill(.white.opacity(0.18))
                .frame(width: 1)
                .padding(.horizontal, 16)
            bound(.localized(.enrollmentHeroClosesLabel), EnrollmentFormat.endLabel(window.endDate))
        }
        .fixedSize(horizontal: false, vertical: true)
        .padding(.top, 14)
        .overlay(alignment: .top) {
            Rectangle().fill(.white.opacity(0.15)).frame(height: 1)
        }
    }

    private func bound(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(label)
                .textCase(.uppercase)
                .font(.system(size: 10.5, weight: .semibold))
                .tracking(0.4)
                .foregroundStyle(.white.opacity(0.6))
            Text(value)
                .font(.system(size: 16, weight: .semibold))
                .tracking(-0.16)
                .monospacedDigit()
                .foregroundStyle(.white)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // MARK: Copy

    private var statusLabel: String {
        switch window.state {
        case .open: .localized(.enrollmentHeroStatusOpen)
        case .upcoming: .localized(.enrollmentHeroStatusUpcoming)
        case .closed: .localized(.enrollmentProposalSent)
        }
    }

    private var headline: String {
        switch window.state {
        case .open: .localized(.enrollmentHeroHeadlineOpen(EnrollmentFormat.dayLabel(window.endDate)))
        case .upcoming: .localized(.enrollmentHeroHeadlineUpcoming(EnrollmentFormat.dayLabel(window.startDate)))
        case .closed: .localized(.enrollmentHeroHeadlineClosed)
        }
    }

    private var lead: String {
        switch window.state {
        case .open: .localized(.enrollmentHeroLeadOpen)
        case .upcoming: .localized(.enrollmentHeroLeadUpcoming)
        case .closed: .localized(.enrollmentHeroLeadClosed)
        }
    }
}

/// The countdown ring: a full track with the remaining share drawn on top,
/// animated in on appearance.
struct EnrollmentRing<Content: View>: View {
    var fraction: Double
    var size: CGFloat = 78
    var stroke: CGFloat = 6
    @ViewBuilder var content: Content

    @State private var drawn = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        ZStack {
            Circle()
                .stroke(.white.opacity(0.18), lineWidth: stroke)
            Circle()
                .trim(from: 0, to: drawn ? fraction : 0)
                .stroke(.white, style: StrokeStyle(lineWidth: stroke, lineCap: .round))
                .rotationEffect(.degrees(-90))
            content
        }
        .padding(stroke / 2)
        .frame(width: size, height: size)
        .onAppear {
            guard !drawn else { return }
            if reduceMotion {
                drawn = true
            } else {
                withAnimation(UNESMotion.settle(0.9).delay(0.35)) {
                    drawn = true
                }
            }
        }
    }
}

/// One of the disciplinas / conflitos / em fila tiles.
struct EnrollmentStatTile: View {
    var label: String
    var value: Int
    var hint: String
    var tone: Color = UNESColor.ink

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(label)
                .textCase(.uppercase)
                .font(.system(size: 10, weight: .semibold))
                .tracking(0.4)
                .foregroundStyle(UNESColor.ink3)
            Text(verbatim: "\(value)")
                .font(.system(size: 28, weight: .bold))
                .tracking(-1.12)
                .monospacedDigit()
                .foregroundStyle(tone)
                .padding(.top, 8)
                .contentTransition(.numericText())
                .animation(UNESMotion.settle(0.4), value: value)
            Text(hint)
                .font(.system(size: 11.5, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
                .padding(.top, 3)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 13, leading: 13, bottom: 12, trailing: 13))
        .enrollmentCard(radius: 18)
    }
}

#Preview {
    ScrollView {
        VStack(spacing: 16) {
            EnrollmentHeroCard(window: .preview, now: EnrollmentWindow.preview.startDate!.addingTimeInterval(2 * 86_400))
            EnrollmentHeroCard(window: {
                var window = EnrollmentWindow.preview
                window.state = .upcoming
                return window
            }(), now: .now)
            EnrollmentHeroCard(window: {
                var window = EnrollmentWindow.preview
                window.state = .closed
                return window
            }(), now: .now)
            HStack(spacing: 12) {
                EnrollmentStatTile(label: .localized(.enrollmentStatDisciplines), value: 4, hint: .localized(.enrollmentStatClassesHintOther))
                EnrollmentStatTile(label: .localized(.enrollmentStatConflicts), value: 1, hint: .localized(.enrollmentStatResolve), tone: EnrollmentTone.danger)
                EnrollmentStatTile(label: .localized(.enrollmentStatQueue), value: 0, hint: .localized(.enrollmentStatWaitingHint))
            }
        }
        .padding(16)
    }
    .background(UNESColor.surface)
}
