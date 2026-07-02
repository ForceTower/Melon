import SwiftUI

// MARK: - Section header

/// "Notas ......... peso igual" — a 22pt section title with an optional
/// trailing caption, shared by every detail section.
struct DisciplineSectionHeader: View {
    var title: String
    var trailing: String?

    var body: some View {
        HStack(alignment: .lastTextBaseline) {
            Text(title)
                .font(.system(size: 22, weight: .bold))
                .tracking(-0.66)
                .foregroundStyle(UNESColor.ink)
            Spacer()
            if let trailing {
                Text(trailing)
                    .font(.system(size: 13, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink4)
            }
        }
        .padding(EdgeInsets(top: 0, leading: 4, bottom: 12, trailing: 4))
    }
}

// MARK: - Stat card

/// "Carga 60h" / "Faltas 4" — the two-up quick facts under the hero.
struct DisciplineStatCard: View {
    var icon: String
    var tint: Color
    var label: String
    var value: String
    var sub: String?
    var valueColor: Color = UNESColor.ink

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 7) {
                Image(systemName: icon)
                    .font(.system(size: 13, weight: .medium))
                Text(label)
                    .font(.system(size: 12.5, weight: .semibold))
                    .tracking(-0.13)
            }
            .foregroundStyle(tint)

            VStack(alignment: .leading, spacing: 4) {
                Text(value)
                    .font(.system(size: 30, weight: .bold))
                    .tracking(-1.05)
                    .monospacedDigit()
                    .foregroundStyle(valueColor)
                if let sub {
                    Text(sub)
                        .font(.system(size: 12, weight: .medium))
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink3)
                }
            }
        }
        .padding(15)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
    }
}

// MARK: - Grades

/// The "Notas" section: one grouped card per grade section, with a kind +
/// group sub-header when a filter would change what's shown.
struct DisciplineGradesBlock: View {
    let detail: DisciplineDetail
    let color: Color
    var selectedGroup: String?

    var body: some View {
        let sections = detail.sections(forGroup: selectedGroup)
        VStack(spacing: 0) {
            DisciplineSectionHeader(
                title: "Notas",
                trailing: detail.hasEqualWeights ? "peso igual" : "média ponderada"
            )

            if sections.isEmpty {
                DetailEmptyCard(message: "O professor ainda não cadastrou avaliações.")
            } else {
                VStack(spacing: 14) {
                    ForEach(sections) { section in
                        VStack(alignment: .leading, spacing: 8) {
                            if sections.count > 1 || section.groupCode != nil {
                                sectionLabel(section)
                            }
                            gradesCard(section.grades)
                        }
                    }
                }
            }
        }
    }

    private func sectionLabel(_ section: DisciplineGradeSection) -> some View {
        HStack(spacing: 7) {
            Text(section.name ?? "Notas")
                .font(.system(size: 11.5, weight: .bold))
                .tracking(0.3)
                .foregroundStyle(color)
            if let code = section.groupCode {
                Text(code)
                    .font(.system(size: 9.5, weight: .semibold))
                    .tracking(0.3)
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink4)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 6))
            }
        }
        .padding(.leading, 4)
    }

    private func gradesCard(_ grades: [DisciplineDetailGrade]) -> some View {
        VStack(spacing: 0) {
            ForEach(Array(grades.enumerated()), id: \.element.id) { index, grade in
                GradeDetailRow(grade: grade, color: color, isLast: index == grades.count - 1)
            }
        }
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
    }
}

/// One evaluation: label chip, name + date (or countdown), released value.
private struct GradeDetailRow: View {
    let grade: DisciplineDetailGrade
    let color: Color
    var isLast: Bool

    var body: some View {
        let released = grade.value != nil
        HStack(spacing: 13) {
            Text(grade.label)
                .font(.system(size: 10.5, weight: .bold))
                .tracking(0.3)
                .foregroundStyle(released ? color : UNESColor.ink4)
                .padding(.horizontal, 7)
                .padding(.vertical, 3)
                .background(
                    released ? color.opacity(0.13) : UNESColor.surface2,
                    in: RoundedRectangle(cornerRadius: 7)
                )

            VStack(alignment: .leading, spacing: 1) {
                Text(grade.title)
                    .font(.system(size: 15, weight: .semibold))
                    .tracking(-0.23)
                    .lineLimit(1)
                    .foregroundStyle(UNESColor.ink)
                Text(dateLabel)
                    .font(.system(size: 12.5, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(grade.daysUntil != nil ? color : UNESColor.ink4)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Text(formatGrade(grade.value))
                .font(.system(size: 24, weight: .bold))
                .tracking(-0.72)
                .monospacedDigit()
                .foregroundStyle(released ? UNESColor.score(grade.value) : UNESColor.ink4)
        }
        .padding(EdgeInsets(top: 13, leading: 15, bottom: 13, trailing: 15))
        .overlay(alignment: .bottom) {
            if !isLast {
                Rectangle().fill(UNESColor.line).frame(height: 0.5)
            }
        }
    }

    private var dateLabel: String {
        if let days = grade.daysUntil { return DisciplinesFormat.inDaysLabel(days) }
        if let date = grade.date { return DisciplinesFormat.longDate(date) }
        return "data não divulgada"
    }
}

// MARK: - Presença

/// Absences against the 75% rule: big counts, a tick meter, and a warning
/// banner once half the allowance is gone.
struct DisciplinePresencaCard: View {
    let detail: DisciplineDetail

    private var tone: Color {
        switch detail.absenceRisk {
        case .critical: UNESColor.coral
        case .warning: UNESColor.caution
        case .ok: UNESColor.ink
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            DisciplineSectionHeader(title: "Presença")

            VStack(spacing: 14) {
                HStack(alignment: .bottom, spacing: 12) {
                    VStack(alignment: .leading, spacing: 4) {
                        HStack(alignment: .lastTextBaseline, spacing: 6) {
                            Text("\(detail.missedHours)")
                                .font(.system(size: 34, weight: .bold))
                                .tracking(-1.36)
                                .monospacedDigit()
                                .foregroundStyle(tone)
                            Text("de \(detail.allowedMissedHours)")
                                .font(.system(size: 16, weight: .semibold))
                                .monospacedDigit()
                                .foregroundStyle(UNESColor.ink4)
                        }
                        Text("faltas registradas")
                            .font(.system(size: 13, weight: .medium))
                            .foregroundStyle(UNESColor.ink3)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)

                    VStack(alignment: .trailing, spacing: 3) {
                        Text("\(max(0, detail.allowedMissedHours - detail.missedHours))")
                            .font(.system(size: 22, weight: .bold))
                            .tracking(-0.66)
                            .monospacedDigit()
                            .foregroundStyle(UNESColor.ink)
                        Text("restantes")
                            .textCase(.uppercase)
                            .font(.system(size: 11, weight: .semibold))
                            .tracking(0.4)
                            .foregroundStyle(UNESColor.ink4)
                    }
                }

                meter

                if detail.absenceRisk != .ok {
                    warningBanner
                }
            }
            .padding(16)
            .background(UNESColor.card)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }
            .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
        }
    }

    private var meter: some View {
        let allowed = detail.allowedMissedHours
        let ticks = max(1, min(allowed, 15))
        let filled = allowed > 0
            ? min(ticks, Int((Double(detail.missedHours) / Double(allowed) * Double(ticks)).rounded()))
            : (detail.missedHours > 0 ? ticks : 0)

        return HStack(spacing: 3) {
            ForEach(0..<ticks, id: \.self) { index in
                RoundedRectangle(cornerRadius: 4, style: .continuous)
                    .fill(index < filled ? tone : UNESColor.surface3)
                    .frame(height: 8)
                    .frame(maxWidth: .infinity)
            }
        }
    }

    private var warningBanner: some View {
        let percent = detail.allowedMissedHours > 0
            ? Int((Double(detail.missedHours) / Double(detail.allowedMissedHours) * 100).rounded())
            : 100
        return HStack(spacing: 8) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 13, weight: .medium))
            Text(
                detail.absenceRisk == .critical
                    ? "Atenção: \(percent)% do limite atingido."
                    : "Monitore suas faltas com cuidado."
            )
            .font(.system(size: 12.5, weight: .medium))
        }
        .foregroundStyle(tone)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 9, leading: 12, bottom: 9, trailing: 12))
        .background(tone.opacity(0.09), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

// MARK: - Ementa

/// The syllabus, clamped to a teaser until "ler mais".
struct DisciplineEmentaCard: View {
    let detail: DisciplineDetail
    let color: Color

    @State private var expanded = false

    var body: some View {
        VStack(spacing: 0) {
            DisciplineSectionHeader(title: "Ementa")

            if let ementa = detail.ementa {
                let isLong = ementa.count > 150
                VStack(alignment: .leading, spacing: 10) {
                    Text(expanded || !isLong ? ementa : ementa.prefix(150) + "…")
                        .font(.system(size: 14))
                        .lineSpacing(4)
                        .foregroundStyle(UNESColor.ink2)

                    if isLong {
                        Button(expanded ? "mostrar menos" : "ler mais") {
                            expanded.toggle()
                        }
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(color)
                        .buttonStyle(.plain)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.leading, 12)
                .padding(EdgeInsets(top: 16, leading: 18, bottom: 16, trailing: 18))
                .background(UNESColor.card)
                .overlay(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 3, style: .continuous)
                        .fill(color)
                        .frame(width: 4)
                        .padding(.vertical, 16)
                }
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .strokeBorder(UNESColor.cardLine)
                }
                .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
            }
        }
    }
}

// MARK: - Empty card

/// A quiet, centered placeholder card shared by the empty sections.
struct DetailEmptyCard: View {
    var message: String

    var body: some View {
        Text(message)
            .font(.system(size: 13.5, weight: .medium))
            .foregroundStyle(UNESColor.ink3)
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)
            .padding(22)
            .background(UNESColor.card)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }
            .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
    }
}

#Preview {
    ScrollView {
        VStack(spacing: 22) {
            DisciplineGradesBlock(detail: .preview(), color: UNESColor.coral)
            DisciplineGradesBlock(detail: .previewMultiGroup(), color: UNESColor.violet)
            DisciplinePresencaCard(detail: .preview())
            DisciplineEmentaCard(detail: .preview(), color: UNESColor.coral)
        }
        .padding(16)
    }
    .background(UNESColor.surface)
}
