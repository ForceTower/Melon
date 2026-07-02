import SwiftUI

/// The package search field: accent border while focused, clear button
/// while anything is typed.
struct LicensesSearchField: View {
    var query: String
    var onQueryChange: (String) -> Void

    @FocusState private var focused: Bool

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(UNESColor.ink3)

            TextField(
                "Buscar pacote ou autor",
                text: Binding(get: { query }, set: { onQueryChange($0) })
            )
            .font(.system(size: 16, weight: .medium))
            .tracking(-0.16)
            .foregroundStyle(UNESColor.ink)
            .focused($focused)
            .autocorrectionDisabled()
            .noAutocapitalization()
            .padding(.vertical, 11)

            if !query.isEmpty {
                Button {
                    onQueryChange("")
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(UNESColor.ink3)
                        .frame(width: 20, height: 20)
                        .background(UNESColor.surface3, in: Circle())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 12)
        .background(UNESColor.surface2)
        .clipShape(RoundedRectangle(cornerRadius: 13, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 13, style: .continuous)
                .strokeBorder(focused ? UNESColor.accent : .clear)
        }
        .animation(.easeOut(duration: 0.15), value: focused)
    }
}

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
        LicensesSearchField(query: "", onQueryChange: { _ in })
            .padding(.horizontal, 16)
        LicensesSearchField(query: "swift", onQueryChange: { _ in })
            .padding(.horizontal, 16)
        LicensesFilterChips(active: nil, onChange: { _ in })
        LicensesFilterChips(active: .mit, onChange: { _ in })
    }
    .padding(.vertical, 16)
    .background(UNESColor.surface)
}
