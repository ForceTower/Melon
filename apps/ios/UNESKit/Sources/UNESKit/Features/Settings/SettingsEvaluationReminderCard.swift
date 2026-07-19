import SwiftUI

/// The device-local evaluation-reminder switch — a single-row card under the
/// server-synced push groups, styled like their rows. Not part of the
/// "n ativas" counter: it schedules on this device, it doesn't PATCH.
struct SettingsEvaluationReminderCard: View {
    var isOn: Bool
    var onToggle: @MainActor (Bool) -> Void

    private let tone = UNESColor.readable(0x7A5AD0)

    var body: some View {
        HStack(spacing: 13) {
            Image(systemName: "alarm")
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(.white)
                .frame(width: 30, height: 30)
                .background(tone, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                .shadow(color: tone.opacity(0.27), radius: 5, y: 4)

            VStack(alignment: .leading, spacing: 1) {
                Text(.settingsNotificationsEvaluationReminderLabel)
                    .font(.system(size: 15, weight: .medium))
                    .tracking(-0.15)
                    .foregroundStyle(UNESColor.ink)
                Text(.settingsNotificationsEvaluationReminderHint)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Toggle(
                String.localized(.settingsNotificationsEvaluationReminderLabel),
                isOn: Binding(get: { isOn }, set: { onToggle($0) })
            )
            .labelsHidden()
        }
        .padding(EdgeInsets(top: 11, leading: 14, bottom: 11, trailing: 14))
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
    }
}

#Preview {
    VStack(spacing: 12) {
        SettingsEvaluationReminderCard(isOn: true, onToggle: { _ in })
        SettingsEvaluationReminderCard(isOn: false, onToggle: { _ in })
    }
    .padding(16)
    .background(UNESColor.surface)
}
