import SwiftUI

/// The "Definições" card: one row per app-level surface. Rows highlight on
/// press; navigation arrives with the features they point at.
struct MeSettingsList: View {
    var onSelect: (MeSettingsRow) -> Void

    var body: some View {
        VStack(spacing: 0) {
            ForEach(Array(MeSettingsRow.allCases.enumerated()), id: \.element) { position, row in
                button(row)
                    .overlay(alignment: .bottom) {
                        if position < MeSettingsRow.allCases.count - 1 {
                            Rectangle()
                                .fill(UNESColor.line)
                                .frame(height: 0.5)
                        }
                    }
            }
        }
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
    }

    private func button(_ row: MeSettingsRow) -> some View {
        Button {
            onSelect(row)
        } label: {
            HStack(spacing: 13) {
                Image(systemName: row.icon)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(.white)
                    .frame(width: 30, height: 30)
                    .background(row.tone, in: RoundedRectangle(cornerRadius: 8, style: .continuous))

                VStack(alignment: .leading, spacing: 1) {
                    Text(row.label)
                        .font(.system(size: 15, weight: .medium))
                        .tracking(-0.15)
                        .foregroundStyle(UNESColor.ink)
                    Text(row.hint)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Image(systemName: "chevron.right")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(UNESColor.ink4)
            }
            .padding(EdgeInsets(top: 11, leading: 14, bottom: 11, trailing: 14))
            .contentShape(Rectangle())
        }
        .buttonStyle(RowPressStyle())
    }
}

extension MeSettingsRow {
    var label: String {
        switch self {
        case .settings: .localized(.meSettingsSettings)
        case .about: .localized(.meSettingsAbout)
        case .feedback: .localized(.meSettingsFeedback)
        case .licenses: .localized(.meSettingsLicenses)
        }
    }

    var hint: String {
        switch self {
        case .settings: .localized(.meSettingsSettingsHint)
        case .about: MeFormat.versionHint
        case .feedback: .localized(.meSettingsFeedbackHint)
        case .licenses: .localized(.meSettingsLicensesHint)
        }
    }

    var icon: String {
        switch self {
        case .settings: "gearshape"
        case .about: "info.circle"
        case .feedback: "ladybug"
        case .licenses: "c.circle"
        }
    }

    var tone: Color {
        switch self {
        case .settings: UNESColor.readable(0x8E8E93)
        case .about: UNESColor.readable(0x0A84FF)
        case .feedback: UNESColor.readable(0xE8894E)
        case .licenses: UNESColor.readable(0x7A5AD0)
        }
    }
}

/// The v2 `.eu-row` treatment: rows tint instead of scaling.
struct RowPressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(configuration.isPressed ? UNESColor.surface2 : .clear)
            .animation(.easeOut(duration: 0.15), value: configuration.isPressed)
    }
}

#Preview {
    MeSettingsList(onSelect: { _ in })
        .padding(16)
        .frame(maxHeight: .infinity)
        .background(UNESColor.surface)
}
