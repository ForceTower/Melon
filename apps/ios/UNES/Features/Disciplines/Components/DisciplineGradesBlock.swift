import SwiftUI

/// Grades section for the detail view. Headline card with the partial average
/// plus a projection of what's needed to close at 7.0, followed by a row per
/// evaluation in each grade section.
struct DisciplineGradesBlock: View {
    let discipline: Discipline
    let selectedGroup: String?

    private var visibleSections: [GradeSection] {
        discipline.sections(for: selectedGroup)
    }

    private var visibleGrades: [GradeEntry] {
        visibleSections.flatMap(\.grades)
    }

    private var average: Double? {
        let scores = visibleGrades.compactMap(\.score)
        guard !scores.isEmpty else { return nil }
        return scores.reduce(0, +) / Double(scores.count)
    }

    private var needed: NeededProjection? {
        let done = visibleGrades.compactMap(\.score)
        let pending = visibleGrades.filter { $0.score == nil }
        guard !done.isEmpty, !pending.isEmpty else { return nil }
        let sumDone = done.reduce(0, +)
        let required = (7.0 * Double(visibleGrades.count) - sumDone) / Double(pending.count)
        return NeededProjection(required: required, pending: pending.count, target: 7.0)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            DisciplineSectionHeader("Notas")
            headline
                .padding(.bottom, 10)
            ForEach(visibleSections) { sec in
                sectionBlock(sec)
                    .padding(.bottom, 10)
            }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 18)
    }

    private var headline: some View {
        HStack(alignment: .center, spacing: 16) {
            GradeRing(score: average, size: 74, stroke: 5, color: discipline.color)

            VStack(alignment: .leading, spacing: 4) {
                Text("MÉDIA PARCIAL\(selectedGroup.map { " · \($0)" } ?? "")")
                    .font(UNESFont.mono(9, weight: .semibold))
                    .tracking(1.08)
                    .foregroundStyle(UNESColor.ink4)

                if let needed {
                    neededText(needed: needed)
                        .font(UNESFont.sans(13))
                        .foregroundStyle(UNESColor.ink2)
                } else {
                    Text(average != nil ? "Você está indo bem." : "Ainda sem notas lançadas.")
                        .font(UNESFont.sans(13))
                        .foregroundStyle(UNESColor.ink3)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(16)
        .cardSurface(RoundedRectangle(cornerRadius: 20, style: .continuous))
    }

    private func neededText(needed: NeededProjection) -> Text {
        let formatted: String = needed.required > 10 ? "—" : String(format: "%.1f", needed.required)
        let color: Color = needed.required > 10
            ? DisciplineScoreColor.danger
            : needed.required > 7
                ? DisciplineScoreColor.caution
                : DisciplineScoreColor.excellent
        let pluralS = needed.pending > 1 ? "s" : ""

        let requiredStyled = Text(formatted)
            .font(UNESFont.serif(18))
            .foregroundColor(color)
        let target = Text("7,0").font(UNESFont.sans(13, weight: .semibold))
        let count = "\(needed.pending) avaliaç\(needed.pending > 1 ? "ões" : "ão") restante\(pluralS)"
        return Text("Precisa de \(requiredStyled) na\(pluralS) \(count) para fechar em \(target).")
    }

    @ViewBuilder
    private func sectionBlock(_ sec: GradeSection) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text(sec.name)
                    .font(UNESFont.mono(9, weight: .semibold))
                    .tracking(1.26)
                    .textCase(.uppercase)
                    .foregroundStyle(discipline.color)
                if let group = sec.group {
                    Text(group)
                        .font(UNESFont.mono(9, weight: .semibold))
                        .tracking(0.72)
                        .foregroundStyle(UNESColor.ink4)
                        .padding(.horizontal, 5)
                        .padding(.vertical, 1)
                        .background(
                            RoundedRectangle(cornerRadius: 3, style: .continuous)
                                .fill(UNESColor.surface2)
                        )
                }
            }
            .padding(.leading, 4)
            .padding(.bottom, 8)

            VStack(spacing: 0) {
                ForEach(Array(sec.grades.enumerated()), id: \.element.id) { idx, g in
                    GradeRow(grade: g, accent: discipline.color)
                    if idx < sec.grades.count - 1 {
                        Rectangle()
                            .fill(UNESColor.line)
                            .frame(height: 1)
                    }
                }
            }
            .cardSurface(RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
    }
}

private struct GradeRow: View {
    let grade: GradeEntry
    let accent: Color

    private var hasScore: Bool { grade.score != nil }

    var body: some View {
        HStack(spacing: 12) {
            Text(grade.label)
                .font(UNESFont.mono(10, weight: .bold))
                .tracking(0.6)
                .foregroundStyle(hasScore ? accent : UNESColor.ink4)
                .padding(.horizontal, 6)
                .padding(.vertical, 3)
                .background(
                    RoundedRectangle(cornerRadius: 5, style: .continuous)
                        .fill(hasScore ? accent.opacity(0.13) : UNESColor.surface2)
                )

            VStack(alignment: .leading, spacing: 1) {
                Text(grade.title)
                    .font(UNESFont.sans(13))
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(1)
                Text(grade.date ?? "data não divulgada")
                    .font(UNESFont.mono(10))
                    .foregroundStyle(UNESColor.ink4)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Text(hasScore ? String(format: "%.1f", grade.score ?? 0) : "—")
                .font(UNESFont.serif(22))
                .tracking(-0.44)
                .foregroundStyle(DisciplineScoreColor.color(for: grade.score))
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
    }
}
