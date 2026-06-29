import SwiftUI

// UNES — proposal review + submit. Aggregates the blockers (conflicts, under/
// over workload, pending prerequisites), lists every pick with its toggles,
// and gates the submit button until the proposal is valid. Ported from
// `ReviewScreen` in `screens-matricula-review.jsx`.
struct ReviewView: View {
    let enroll: EnrollmentState
    let window: EnrollmentWindow
    let windowState: EnrollmentWindowState
    let onTimetable: () -> Void
    /// Runs the live submit; returns nil on success (navigation handled by the
    /// caller) or an error message to surface here.
    let onSubmit: () async -> String?

    @Environment(\.dismiss) private var dismiss
    @State private var submitting = false
    @State private var submitError: String?

    private var total: Int { enroll.totalHours }
    private var isUnder: Bool { total < window.minHours }
    private var isOver: Bool { total > window.maxHours }
    private var hasConflict: Bool { !enroll.conflicts.isEmpty }
    private var isEmpty: Bool { enroll.picks.isEmpty }
    private var isReadonly: Bool { windowState == .closed }
    private var unmetItems: [EnrollmentPick] { enroll.picks.filter { $0.discipline.hasUnmetPrereq } }

    private var blockers: [String] {
        var out: [String] = []
        if hasConflict { out.append("\(enroll.conflicts.count) conflito\(enroll.conflicts.count > 1 ? "s" : "") de horário") }
        if isUnder { out.append("carga \(window.minHours - total)h abaixo do mínimo") }
        if isOver { out.append("carga \(total - window.maxHours)h acima do máximo") }
        if isEmpty { out.append("nenhuma disciplina selecionada") }
        return out
    }

    private var canSubmit: Bool { blockers.isEmpty && !isReadonly }

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 14) {
                if isReadonly {
                    EnrollmentBanner(tone: .info, title: "Proposta enviada", systemImage: "checkmark") {
                        Text("Protocolo #48201 · \(window.endLabel). Somente leitura — reabra a matrícula para editar.")
                    }
                    .fadeUpOnAppear(delay: 0.02, distance: 10, duration: 0.5)
                }

                WorkloadMeter(total: total, min: window.minHours, max: window.maxHours)
                    .fadeUpOnAppear(delay: 0.06, distance: 12, duration: 0.55)

                if !isEmpty && (hasConflict || isUnder || isOver || !unmetItems.isEmpty) {
                    warnings
                        .fadeUpOnAppear(delay: 0.1, distance: 12, duration: 0.55)
                }

                items
                    .fadeUpOnAppear(delay: 0.14, distance: 12, duration: 0.55)
            }
            .padding(.horizontal, 18)
            .padding(.top, 6)
            .padding(.bottom, isReadonly ? 28 : 120)
        }
        .background(UNESColor.surface.ignoresSafeArea())
        .overlay(alignment: .bottom) {
            if !isReadonly { submitDock }
        }
        .navigationTitle(isReadonly ? "Comprovante" : "Revisar proposta")
        .navigationBarTitleDisplayMode(.inline)
        .alert("Não foi possível enviar", isPresented: submitErrorBinding) {
            Button("OK", role: .cancel) { submitError = nil }
        } message: {
            Text(submitError ?? "")
        }
    }

    // MARK: Warnings

    private var warnings: some View {
        VStack(spacing: 8) {
            if hasConflict {
                EnrollmentBanner(tone: .danger, title: "Conflitos de horário") {
                    VStack(alignment: .leading, spacing: 6) {
                        ForEach(enroll.conflicts) { c in
                            Text("\(c.a.code) \(c.aLabel) × \(c.b.code) \(c.bLabel) · \(EnrollmentScheduling.daysFull[c.day].lowercased())")
                        }
                        Button("Ver na grade →", action: onTimetable)
                            .font(UNESFont.sans(12, weight: .semibold))
                            .foregroundStyle(EnrollmentPalette.danger)
                    }
                }
            }
            if isUnder {
                EnrollmentBanner(tone: .warn, title: "Carga horária insuficiente") {
                    Text("Faltam ")
                        + Text("\(window.minHours - total)h").foregroundColor(UNESColor.ink).bold()
                        + Text(" para o mínimo de \(window.minHours)h. Adicione mais disciplinas.")
                }
            }
            if isOver {
                EnrollmentBanner(tone: .danger, title: "Carga horária excedida") {
                    Text("Você está ")
                        + Text("\(total - window.maxHours)h").foregroundColor(UNESColor.ink).bold()
                        + Text(" acima do máximo de \(window.maxHours)h. Remova alguma turma.")
                }
            }
            if !unmetItems.isEmpty {
                EnrollmentBanner(tone: .warn, title: "Pré-requisitos pendentes") {
                    Text("\(unmetItems.map(\.discipline.code).joined(separator: ", ")) \(unmetItems.count > 1 ? "dependem" : "depende") de análise do colegiado.")
                }
            }
        }
    }

    // MARK: Items

    @ViewBuilder
    private var items: some View {
        if isEmpty {
            VStack(spacing: 8) {
                Text("◦").font(UNESFont.serif(30)).foregroundStyle(UNESColor.ink4)
                Text("Nenhuma disciplina na proposta ainda.")
                    .font(UNESFont.sans(13))
                    .foregroundStyle(UNESColor.ink3)
            }
            .frame(maxWidth: .infinity)
            .padding(.top, 50)
        } else {
            VStack(alignment: .leading, spacing: 10) {
                EnrollmentEyebrow(text: "Disciplinas selecionadas · \(enroll.picks.count)")
                ForEach(enroll.picks) { pick in
                    ReviewItem(pick: pick, enroll: enroll, readonly: isReadonly)
                }
            }
        }
    }

    // MARK: Submit dock

    private var submitDock: some View {
        VStack(spacing: 10) {
            if !canSubmit && !isEmpty {
                HStack(spacing: 7) {
                    Text("!")
                        .font(UNESFont.sans(10, weight: .heavy))
                        .foregroundStyle(.white)
                        .frame(width: 14, height: 14)
                        .background(Circle().fill(EnrollmentPalette.danger))
                    Text(blockers.joined(separator: " · "))
                        .font(UNESFont.mono(10))
                        .foregroundStyle(EnrollmentPalette.danger)
                        .fixedSize(horizontal: false, vertical: true)
                    Spacer(minLength: 0)
                }
            }
            HStack(spacing: 10) {
                Button { dismiss() } label: {
                    Image(systemName: "square.and.arrow.down")
                        .font(.system(size: 17, weight: .regular))
                        .foregroundStyle(UNESColor.ink2)
                        .frame(width: 54, height: 50)
                        .cardSurface(RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .buttonStyle(PressScaleStyle())

                Button(action: { Task { await runSubmit() } }) {
                    HStack(spacing: 8) {
                        if submitting {
                            SpinnerView(color: UNESColor.surface).frame(width: 16, height: 16)
                            Text("Enviando…").font(UNESFont.sans(15, weight: .semibold))
                        } else {
                            Text("Enviar proposta de matrícula").font(UNESFont.sans(15, weight: .semibold))
                            Image(systemName: "checkmark").font(.system(size: 13, weight: .bold))
                        }
                    }
                    .foregroundStyle(canSubmit ? UNESColor.surface : UNESColor.ink4)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(canSubmit ? UNESColor.ink : UNESColor.surface3)
                    )
                }
                .buttonStyle(PressScaleStyle())
                .disabled(!canSubmit || submitting)
            }
        }
        .padding(14)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .strokeBorder(UNESColor.cardLine, lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.16), radius: 18, y: 14)
        .padding(.horizontal, 14)
        .padding(.bottom, 16)
    }

    private func runSubmit() async {
        guard canSubmit, !submitting else { return }
        submitting = true
        let error = await onSubmit()
        submitting = false
        // On success the caller pushes the comprovante; only failures surface
        // here, as an alert the student can dismiss and retry from.
        submitError = error
    }

    private var submitErrorBinding: Binding<Bool> {
        Binding(get: { submitError != nil }, set: { if !$0 { submitError = nil } })
    }
}

#Preview("Editar") {
    NavigationStack {
        ReviewView(
            enroll: .previewSeeded, window: EnrollmentFixtures.window,
            windowState: .open, onTimetable: {}, onSubmit: { nil }
        )
    }
}

#Preview("Comprovante") {
    NavigationStack {
        ReviewView(
            enroll: .previewSeeded, window: EnrollmentFixtures.window,
            windowState: .closed, onTimetable: {}, onSubmit: { nil }
        )
    }
}
