import SwiftUI

/// "Você foi para a prova final" — the amber banner shown while the Prova
/// Final grade hasn't been published: what happened, the minimum needed on
/// the exam, and the 0,6/0,4 closing formula. Disappears once the result
/// lands (the grade then joins its own section in the grades block).
struct DisciplineFinalsCard: View {
    let detail: DisciplineDetail

    private var tone: Color { UNESColor.caution }

    /// Truncated partial mean — the value the university feeds the formula.
    private var average: Double {
        DisciplineRules.floorToTenth(
            DisciplineDetail.partialAverage(of: detail.sections.flatMap(\.grades)) ?? 0
        )
    }

    /// Minimum Prova Final grade to close at 5 — rounded up, like every
    /// other "needed" value, and clamped to the 0–10 grade scale.
    private var needed: Double {
        min(10, max(0, DisciplineRules.ceilToTenth(DisciplineRules.neededFinal(avg: average))))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            badge

            Text(.disciplinesFinalsTitle)
                .font(.system(size: 21, weight: .bold))
                .tracking(-0.63)
                .foregroundStyle(UNESColor.ink)
                .padding(.top, 12)

            lead
                .padding(.top, 6)

            neededBox
                .padding(.top, 16)

            formulaStrip
                .padding(.top, 12)

            Text(.disciplinesFinalsFormulaFootnote)
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .lineSpacing(2)
                .frame(maxWidth: .infinity)
                .multilineTextAlignment(.center)
                .padding(.top, 8)
        }
        .padding(EdgeInsets(top: 16, leading: 18, bottom: 18, trailing: 18))
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .strokeBorder(tone.opacity(0.35))
        }
        .shadow(color: tone.opacity(0.13), radius: 17, y: 14)
    }

    private var badge: some View {
        HStack(spacing: 6) {
            Image(systemName: "flag")
                .font(.system(size: 11, weight: .semibold))
            Text(.disciplinesFinalsBadge)
                .textCase(.uppercase)
                .font(.system(size: 11, weight: .bold))
                .tracking(0.5)
        }
        .foregroundStyle(tone)
        .padding(EdgeInsets(top: 4, leading: 8, bottom: 4, trailing: 10))
        .background(tone.opacity(0.13), in: RoundedRectangle(cornerRadius: 9, style: .continuous))
    }

    private var lead: some View {
        (
            Text(.disciplinesFinalsLeadPrefix)
                + Text(formatGrade(average))
                .fontWeight(.bold)
                .foregroundStyle(UNESColor.ink2)
                + Text(.disciplinesFinalsLeadSuffix(formatGrade(DisciplineRules.passThreshold)))
        )
        .font(.system(size: 13.5, weight: .medium))
        .lineSpacing(3)
        .foregroundStyle(UNESColor.ink3)
    }

    /// The big needed value with its plain-words explanation, next to the
    /// target glyph.
    private var neededBox: some View {
        HStack(spacing: 14) {
            VStack(alignment: .leading, spacing: 6) {
                HStack(alignment: .lastTextBaseline, spacing: 8) {
                    Text(DisciplinesFormat.neededGrade(needed))
                        .font(.system(size: 46, weight: .bold))
                        .tracking(-2.07)
                        .monospacedDigit()
                        .foregroundStyle(tone)
                    Text(.disciplinesFinalsMinimumCaption)
                        .font(.system(size: 14.5, weight: .semibold))
                        .foregroundStyle(UNESColor.ink3)
                }

                (
                    Text(.disciplinesFinalsExplainPrefix)
                        + Text(DisciplinesFormat.neededGrade(needed))
                        .fontWeight(.bold)
                        .foregroundStyle(tone)
                        + Text(.disciplinesFinalsExplainMiddle)
                        + Text(formatGrade(DisciplineRules.finalCutoff))
                        .fontWeight(.bold)
                        .foregroundStyle(UNESColor.ink2)
                        + Text(.disciplinesFinalsExplainSuffix)
                )
                .font(.system(size: 13, weight: .medium))
                .lineSpacing(2.5)
                .foregroundStyle(UNESColor.ink3)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Image(systemName: "target")
                .font(.system(size: 26, weight: .medium))
                .foregroundStyle(tone)
                .frame(width: 58, height: 58)
                .background(tone.opacity(0.15), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .padding(EdgeInsets(top: 15, leading: 16, bottom: 15, trailing: 16))
        .background(tone.opacity(0.08), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .strokeBorder(tone.opacity(0.2))
        }
    }

    /// "média sem. | prova final → média final".
    private var formulaStrip: some View {
        HStack(spacing: 0) {
            formulaCell(value: formatGrade(average), label: .disciplinesFinalsFormulaSemester)
            Rectangle()
                .fill(UNESColor.line)
                .frame(width: 1, height: 26)
            formulaCell(
                value: DisciplinesFormat.neededGrade(needed),
                label: .disciplinesFinalsFormulaExam,
                valueColor: tone
            )
            Image(systemName: "arrow.right")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(UNESColor.ink4)
                .frame(width: 24)
            formulaCell(
                value: formatGrade(DisciplineRules.finalCutoff),
                label: .disciplinesFinalsFormulaResult,
                labelColor: UNESColor.ink3
            )
        }
        .padding(EdgeInsets(top: 11, leading: 8, bottom: 11, trailing: 8))
        .frame(maxWidth: .infinity)
        .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private func formulaCell(
        value: String,
        label: LocalizedStringResource,
        valueColor: Color = UNESColor.ink,
        labelColor: Color = UNESColor.ink4
    ) -> some View {
        VStack(spacing: 5) {
            Text(value)
                .font(.system(size: 20, weight: .bold))
                .tracking(-0.6)
                .monospacedDigit()
                .foregroundStyle(valueColor)
            Text(label)
                .font(.system(size: 10.5, weight: .semibold))
                .tracking(0.2)
                .lineLimit(1)
                .foregroundStyle(labelColor)
        }
        .frame(maxWidth: .infinity)
    }
}

#Preview {
    ScrollView {
        DisciplineFinalsCard(detail: .previewFinals())
            .padding(16)
    }
    .background(UNESColor.surface)
}
