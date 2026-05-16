import SwiftUI

/// Horizontally scrolling chips that filter the inbox. Active chip inverts
/// to ink-on-surface; inactive chips sit in the card color with a thin line.
struct FilterChipRow: View {
    @Binding var active: MessageFilter
    let counts: [MessageFilter: Int]

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(MessageFilter.allCases) { filter in
                    chip(for: filter)
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 2)
        }
    }

    private func chip(for filter: MessageFilter) -> some View {
        let isActive = active == filter
        let count = counts[filter] ?? 0
        return Button {
            withAnimation(.easeOut(duration: 0.15)) { active = filter }
        } label: {
            HStack(spacing: 6) {
                Text(filter.label)
                    .font(UNESFont.sans(12, weight: .medium))
                if count > 0 {
                    Text("\(count)")
                        .font(UNESFont.mono(10, weight: .semibold))
                        .opacity(0.85)
                }
            }
            .foregroundStyle(isActive ? UNESColor.surface : UNESColor.ink2)
            .padding(.horizontal, 11)
            .padding(.vertical, 6)
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(isActive ? UNESColor.ink : UNESColor.card)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(isActive ? UNESColor.ink : UNESColor.cardLine, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }
}
