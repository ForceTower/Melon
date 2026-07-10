import SwiftUI

/// The floating glass dock under the catalogue screens: the running hours
/// against the ceiling, the conflict/count line, and the step's actions.
struct EnrollmentDock: View {
    var session: EnrollmentSession
    var primaryLabel: String
    var onPrimary: () -> Void
    var secondaryLabel: String?
    var onSecondary: (() -> Void)?

    var body: some View {
        HStack(spacing: 12) {
            summary
            Spacer(minLength: 0)
            if let secondaryLabel {
                Button {
                    onSecondary?()
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "square.grid.2x2")
                            .font(.system(size: 13, weight: .semibold))
                        Text(secondaryLabel)
                            .font(.system(size: 14, weight: .semibold))
                            .tracking(-0.14)
                    }
                    .foregroundStyle(UNESColor.ink)
                    .padding(.horizontal, 15)
                    .frame(height: 44)
                    .background(UNESColor.card, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .overlay {
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .strokeBorder(UNESColor.line)
                    }
                }
                .buttonStyle(TilePressStyle())
            }

            Button {
                onPrimary()
            } label: {
                HStack(spacing: 6) {
                    Text(primaryLabel)
                        .font(.system(size: 14, weight: .semibold))
                        .tracking(-0.14)
                    Image(systemName: "arrow.right")
                        .font(.system(size: 12, weight: .semibold))
                }
                .foregroundStyle(.white)
                .padding(.horizontal, 18)
                .frame(height: 44)
                .background(UNESColor.accent, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                .shadow(color: UNESColor.accent.opacity(0.4), radius: 8, y: 6)
            }
            .buttonStyle(TilePressStyle())
        }
        .padding(EdgeInsets(top: 12, leading: 14, bottom: 12, trailing: 14))
        .enrollmentDockChrome()
    }

    private var summary: some View {
        // Hoisted once — totalHours and conflicts both rebuild the resolved
        // picks on every access.
        let total = session.totalHours
        let conflictCount = session.conflicts.count

        return VStack(alignment: .leading, spacing: 3) {
            HStack(alignment: .firstTextBaseline, spacing: 4) {
                Text(verbatim: "\(total)h")
                    .font(.system(size: 21, weight: .bold))
                    .tracking(-0.75)
                    .monospacedDigit()
                    .foregroundStyle(hoursColor(total: total))
                    .contentTransition(.numericText())
                    .animation(UNESMotion.settle(0.4), value: total)
                if let window = session.window {
                    Text(verbatim: "/ \(window.maxHours)")
                        .font(.system(size: 11, weight: .medium))
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink4)
                }
            }
            Text(statusLine(conflictCount: conflictCount))
                .font(.system(size: 11.5, weight: .medium))
                .foregroundStyle(conflictCount == 0 ? UNESColor.ink3 : EnrollmentTone.danger)
        }
    }

    private func hoursColor(total: Int) -> Color {
        guard let window = session.window else { return UNESColor.ink }
        if total > window.maxHours { return EnrollmentTone.danger }
        if total == 0 { return UNESColor.ink4 }
        if total < window.minHours { return EnrollmentTone.warn }
        return EnrollmentTone.ok
    }

    private func statusLine(conflictCount: Int) -> String {
        if conflictCount > 0 { return EnrollmentFormat.conflictCountLabel(conflictCount) }
        return DisciplinesFormat.disciplineCountLabel(session.picks.count)
    }
}

/// The dock's glass slab — shared with the review dock.
private struct EnrollmentDockChrome: ViewModifier {
    func body(content: Content) -> some View {
        content
            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .strokeBorder(UNESColor.line)
            }
            .shadow(color: Color(hex: 0x141020, opacity: 0.18), radius: 18, y: 14)
            .padding(EdgeInsets(top: 8, leading: 14, bottom: 8, trailing: 14))
    }
}

extension View {
    func enrollmentDockChrome() -> some View {
        modifier(EnrollmentDockChrome())
    }
}

#Preview {
    VStack {
        Spacer()
        EnrollmentDock(
            session: .preview,
            primaryLabel: .localized(.enrollmentActionReview),
            onPrimary: {},
            secondaryLabel: String.localized(.enrollmentActionGrid),
            onSecondary: {}
        )
        EnrollmentDock(
            session: EnrollmentSession(window: .preview),
            primaryLabel: .localized(.enrollmentActionReview),
            onPrimary: {}
        )
    }
    .background(UNESColor.surface)
}
