import SwiftUI

/// The Notas privacy pair: a live lock-screen mock showing exactly what a
/// grade push would reveal, and the picker that drives it.
struct SettingsSpoilerSection: View {
    var spoiler: GradeSpoiler
    var onSelect: (GradeSpoiler) -> Void

    var body: some View {
        VStack(spacing: 12) {
            lockScreenPreview
            pickerCard
        }
    }

    // MARK: Lock-screen preview

    private var lockScreenPreview: some View {
        ZStack {
            Color(hex: 0x0E0B14)
            MeshView(variant: .rose, intensity: 0.7)
                .opacity(0.5)
            LinearGradient.css(
                stops: [
                    .init(color: Color(hex: 0x08050E, opacity: 0.35), location: 0),
                    .init(color: Color(hex: 0x08050E, opacity: 0.72), location: 1),
                ],
                angle: 160
            )

            VStack(spacing: 12) {
                clock
                notificationBubble
            }
            .padding(EdgeInsets(top: 16, leading: 16, bottom: 15, trailing: 16))
        }
        .environment(\.colorScheme, .dark)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.22), radius: 13, y: 10)
    }

    private var clock: some View {
        TimelineView(.everyMinute) { context in
            VStack(spacing: 2) {
                Text(Self.lockScreenDate(context.date))
                    .font(.system(size: 13, weight: .semibold))
                    .tracking(0.3)
                // Apple's canonical lock-screen time — this is a mock.
                Text(verbatim: "9:41")
                    .font(.system(size: 46, weight: .bold))
                    .tracking(-1.38)
                    .monospacedDigit()
            }
            .foregroundStyle(.white.opacity(0.9))
        }
    }

    private var notificationBubble: some View {
        HStack(spacing: 11) {
            Text(verbatim: "U")
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(.white)
                .frame(width: 32, height: 32)
                .background(
                    LinearGradient.css(
                        stops: [
                            .init(color: UNESColor.amber, location: 0),
                            .init(color: UNESColor.coral, location: 1),
                        ],
                        angle: 135
                    ),
                    in: RoundedRectangle(cornerRadius: 8, style: .continuous)
                )

            VStack(alignment: .leading, spacing: 2) {
                HStack(alignment: .firstTextBaseline) {
                    Text(verbatim: "UNES")
                        .font(.system(size: 12, weight: .bold))
                        .tracking(-0.12)
                    Spacer()
                    Text(.commonNow)
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(.white.opacity(0.6))
                }
                Text(spoiler.lockScreenPreview)
                    .font(.system(size: 12.5, weight: .medium))
                    .lineLimit(1)
            }
            .foregroundStyle(.white)
        }
        .padding(EdgeInsets(top: 11, leading: 12, bottom: 11, trailing: 12))
        .background(.white.opacity(0.14), in: RoundedRectangle(cornerRadius: 15, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 15, style: .continuous)
                .strokeBorder(.white.opacity(0.14))
        }
    }

    /// "quinta-feira, 17 de abril" — the lock-screen date line.
    private static func lockScreenDate(_ date: Date) -> String {
        date.formatted(
            .dateTime.weekday(.wide).day().month(.wide)
                .locale(Locale.autoupdatingCurrent)
        )
    }

    // MARK: Picker

    private var pickerCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 11) {
                Image(systemName: "checkmark.shield")
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(.white)
                    .frame(width: 30, height: 30)
                    .background(UNESColor.coral, in: RoundedRectangle(cornerRadius: 8, style: .continuous))

                VStack(alignment: .leading, spacing: 1) {
                    Text(.settingsSpoilerTitle)
                        .font(.system(size: 15, weight: .semibold))
                        .tracking(-0.15)
                        .foregroundStyle(UNESColor.ink)
                    Text(.settingsSpoilerSubtitle)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                }
            }

            SettingsSegmented(
                options: GradeSpoiler.allCases.map { ($0, $0.label) },
                selected: spoiler,
                onSelect: onSelect
            )
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
    }
}

extension GradeSpoiler {
    var label: String {
        switch self {
        case .value: String.localized(.settingsSpoilerValue)
        case .summary: String.localized(.settingsSpoilerSummary)
        case .discreet: String.localized(.settingsSpoilerDiscreet)
        }
    }

    /// What the mock push shows for a fresh Cálculo Diferencial B2 grade.
    var lockScreenPreview: String {
        switch self {
        case .value: String.localized(.settingsSpoilerPreviewValue)
        case .summary: String.localized(.settingsSpoilerPreviewSummary)
        case .discreet: String.localized(.settingsSpoilerPreviewDiscreet)
        }
    }
}

#Preview {
    @Previewable @State var spoiler = GradeSpoiler.summary
    SettingsSpoilerSection(spoiler: spoiler) {
        spoiler = $0
    }
    .padding(16)
    .frame(maxHeight: .infinity)
    .background(UNESColor.surface)
}
