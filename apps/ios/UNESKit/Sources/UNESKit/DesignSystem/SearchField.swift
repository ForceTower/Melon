import SwiftUI

/// The v2 inline search field: accent border while focused, clear button
/// while anything is typed.
struct SearchField: View {
    var placeholder: LocalizedStringResource
    var query: String
    var onQueryChange: (String) -> Void

    @FocusState private var focused: Bool

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(UNESColor.ink3)

            TextField(
                "",
                text: Binding(get: { query }, set: { onQueryChange($0) }),
                prompt: Text(placeholder)
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

#Preview {
    VStack(spacing: 11) {
        SearchField(placeholder: .commonSearch, query: "", onQueryChange: { _ in })
        SearchField(placeholder: .commonSearch, query: "estruturas", onQueryChange: { _ in })
    }
    .padding(16)
    .background(UNESColor.surface)
}
