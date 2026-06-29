import SwiftUI

// UNES — one selected discipline on the review screen: identity, schedule and
// standing badges, plus the per-pick "aceitar outra turma" / "fila de espera"
// toggles. Ported from `ReviewItem` in `screens-matricula-review.jsx`.
struct ReviewItem: View {
    let pick: EnrollmentPick
    let enroll: EnrollmentState
    var readonly: Bool = false

    private var discipline: OfferedDiscipline { pick.discipline }
    private var section: ClassSection { pick.section }
    private var tone: Color { section.tone.color }
    private var seats: SeatState { SeatState(section) }
    private var noSchedule: Bool { !EnrollmentScheduling.hasSchedule(section) }

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                Rectangle().fill(tone).frame(width: 4)
                details.padding(.horizontal, 14).padding(.vertical, 13)
            }
            if !readonly {
                toggles
            }
        }
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .strokeBorder(UNESColor.cardLine, lineWidth: 1)
        )
    }

    // MARK: Details

    private var details: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 8) {
                CodeChip(code: discipline.code, tone: discipline.tone, small: true)
                Text(section.label)
                    .font(UNESFont.mono(10, weight: .semibold))
                    .foregroundStyle(UNESColor.ink2)
                Text("· \(discipline.workload)h")
                    .font(UNESFont.mono(9.5))
                    .foregroundStyle(UNESColor.ink4)
                Spacer(minLength: 8)
                if !readonly { removeButton }
            }

            Text(discipline.name)
                .font(UNESFont.serif(17))
                .tracking(-0.17)
                .foregroundStyle(UNESColor.ink)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 7)

            ScheduleLine(section: section)
                .padding(.top, 8)

            HStack(spacing: 8) {
                if discipline.hasUnmetPrereq {
                    EnrollmentBadge(kind: .prereq, text: "Pré-req. pendente")
                }
                if seats.isFull {
                    EnrollmentBadge(kind: .waitlist, text: "Fila · \(waitlistRank)")
                }
                if noSchedule {
                    EnrollmentBadge(kind: .optional, text: "Horário a definir")
                }
            }
            .padding(.top, 8)
        }
    }

    private var waitlistRank: String {
        section.waitlistCount > 0 ? "\(section.waitlistCount + 1)º" : "na espera"
    }

    private var removeButton: some View {
        Button {
            enroll.remove(discipline.id)
        } label: {
            Image(systemName: "xmark")
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(UNESColor.ink3)
                .frame(width: 26, height: 26)
                .background(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(UNESColor.surface2)
                        .overlay(RoundedRectangle(cornerRadius: 8, style: .continuous).strokeBorder(UNESColor.cardLine, lineWidth: 1))
                )
        }
        .buttonStyle(.plain)
    }

    // MARK: Toggles

    private var toggles: some View {
        VStack(spacing: 11) {
            toggleRow(
                title: "Aceitar outra turma",
                subtitle: "Sem vaga na \(section.label)? Me aloque em outra turma de \(discipline.code).",
                isOn: Binding(
                    get: { enroll.selection(for: discipline.id)?.allowsOther ?? pick.allowsOther },
                    set: { enroll.setAllowsOther(discipline.id, $0) }
                ),
                tint: tone
            )
            if seats.isFull {
                Divider().overlay(UNESColor.line)
                toggleRow(
                    title: "Entrar na fila de espera",
                    subtitle: "Turma lotada — você concorre conforme a fila.",
                    isOn: Binding(
                        get: { enroll.selection(for: discipline.id)?.waitlist ?? pick.waitlist },
                        set: { enroll.setWaitlist(discipline.id, $0) }
                    ),
                    tint: EnrollmentPalette.warnSolid
                )
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 11)
        .overlay(alignment: .top) {
            Rectangle().fill(UNESColor.line).frame(height: 1)
        }
    }

    private func toggleRow(title: String, subtitle: String, isOn: Binding<Bool>, tint: Color) -> some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(UNESFont.sans(12.5, weight: .semibold))
                    .foregroundStyle(UNESColor.ink)
                Text(subtitle)
                    .font(UNESFont.sans(11))
                    .foregroundStyle(UNESColor.ink3)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 8)
            Toggle("", isOn: isOn)
                .labelsHidden()
                .tint(tint)
        }
    }
}
