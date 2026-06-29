import SwiftUI

// UNES — floating glass action dock pinned to the bottom of the catalogue,
// timetable and review screens. Shows the running workload + proposal state on
// the left and one or two actions on the right. Ported from `BottomDock` in
// `screens-matricula-screens.jsx`.
struct EnrollmentDock: View {
    let enroll: EnrollmentState
    let window: EnrollmentWindow
    var secondary: SecondaryAction?
    let primaryLabel: String
    var primarySystemImage: String = "arrow.right"
    let primaryAction: () -> Void

    struct SecondaryAction {
        let label: String
        let systemImage: String
        let action: () -> Void
    }

    private var total: Int { enroll.totalHours }
    private var conflictCount: Int { enroll.conflicts.count }

    private var statusColor: Color {
        if total > window.maxHours { return EnrollmentPalette.danger }
        if total < window.minHours { return EnrollmentPalette.warnSolid }
        if total == 0 { return UNESColor.ink4 }
        return EnrollmentPalette.okSolid
    }

    private var subtitle: String {
        if conflictCount > 0 {
            return "\(conflictCount) conflito\(conflictCount > 1 ? "s" : "")"
        }
        let n = enroll.picks.count
        return "\(n) disciplina\(n == 1 ? "" : "s")"
    }

    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 3) {
                HStack(alignment: .firstTextBaseline, spacing: 4) {
                    Text("\(total)h")
                        .font(UNESFont.sans(20, weight: .bold))
                        .tracking(-0.6)
                        .foregroundStyle(statusColor)
                    Text("/ \(window.maxHours)")
                        .font(UNESFont.mono(9.5))
                        .foregroundStyle(UNESColor.ink4)
                }
                Text(subtitle)
                    .font(UNESFont.mono(9))
                    .foregroundStyle(conflictCount > 0 ? EnrollmentPalette.danger : UNESColor.ink3)
                    .lineLimit(1)
            }
            .fixedSize(horizontal: true, vertical: false)

            Spacer(minLength: 8)

            if let secondary {
                Button(action: secondary.action) {
                    HStack(spacing: 6) {
                        Image(systemName: secondary.systemImage)
                            .font(.system(size: 13, weight: .medium))
                        Text(secondary.label)
                            .font(UNESFont.sans(13, weight: .semibold))
                            .lineLimit(1)
                    }
                    .fixedSize(horizontal: true, vertical: false)
                    .foregroundStyle(UNESColor.ink)
                    .frame(height: 44)
                    .padding(.horizontal, 16)
                    .cardSurface(RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .buttonStyle(PressScaleStyle())
            }

            Button(action: primaryAction) {
                HStack(spacing: 6) {
                    Text(primaryLabel)
                        .font(UNESFont.sans(13, weight: .semibold))
                        .lineLimit(1)
                    Image(systemName: primarySystemImage)
                        .font(.system(size: 11, weight: .semibold))
                }
                .fixedSize(horizontal: true, vertical: false)
                .foregroundStyle(UNESColor.surface)
                .frame(height: 44)
                .padding(.horizontal, 18)
                .background(UNESColor.ink, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
            .buttonStyle(PressScaleStyle())
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .strokeBorder(UNESColor.cardLine, lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.16), radius: 18, y: 14)
        .padding(.horizontal, 14)
        .padding(.bottom, 16)
    }
}
