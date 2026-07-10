import SwiftUI

/// The workload meter: hours picked against the window's min–max band, with
/// the status chip and the tick-marked track.
struct EnrollmentWorkloadCard: View {
    var totalHours: Int
    var minHours: Int
    var maxHours: Int
    var compact = false

    private var isUnder: Bool { totalHours < minHours }
    private var isOver: Bool { totalHours > maxHours }
    private var isOk: Bool { !isUnder && !isOver && totalHours > 0 }

    private var color: Color {
        if isOver { return EnrollmentTone.danger }
        if totalHours == 0 { return UNESColor.ink4 }
        if isUnder { return EnrollmentTone.warn }
        return EnrollmentTone.ok
    }

    private var status: String {
        if totalHours == 0 { return .localized(.enrollmentWorkloadStatusStart) }
        if isOver { return .localized(.enrollmentWorkloadStatusOver) }
        if isUnder { return .localized(.enrollmentWorkloadStatusUnder(minHours - totalHours)) }
        return .localized(.enrollmentWorkloadStatusOk)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .bottom) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(.enrollmentWorkloadTitle)
                        .textCase(.uppercase)
                        .font(.system(size: 11, weight: .semibold))
                        .tracking(0.6)
                        .foregroundStyle(UNESColor.ink3)
                    HStack(alignment: .firstTextBaseline, spacing: 6) {
                        Text(verbatim: "\(totalHours)")
                            .font(.system(size: compact ? 32 : 40, weight: .bold))
                            .tracking(compact ? -1.4 : -1.8)
                            .monospacedDigit()
                            .foregroundStyle(color)
                            .contentTransition(.numericText())
                            .animation(UNESMotion.settle(0.4), value: totalHours)
                        Text(.enrollmentWorkloadHoursSelected)
                            .font(.system(size: 14, weight: .medium))
                            .foregroundStyle(UNESColor.ink3)
                    }
                }

                Spacer()

                HStack(spacing: 5) {
                    if isOk {
                        Image(systemName: "checkmark")
                            .font(.system(size: 9, weight: .bold))
                    }
                    Text(status)
                }
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(color)
                .padding(EdgeInsets(top: 5, leading: 10, bottom: 5, trailing: 10))
                .background(color.opacity(0.12), in: Capsule())
            }

            track

            HStack {
                Text(.enrollmentWorkloadMin(minHours))
                Spacer()
                Text(.enrollmentWorkloadMax(maxHours))
            }
            .font(.system(size: 11, weight: .medium))
            .monospacedDigit()
            .foregroundStyle(UNESColor.ink4)
        }
        .padding(EdgeInsets(top: compact ? 14 : 18, leading: compact ? 16 : 18, bottom: compact ? 13 : 16, trailing: compact ? 16 : 18))
        .enrollmentCard(radius: compact ? 20 : 24)
    }

    private var track: some View {
        GeometryReader { geometry in
            let width = geometry.size.width
            // Overshooting picks stretch the scale so the fill never clips.
            let trackMax = Double(max(maxHours, totalHours))
            let position = { (hours: Int) in trackMax > 0 ? Double(hours) / trackMax * width : 0 }

            ZStack(alignment: .leading) {
                RoundedRectangle(cornerRadius: 4, style: .continuous)
                    .fill(UNESColor.surface3)

                // The allowed min–max band.
                Rectangle()
                    .fill(EnrollmentTone.ok.opacity(0.2))
                    .frame(width: position(maxHours) - position(minHours))
                    .offset(x: position(minHours))

                RoundedRectangle(cornerRadius: 4, style: .continuous)
                    .fill(color)
                    .frame(width: position(totalHours))
                    .animation(UNESMotion.settle(0.5), value: totalHours)

                ForEach([minHours, maxHours], id: \.self) { bound in
                    Rectangle()
                        .fill(UNESColor.ink4)
                        .frame(width: 1.5, height: 14)
                        .offset(x: position(bound))
                }
            }
        }
        .frame(height: 8)
    }
}

#Preview {
    VStack(spacing: 14) {
        EnrollmentWorkloadCard(totalHours: 240, minHours: 240, maxHours: 420, compact: true)
        EnrollmentWorkloadCard(totalHours: 180, minHours: 240, maxHours: 420)
        EnrollmentWorkloadCard(totalHours: 450, minHours: 240, maxHours: 420)
        EnrollmentWorkloadCard(totalHours: 0, minHours: 240, maxHours: 420, compact: true)
    }
    .padding(16)
    .background(UNESColor.surface)
}
