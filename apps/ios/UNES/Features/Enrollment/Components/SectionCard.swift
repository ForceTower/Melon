import SwiftUI

// UNES — a selectable turma card on the discipline picker: shift, seat meter,
// schedule, meeting details, and inline conflict / full notices, capped by a
// state-driven select button. Ported from `GroupCard` in
// `screens-matricula-screens.jsx`.
struct SectionCard: View {
    let discipline: OfferedDiscipline
    let section: ClassSection
    let enroll: EnrollmentState
    /// Whether full sections offer a waitlist (from the window) — drives the
    /// "fila de espera" copy and the select-button affordance.
    var useQueue: Bool = true

    private var tone: Color { section.tone.color }
    private var seats: SeatState { SeatState(section) }
    private var selectedHere: Bool { enroll.selection(for: discipline.id)?.section.id == section.id }
    private var clash: (discipline: OfferedDiscipline, section: ClassSection, day: Int)? {
        enroll.clash(for: discipline, section)
    }
    private var blocked: Bool { clash != nil && !selectedHere }
    private var noSchedule: Bool { !EnrollmentScheduling.hasSchedule(section) }

    private func toggle() {
        if blocked { return }
        if selectedHere {
            enroll.remove(discipline.id)
        } else {
            enroll.select(discipline, section, waitlist: seats.isFull)
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            content
            selectButton
        }
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .strokeBorder(borderColor, lineWidth: selectedHere ? 1.5 : 1)
        )
        .shadow(color: selectedHere ? tone.opacity(0.18) : .clear, radius: 14, y: 8)
        .animation(.easeInOut(duration: 0.2), value: selectedHere)
    }

    private var borderColor: Color {
        if selectedHere { return tone }
        if blocked { return EnrollmentPalette.danger.opacity(0.27) }
        return UNESColor.cardLine
    }

    // MARK: Content

    private var content: some View {
        VStack(alignment: .leading, spacing: 0) {
            head
                .padding(.bottom, 12)
            ScheduleLine(section: section)
                .padding(.bottom, 10)
            meetingsDetail
            notices
        }
        .padding(.horizontal, 14)
        .padding(.top, 14)
        .padding(.bottom, 12)
    }

    private var head: some View {
        HStack(spacing: 8) {
            Text(section.label)
                .font(UNESFont.sans(18, weight: .bold))
                .tracking(-0.36)
                .foregroundStyle(UNESColor.ink)
            shiftChip
            if section.coursePreferential {
                Circle().fill(EnrollmentPalette.okSolid).frame(width: 6, height: 6)
            }
            Spacer(minLength: 8)
            SeatMeter(section: section, compact: true)
        }
    }

    private var shiftChip: some View {
        Text((section.meetings.first?.shift ?? .undefined).label)
            .font(UNESFont.mono(9.5, weight: .semibold))
            .tracking(0.57)
            .foregroundStyle(noSchedule ? UNESColor.ink3 : tone)
            .padding(.horizontal, 7)
            .padding(.vertical, 3)
            .background(
                RoundedRectangle(cornerRadius: 6, style: .continuous)
                    .fill(noSchedule ? UNESColor.surface3 : tone.opacity(0.10))
            )
    }

    private var meetingsDetail: some View {
        VStack(alignment: .leading, spacing: 6) {
            ForEach(Array(section.meetings.enumerated()), id: \.offset) { _, meeting in
                HStack(spacing: 8) {
                    Text(meeting.kind)
                        .font(UNESFont.mono(9))
                        .foregroundStyle(UNESColor.ink3)
                        .padding(.horizontal, 5)
                        .padding(.vertical, 1)
                        .background(RoundedRectangle(cornerRadius: 4, style: .continuous).fill(UNESColor.surface2))
                    Text(meeting.room ?? "Sala a definir")
                        .font(UNESFont.sans(11.5))
                        .foregroundStyle(UNESColor.ink3)
                    Text("·").foregroundStyle(UNESColor.ink4).opacity(0.3)
                    if meeting.professors.isEmpty {
                        Text("Professor a definir")
                            .font(UNESFont.sans(11.5, weight: .regular).italic())
                            .foregroundStyle(UNESColor.ink4)
                    } else {
                        Text(meeting.professors.joined(separator: ", "))
                            .font(UNESFont.sans(11.5))
                            .foregroundStyle(UNESColor.ink3)
                    }
                    Spacer(minLength: 0)
                }
            }
        }
    }

    @ViewBuilder
    private var notices: some View {
        if blocked, let clash {
            EnrollmentBanner(tone: .danger, title: "Conflito de horário") {
                conflictText(clash)
            }
            .padding(.top, 12)
        } else if seats.isFull {
            EnrollmentBanner(tone: .warn, title: "Turma lotada", systemImage: "person.2.fill") {
                fullText
            }
            .padding(.top, 12)
        }
    }

    private func conflictText(_ clash: (discipline: OfferedDiscipline, section: ClassSection, day: Int)) -> Text {
        let weekday = EnrollmentScheduling.daysFull[clash.day].lowercased()
        return Text("Choca com ")
            + Text("\(clash.discipline.code) \(clash.section.label)").foregroundColor(UNESColor.ink).bold()
            + Text(" na ")
            + Text(weekday).foregroundColor(UNESColor.ink).bold()
            + Text(". Troque uma das turmas para selecionar.")
    }

    private var fullText: Text {
        guard useQueue else {
            return Text("Sem vagas disponíveis nesta turma.")
        }
        if section.waitlistCount > 0 {
            return Text("Você pode entrar na fila de espera — ")
                + Text("\(section.waitlistCount) na frente").foregroundColor(UNESColor.ink).bold()
                + Text(".")
        }
        return Text("Você pode entrar na fila de espera.")
    }

    // MARK: Select button

    private var selectButton: some View {
        Button(action: toggle) {
            HStack(spacing: 7) {
                if let symbol = buttonSymbol {
                    Image(systemName: symbol).font(.system(size: 12, weight: .bold))
                }
                Text(buttonLabel)
                    .font(UNESFont.sans(13.5, weight: .semibold))
            }
            .foregroundStyle(buttonForeground)
            .frame(maxWidth: .infinity)
            .frame(height: 46)
            .background(buttonBackground)
            .overlay(alignment: .top) {
                Rectangle().fill(UNESColor.line).frame(height: 1)
            }
        }
        .buttonStyle(.plain)
        .disabled(blocked)
    }

    private var buttonLabel: String {
        if selectedHere { return "Selecionada · tocar para remover" }
        if blocked { return "Indisponível — conflito" }
        if seats.isFull && useQueue { return "Entrar na fila de espera" }
        if seats.isFull { return "Sem vagas" }
        return "Selecionar esta turma"
    }

    private var buttonSymbol: String? {
        if selectedHere { return "checkmark" }
        if seats.isFull && !blocked && useQueue { return "person.2.fill" }
        return nil
    }

    private var buttonForeground: Color {
        if selectedHere { return .white }
        if blocked { return UNESColor.ink4 }
        if seats.isFull { return .white }
        return UNESColor.ink
    }

    private var buttonBackground: Color {
        if selectedHere { return tone }
        if blocked { return UNESColor.surface2 }
        if seats.isFull { return EnrollmentPalette.warnSolid }
        return UNESColor.surface2
    }
}

#Preview {
    ScrollView {
        VStack(spacing: 12) {
            ForEach(EnrollmentFixtures.disciplines[2].sections) { section in
                SectionCard(
                    discipline: EnrollmentFixtures.disciplines[2],
                    section: section,
                    enroll: EnrollmentState()
                )
            }
        }
        .padding()
    }
    .background(UNESColor.surface)
}
