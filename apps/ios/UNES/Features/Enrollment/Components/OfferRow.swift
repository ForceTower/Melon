import SwiftUI

// UNES — one discipline row in the offers catalogue. Shows the code, name,
// workload, section count and standing badges; highlights with a left accent
// once a section is in the proposal. Ported from `OfferRow` in
// `screens-matricula-screens.jsx`.
struct OfferRow: View {
    let discipline: OfferedDiscipline
    let enroll: EnrollmentState
    let onTap: () -> Void

    private var tone: Color { discipline.tone.color }
    private var selection: EnrollmentPick? { enroll.selection(for: discipline.id) }

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 10) {
                    CodeChip(code: discipline.code, tone: discipline.tone)
                    Spacer(minLength: 0)
                    if discipline.suggestion {
                        EnrollmentBadge(kind: .suggested, text: "Sugerida")
                    }
                    if let selection {
                        EnrollmentBadge(kind: .inProposal, text: selection.section.label)
                    } else {
                        Image(systemName: "chevron.right")
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundStyle(UNESColor.ink4)
                    }
                }

                Text(discipline.name)
                    .font(UNESFont.serif(18))
                    .tracking(-0.18)
                    .foregroundStyle(UNESColor.ink)
                    .fixedSize(horizontal: false, vertical: true)
                    .multilineTextAlignment(.leading)

                HStack(spacing: 8) {
                    Text("\(discipline.workload)h")
                        .font(UNESFont.mono(10))
                        .foregroundStyle(UNESColor.ink3)
                    Text("·").foregroundStyle(UNESColor.ink4).opacity(0.4)
                    Text("\(discipline.sections.count) \(discipline.sections.count == 1 ? "turma" : "turmas")")
                        .font(UNESFont.mono(10))
                        .foregroundStyle(UNESColor.ink3)
                    EnrollmentBadge(
                        kind: discipline.mandatory ? .mandatory : .optional,
                        text: discipline.mandatory ? "Obrigatória" : "Optativa"
                    )
                    if discipline.hasUnmetPrereq {
                        EnrollmentBadge(kind: .prereq, text: "Pré-requisito")
                    }
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .cardSurface(
                RoundedRectangle(cornerRadius: 16, style: .continuous),
                stroke: selection != nil ? tone.opacity(0.33) : UNESColor.cardLine
            )
            .overlay(alignment: .leading) {
                if selection != nil {
                    Rectangle()
                        .fill(tone)
                        .frame(width: 3)
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .buttonStyle(PressScaleStyle())
    }
}
