import SwiftUI

// UNES — discipline detail + turma picker: title, prerequisite notice, the
// selectable section cards, and the "aceitar outra turma" preference once a
// section is chosen. Ported from `DisciplinePickerScreen` in
// `screens-matricula-screens.jsx`.
struct SectionPickerView: View {
    let discipline: OfferedDiscipline
    let enroll: EnrollmentState
    let window: EnrollmentWindow
    let onTimetable: () -> Void

    @Environment(\.dismiss) private var dismiss

    private var tone: Color { discipline.tone.color }
    private var selection: EnrollmentPick? { enroll.selection(for: discipline.id) }

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 14) {
                titleBlock
                    .fadeUpOnAppear(delay: 0.02, distance: 12, duration: 0.5)
                if !discipline.prereqs.isEmpty {
                    prereqBanner
                        .fadeUpOnAppear(delay: 0.08, distance: 12, duration: 0.55)
                }
                sections
                    .fadeUpOnAppear(delay: 0.14, distance: 12, duration: 0.55)
                if selection != nil {
                    allowsOtherRow
                        .fadeUpOnAppear(delay: 0.04, distance: 10, duration: 0.5)
                }
            }
            .padding(.horizontal, 18)
            .padding(.top, 6)
            .padding(.bottom, 124)
        }
        .background {
            UNESColor.surface
                .overlay(alignment: .top) {
                    RadialGradient(
                        colors: [tone.opacity(0.12), .clear],
                        center: .top, startRadius: 0, endRadius: 260
                    )
                    .frame(height: 240)
                    .frame(maxHeight: .infinity, alignment: .top)
                }
                .ignoresSafeArea()
        }
        .overlay(alignment: .bottom) {
            EnrollmentDock(
                enroll: enroll,
                window: window,
                secondary: .init(label: "Grade", systemImage: "square.grid.2x2", action: onTimetable),
                primaryLabel: "Concluir",
                primarySystemImage: "checkmark",
                primaryAction: { dismiss() }
            )
        }
        .navigationTitle(discipline.code)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.hidden, for: .navigationBar)
    }

    // MARK: Title

    private var titleBlock: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(discipline.name)
                .font(UNESFont.serif(28))
                .tracking(-0.42)
                .foregroundStyle(UNESColor.ink)
                .fixedSize(horizontal: false, vertical: true)
            HStack(spacing: 8) {
                Text("\(discipline.workload)h")
                    .font(UNESFont.mono(11))
                    .foregroundStyle(UNESColor.ink3)
                Text("·").foregroundStyle(UNESColor.ink4).opacity(0.3)
                Text("\(discipline.sections.count) turmas")
                    .font(UNESFont.mono(11))
                    .foregroundStyle(UNESColor.ink3)
                if discipline.suggestion {
                    EnrollmentBadge(kind: .suggested, text: "Sugerida pelo curso")
                }
            }
        }
        .padding(.top, 6)
    }

    // MARK: Prereq

    private var prereqBanner: some View {
        let unmet = discipline.hasUnmetPrereq
        return EnrollmentBanner(
            tone: unmet ? .danger : .info,
            title: unmet ? "Pré-requisito não cumprido" : "Pré-requisitos cumpridos",
            systemImage: unmet ? "exclamationmark" : "checkmark"
        ) {
            VStack(alignment: .leading, spacing: 4) {
                prereqText(unmet: unmet)
                if unmet {
                    Text("Você pode selecionar, mas a matrícula depende de análise do colegiado.")
                        .foregroundColor(UNESColor.ink3)
                }
            }
        }
    }

    private func prereqText(unmet: Bool) -> Text {
        var text = Text("")
        for (i, p) in discipline.prereqs.enumerated() {
            if i > 0 { text = text + Text(", ") }
            text = text
                + Text(p.code).foregroundColor(UNESColor.ink).bold()
                + Text(" \(p.name)")
            if !p.met {
                text = text + Text(" — pendente").foregroundColor(EnrollmentPalette.danger)
            }
        }
        return text
    }

    // MARK: Sections

    private var sections: some View {
        VStack(alignment: .leading, spacing: 12) {
            EnrollmentEyebrow(text: "Escolha uma turma")
            ForEach(discipline.sections) { section in
                SectionCard(discipline: discipline, section: section, enroll: enroll, useQueue: window.useQueue)
            }
        }
    }

    // MARK: Allows other

    @ViewBuilder
    private var allowsOtherRow: some View {
        if let selection {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("Aceitar outra turma")
                        .font(UNESFont.sans(13.5, weight: .semibold))
                        .foregroundStyle(UNESColor.ink)
                    (
                        Text("Se eu não conseguir vaga na ")
                            + Text(selection.section.label).foregroundColor(UNESColor.ink2).bold()
                            + Text(", me matricule em outra turma de \(discipline.code) com vaga.")
                    )
                    .font(UNESFont.sans(12))
                    .foregroundStyle(UNESColor.ink3)
                    .lineSpacing(2)
                    .fixedSize(horizontal: false, vertical: true)
                }
                Spacer(minLength: 8)
                Toggle("", isOn: Binding(
                    get: { enroll.selection(for: discipline.id)?.allowsOther ?? false },
                    set: { enroll.setAllowsOther(discipline.id, $0) }
                ))
                .labelsHidden()
                .tint(tone)
            }
            .padding(14)
            .cardSurface(RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
    }
}

#Preview("Selecionada") {
    NavigationStack {
        SectionPickerView(
            discipline: EnrollmentFixtures.disciplines[0],
            enroll: .previewSeeded,
            window: EnrollmentFixtures.window,
            onTimetable: {}
        )
    }
}

#Preview("Lotada / pré-req") {
    NavigationStack {
        SectionPickerView(
            discipline: EnrollmentFixtures.disciplines[2],
            enroll: EnrollmentState(),
            window: EnrollmentFixtures.window,
            onTimetable: {}
        )
    }
}
