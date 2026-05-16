import SwiftUI

/// Horizontally scrolling chip row used twice on the calendar — once for the
/// category filter (Tudo / Prazos / Provas / Feriados) and once, smaller, for
/// the scope filter (Todos / Geral / Faculdade / Curso / Turma).
struct CalCategoryFilterRow: View {
    @Binding var active: CalendarCategoryFilter

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(CalendarCategoryFilter.allCases) { f in
                    CalChip(label: f.label, isActive: active == f, small: false) {
                        withAnimation(.easeOut(duration: 0.18)) { active = f }
                    }
                }
            }
            .padding(.horizontal, 20)
        }
    }
}

struct CalScopeFilterRow: View {
    @Binding var active: CalendarScopeFilter

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(CalendarScopeFilter.allCases) { f in
                    CalChip(label: f.label, isActive: active == f, small: true) {
                        withAnimation(.easeOut(duration: 0.18)) { active = f }
                    }
                }
            }
            .padding(.horizontal, 20)
        }
    }
}

private struct CalChip: View {
    let label: String
    let isActive: Bool
    let small: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(UNESFont.sans(small ? 11 : 12, weight: .medium))
                .tracking(-0.06)
                .foregroundStyle(isActive ? UNESColor.surface : UNESColor.ink2)
                .padding(.horizontal, small ? 10 : 13)
                .padding(.vertical, small ? 6 : 8)
                .background(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(isActive ? UNESColor.ink : Color.clear)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .strokeBorder(isActive ? UNESColor.ink : UNESColor.line, lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
    }
}
