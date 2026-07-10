import SwiftUI

/// The Configurações take on the iOS segmented control: equal-width pills on
/// the translucent gray track, the active one lifted onto a card.
struct SettingsSegmented<Option: Hashable>: View {
    var options: [(value: Option, label: String)]
    var selected: Option
    var onSelect: (Option) -> Void

    var body: some View {
        HStack(spacing: 3) {
            ForEach(options, id: \.value) { option in
                segment(option.value, label: option.label)
            }
        }
        .padding(3)
        .background(Color(hex: 0x787880, opacity: 0.16), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func segment(_ value: Option, label: String) -> some View {
        let active = selected == value
        return Button {
            withAnimation(.easeOut(duration: 0.15)) {
                onSelect(value)
            }
        } label: {
            Text(label)
                .font(.system(size: 13, weight: .semibold))
                .tracking(-0.13)
                .foregroundStyle(active ? UNESColor.ink : UNESColor.ink3)
                .lineLimit(1)
                .padding(.vertical, 8)
                .frame(maxWidth: .infinity)
                .background {
                    if active {
                        RoundedRectangle(cornerRadius: 9, style: .continuous)
                            .fill(UNESColor.card)
                            .shadow(color: .black.opacity(0.14), radius: 1.5, y: 1)
                    }
                }
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    @Previewable @State var theme = AppTheme.system
    SettingsSegmented(
        options: [AppTheme.light, .system, .dark].map { ($0, $0.label) },
        selected: theme
    ) {
        theme = $0
    }
    .padding(24)
    .background(UNESColor.surface)
}
