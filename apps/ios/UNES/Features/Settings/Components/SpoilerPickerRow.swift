import SwiftUI

/// Expandable picker for the grade-notification spoiler knob. Collapsed state
/// shows the icon tile + label + current selection pill; tapping it reveals
/// the three options (Valor · Comentário · Apenas aviso) as radio rows.
/// Mirrors `PickerRow` in `screens-settings.jsx`.
struct SpoilerPickerRow: View {
    @Binding var value: SpoilerMode
    @Binding var isOpen: Bool

    var body: some View {
        VStack(spacing: 0) {
            collapsedRow

            if isOpen {
                VStack(spacing: 4) {
                    ForEach(SpoilerMode.allCases) { option in
                        SpoilerOptionRow(
                            option: option,
                            isActive: option == value,
                            onSelect: {
                                withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                                    value = option
                                    isOpen = false
                                }
                            }
                        )
                    }
                }
                .padding(.leading, 60)
                .padding(.trailing, 14)
                .padding(.bottom, 14)
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .cardSurface(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
    }

    private var collapsedRow: some View {
        Button {
            withAnimation(.spring(response: 0.4, dampingFraction: 0.85)) {
                isOpen.toggle()
            }
        } label: {
            HStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .fill(SettingsTone.coral.background)
                    Image(systemName: "shield")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundStyle(SettingsTone.coral.foreground)
                }
                .frame(width: 32, height: 32)
                .shadow(color: SettingsTone.coral.background.opacity(0.2), radius: 4, x: 0, y: 3)

                VStack(alignment: .leading, spacing: 2) {
                    Text("Spoiler das notas")
                        .font(UNESFont.sans(14, weight: .medium))
                        .tracking(-0.07)
                        .foregroundStyle(UNESColor.ink)

                    Text("O QUE APARECE NA NOTIFICAÇÃO")
                        .font(UNESFont.mono(9.5))
                        .tracking(0.48)
                        .foregroundStyle(UNESColor.ink4)
                        .lineLimit(1)
                        .truncationMode(.tail)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                HStack(spacing: 6) {
                    Text(value.label)
                        .font(UNESFont.sans(12, weight: .medium))
                        .tracking(-0.06)
                        .foregroundStyle(UNESColor.ink)

                    Image(systemName: "chevron.right")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(UNESColor.ink3)
                        .rotationEffect(.degrees(isOpen ? 90 : 0))
                }
                .padding(.horizontal, 11)
                .padding(.vertical, 6)
                .background(
                    Capsule()
                        .fill(UNESColor.surface2)
                        .overlay(Capsule().strokeBorder(UNESColor.line, lineWidth: 1))
                )
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 13)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

private struct SpoilerOptionRow: View {
    let option: SpoilerMode
    let isActive: Bool
    let onSelect: () -> Void

    var body: some View {
        Button(action: onSelect) {
            HStack(spacing: 10) {
                radio

                VStack(alignment: .leading, spacing: 1) {
                    Text(option.label)
                        .font(UNESFont.sans(12.5, weight: .medium))
                        .tracking(-0.06)

                    Text(option.hint)
                        .font(UNESFont.mono(9))
                        .tracking(0.54)
                        .opacity(isActive ? 0.7 : 1)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .foregroundStyle(isActive ? SettingsTone.coral.foreground : UNESColor.ink)
            .padding(.horizontal, 12)
            .padding(.vertical, 9)
            .background(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(isActive ? SettingsTone.coral.background : UNESColor.surface2)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(isActive ? SettingsTone.coral.background : UNESColor.line, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    private var radio: some View {
        ZStack {
            Circle()
                .strokeBorder(isActive ? SettingsTone.coral.foreground : UNESColor.ink4, lineWidth: 1.5)
                .frame(width: 14, height: 14)

            if isActive {
                Circle()
                    .fill(SettingsTone.coral.foreground)
                    .frame(width: 6, height: 6)
            }
        }
    }
}
