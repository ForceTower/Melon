import SwiftUI

/// The horizontally scrolling family filter pills: "Todos" plus one chip
/// per license family, tinted with the family tone while active.
struct LicensesFilterChips: View {
    var active: LicenseFamily?
    var onChange: (LicenseFamily?) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 7) {
                chip(nil)
                ForEach(LicenseCatalog.breakdown) { share in
                    chip(share.family)
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 2)
        }
        .animation(.easeOut(duration: 0.15), value: active)
    }

    private func chip(_ family: LicenseFamily?) -> some View {
        let on = family == active
        return Button {
            onChange(family)
        } label: {
            Text(family?.rawValue ?? "Todos")
                .font(.system(size: 13, weight: .semibold))
                .tracking(-0.13)
                .foregroundStyle(on ? activeLabel(for: family) : UNESColor.ink2)
                .padding(EdgeInsets(top: 7, leading: 13, bottom: 7, trailing: 13))
                .background(on ? family?.tone ?? UNESColor.accent : UNESColor.surface2, in: Capsule())
        }
        .buttonStyle(.plain)
    }

    /// Family tones lift toward pastel in dark mode, so their active label
    /// flips to ink-on-light; the accent "Todos" chip keeps white.
    private func activeLabel(for family: LicenseFamily?) -> Color {
        family == nil ? .white : Color(light: .white, dark: UNESColor.darkBg)
    }
}

#Preview {
    VStack(spacing: 11) {
        LicensesFilterChips(active: nil, onChange: { _ in })
        LicensesFilterChips(active: .mit, onChange: { _ in })
    }
    .padding(.vertical, 16)
    .background(UNESColor.surface)
}
