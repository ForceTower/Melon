import SwiftUI

/// The HIG segmented control over categories — equal-width pills on the
/// translucent gray track, each led by its category dot.
struct CalendarCategorySegments: View {
    var selected: CalendarCategoryFilter
    var onSelect: (CalendarCategoryFilter) -> Void

    var body: some View {
        HStack(spacing: 3) {
            ForEach(CalendarCategoryFilter.allCases, id: \.self) { filter in
                segment(filter)
            }
        }
        .padding(3)
        .background(Color(hex: 0x787880, opacity: 0.16), in: RoundedRectangle(cornerRadius: 13, style: .continuous))
    }

    private func segment(_ filter: CalendarCategoryFilter) -> some View {
        let active = selected == filter
        return Button {
            withAnimation(.easeOut(duration: 0.15)) {
                onSelect(filter)
            }
        } label: {
            HStack(spacing: 6) {
                if let category = filter.category {
                    Circle()
                        .fill(category.color)
                        .frame(width: 8, height: 8)
                }
                Text(filter.label)
                    .font(.system(size: 13, weight: .semibold))
                    .tracking(-0.13)
                    .foregroundStyle(active ? UNESColor.ink : UNESColor.ink3)
                    .lineLimit(1)
            }
            .padding(.vertical, 7)
            .frame(maxWidth: .infinity)
            .background {
                if active {
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .fill(UNESColor.card)
                        .shadow(color: .black.opacity(0.14), radius: 1.5, y: 1)
                }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// The scope row underneath — ink-filled pill when active, card otherwise.
struct CalendarScopePills: View {
    var selected: CalendarScopeFilter
    var onSelect: (CalendarScopeFilter) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 7) {
                ForEach(CalendarScopeFilter.allCases, id: \.self) { filter in
                    pill(filter)
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 4)
        }
        .animation(.easeOut(duration: 0.15), value: selected)
    }

    private func pill(_ filter: CalendarScopeFilter) -> some View {
        let active = selected == filter
        return Button {
            onSelect(filter)
        } label: {
            Text(filter.label)
                .font(.system(size: 12.5, weight: .semibold))
                .tracking(-0.13)
                .foregroundStyle(active ? UNESColor.surface : UNESColor.ink3)
                .padding(EdgeInsets(top: 6, leading: 13, bottom: 6, trailing: 13))
                .background(
                    active ? UNESColor.ink : UNESColor.card,
                    in: RoundedRectangle(cornerRadius: 16, style: .continuous)
                )
                .overlay {
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .strokeBorder(active ? UNESColor.ink : UNESColor.cardLine)
                }
                .shadow(color: Color(hex: 0x141020, opacity: 0.04), radius: 3, y: 2)
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    VStack(spacing: 14) {
        CalendarCategorySegments(selected: .all, onSelect: { _ in })
            .padding(.horizontal, 16)
        CalendarCategorySegments(selected: .exam, onSelect: { _ in })
            .padding(.horizontal, 16)
        CalendarScopePills(selected: .all, onSelect: { _ in })
    }
    .padding(.vertical, 20)
    .background(UNESColor.surface)
}
