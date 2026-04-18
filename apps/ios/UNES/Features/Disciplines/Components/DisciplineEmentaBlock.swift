import SwiftUI

/// Ementa (syllabus) block. Collapsible when the text is long.
struct DisciplineEmentaBlock: View {
    let discipline: Discipline

    @State private var expanded = false

    private var long: Bool { (discipline.ementa?.count ?? 0) > 160 }

    private var shown: String {
        guard let ementa = discipline.ementa else { return "" }
        if expanded || !long { return ementa }
        return String(ementa.prefix(160)) + "…"
    }

    var body: some View {
        if let ementa = discipline.ementa, !ementa.isEmpty {
            VStack(alignment: .leading, spacing: 0) {
                DisciplineSectionHeader("Ementa")

                VStack(alignment: .leading, spacing: 8) {
                    Text(shown)
                        .font(UNESFont.sans(13))
                        .foregroundStyle(UNESColor.ink2)
                        .lineSpacing(3)

                    if long {
                        Button {
                            withAnimation(.easeInOut(duration: 0.25)) { expanded.toggle() }
                        } label: {
                            Text(expanded ? "mostrar menos" : "ler mais")
                                .font(UNESFont.sans(12, weight: .medium))
                                .foregroundStyle(discipline.color)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .fill(UNESColor.card)
                        .overlay(
                            RoundedRectangle(cornerRadius: 18, style: .continuous)
                                .strokeBorder(UNESColor.cardLine, lineWidth: 1)
                        )
                )
                .overlay(alignment: .leading) {
                    Rectangle()
                        .fill(discipline.color)
                        .frame(width: 3)
                        .clipShape(RoundedRectangle(cornerRadius: 2, style: .continuous))
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 18)
        }
    }
}
