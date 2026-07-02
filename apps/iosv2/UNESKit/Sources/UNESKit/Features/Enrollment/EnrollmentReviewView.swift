import ComposableArchitecture
import SwiftUI

/// The proposal review: workload verdict, blockers, the picked sections with
/// their per-pick toggles, and the submit dock. Once the window closes it
/// renders as the read-only comprovante.
struct EnrollmentReviewView: View {
    @Bindable var store: StoreOf<EnrollmentReviewFeature>
    @State private var titleProgress: CGFloat = 0

    private var title: String {
        store.isReadonly ? "Comprovante" : "Revisar"
    }

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            EnrollmentAmbientWash(variant: store.isReadonly ? .fresh : .warm, opacity: 0.24)
            content
        }
        .toolbar {
            ToolbarItem(placement: .principal) {
                Text(title)
                    .font(.system(size: 16, weight: .semibold))
                    .tracking(-0.32)
                    .foregroundStyle(UNESColor.ink)
                    .opacity(titleProgress)
                    .offset(y: (1 - titleProgress) * 6)
            }
        }
        .inlineNavigationBar()
        .hiddenTabBar()
        .alert($store.scope(state: \.alert, action: \.alert))
        .safeAreaInset(edge: .bottom) {
            if !store.isReadonly {
                submitDock
            }
        }
    }

    private var content: some View {
        ScrollView {
            VStack(spacing: 0) {
                header
                    .fadeUp(delay: 0.02)

                VStack(spacing: 0) {
                    if store.isReadonly {
                        EnrollmentBanner(tone: .info, title: "Proposta enviada") {
                            Text("Sua proposta está registrada no SAGRES. Somente leitura — reabra a matrícula para editar.")
                        }
                        .fadeUp(delay: 0.06)
                        .padding(.bottom, 14)
                    }

                    if let window = store.session.window {
                        EnrollmentWorkloadCard(
                            totalHours: store.session.totalHours,
                            minHours: window.minHours,
                            maxHours: window.maxHours
                        )
                        .fadeUp(delay: 0.08)
                        .padding(.bottom, 14)
                    }

                    warnings
                        .fadeUp(delay: 0.12)

                    picks
                        .fadeUp(delay: 0.16)
                }
                .padding(.horizontal, 16)
            }
            .padding(.top, 8)
            .padding(.bottom, 12)
        }
        .onScrollGeometryChange(for: CGFloat.self) { geometry in
            geometry.contentOffset.y + geometry.contentInsets.top
        } action: { _, offset in
            titleProgress = min(max((offset - 40) / 44, 0), 1)
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 5) {
            if store.isReadonly, let window = store.session.window {
                Text("Matrícula · \(window.semester)")
                    .textCase(.uppercase)
                    .font(.system(size: 12, weight: .semibold))
                    .tracking(0.48)
                    .foregroundStyle(UNESColor.accent)
            }
            Text(title)
                .font(.system(size: 40, weight: .bold))
                .tracking(-1.6)
                .foregroundStyle(UNESColor.ink)
            Text(subtitle)
                .font(.system(size: 14, weight: .medium))
                .tracking(-0.14)
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink3)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 2, leading: 20, bottom: 16, trailing: 20))
    }

    private var subtitle: String {
        let picks = store.session.picks
        guard !picks.isEmpty else { return "Proposta vazia" }
        return "\(DisciplinesFormat.disciplineCountLabel(picks.count)) · \(store.session.totalHours)h"
    }

    // MARK: Warnings

    @ViewBuilder
    private var warnings: some View {
        let session = store.session
        let unmet = session.resolvedPicks.filter(\.discipline.hasUnmetPrereq)

        if !session.picks.isEmpty {
            VStack(spacing: 8) {
                if !session.conflicts.isEmpty {
                    EnrollmentBanner(
                        tone: .danger,
                        title: "Conflitos de horário",
                        action: "Ver na grade",
                        onAction: { store.send(.timetableTapped) }
                    ) {
                        VStack(alignment: .leading, spacing: 2) {
                            ForEach(Array(session.conflicts.enumerated()), id: \.offset) { _, conflict in
                                Text("\(conflict.aCode) \(conflict.aLabel) × \(conflict.bCode) \(conflict.bLabel) · \(EnrollmentFormat.dayFull(conflict.day))")
                            }
                        }
                    }
                }
                if let window = session.window, session.totalHours < window.minHours {
                    EnrollmentBanner(tone: .warn, title: "Carga horária insuficiente") {
                        Text("Faltam \(window.minHours - session.totalHours)h para o mínimo de \(window.minHours)h.")
                    }
                }
                if let window = session.window, session.totalHours > window.maxHours {
                    EnrollmentBanner(tone: .danger, title: "Carga horária excedida") {
                        Text("Você está \(session.totalHours - window.maxHours)h acima do máximo de \(window.maxHours)h.")
                    }
                }
                if !unmet.isEmpty {
                    EnrollmentBanner(tone: .warn, title: "Pré-requisitos pendentes") {
                        Text("\(unmet.map(\.discipline.code).joined(separator: ", ")) \(unmet.count > 1 ? "dependem" : "depende") de análise do colegiado.")
                    }
                }
            }
            .padding(.bottom, 14)
        }
    }

    // MARK: Picks

    @ViewBuilder
    private var picks: some View {
        let resolved = store.session.resolvedPicks
        if resolved.isEmpty {
            VStack(spacing: 4) {
                Text("Nenhuma disciplina na proposta ainda")
                    .font(.system(size: 17, weight: .bold))
                    .tracking(-0.34)
                    .foregroundStyle(UNESColor.ink)
                Text("Volte ao catálogo e escolha suas turmas.")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
            }
            .frame(maxWidth: .infinity)
            .padding(EdgeInsets(top: 40, leading: 16, bottom: 40, trailing: 16))
            .enrollmentCard()
        } else {
            VStack(spacing: 0) {
                EnrollmentSectionHeader(title: "Selecionadas · \(resolved.count)")
                VStack(spacing: 12) {
                    ForEach(resolved) { pick in
                        EnrollmentReviewItem(
                            pick: pick,
                            readonly: store.isReadonly,
                            onRemove: { store.send(.removeTapped(pick.id), animation: UNESMotion.ease(0.3)) },
                            onAllowsOther: { store.send(.allowsOtherChanged(pick.id, $0)) },
                            onWaitlist: { store.send(.waitlistChanged(pick.id, $0)) }
                        )
                    }
                }
            }
        }
    }

    // MARK: Dock

    private var submitDock: some View {
        let blockers = store.session.blockers
        return VStack(spacing: 10) {
            if !store.session.picks.isEmpty, !blockers.isEmpty {
                HStack(spacing: 7) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.system(size: 8, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(width: 15, height: 15)
                        .background(EnrollmentTone.danger, in: Circle())
                    Text(blockers.map(EnrollmentFormat.blockerLabel).joined(separator: " · "))
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(EnrollmentTone.danger)
                        .lineLimit(1)
                    Spacer(minLength: 0)
                }
            }

            HStack(spacing: 10) {
                Button {
                    store.send(.saveTapped)
                } label: {
                    Image(systemName: "square.and.arrow.down")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundStyle(UNESColor.ink2)
                        .frame(width: 52, height: 50)
                        .background(UNESColor.card, in: RoundedRectangle(cornerRadius: 15, style: .continuous))
                        .overlay {
                            RoundedRectangle(cornerRadius: 15, style: .continuous)
                                .strokeBorder(UNESColor.line)
                        }
                }
                .buttonStyle(TilePressStyle())
                .accessibilityLabel("Salvar rascunho")

                Button {
                    store.send(.submitTapped)
                } label: {
                    HStack(spacing: 8) {
                        if store.isSubmitting {
                            SpinnerRing(size: 16, color: .white, trackColor: .white.opacity(0.4))
                            Text("Enviando…")
                        } else {
                            Text("Enviar proposta")
                            Image(systemName: "paperplane.fill")
                                .font(.system(size: 13, weight: .semibold))
                        }
                    }
                    .font(.system(size: 15, weight: .semibold))
                    .tracking(-0.15)
                    .foregroundStyle(store.canSubmit ? .white : UNESColor.ink4)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(
                        store.canSubmit ? UNESColor.accent : UNESColor.surface3,
                        in: RoundedRectangle(cornerRadius: 15, style: .continuous)
                    )
                    .shadow(color: store.canSubmit ? UNESColor.accent.opacity(0.4) : .clear, radius: 8, y: 6)
                }
                .buttonStyle(TilePressStyle())
                .disabled(!store.canSubmit || store.isSubmitting)
            }
        }
        .padding(14)
        .enrollmentDockChrome()
    }

}

/// One reviewed pick: tint rail, identity row with the remove button, the
/// schedule, status badges, and the per-pick toggles.
struct EnrollmentReviewItem: View {
    var pick: EnrollmentResolvedPick
    var readonly: Bool
    var onRemove: () -> Void
    var onAllowsOther: (Bool) -> Void
    var onWaitlist: (Bool) -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                Rectangle()
                    .fill(pick.discipline.tint)
                    .frame(width: 4)

                VStack(alignment: .leading, spacing: 8) {
                    identityRow
                    Text(pick.discipline.name)
                        .font(.system(size: 17, weight: .bold))
                        .tracking(-0.43)
                        .foregroundStyle(UNESColor.ink)
                        .fixedSize(horizontal: false, vertical: true)
                    EnrollmentScheduleLines(section: pick.section)
                    badges
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(EdgeInsets(top: 14, leading: 15, bottom: 14, trailing: 15))
            }

            if !readonly {
                toggles
            }
        }
        .enrollmentCard()
    }

    private var identityRow: some View {
        HStack(spacing: 8) {
            EnrollmentCodeChip(code: pick.discipline.code, color: pick.discipline.tint, small: true)
            Text(pick.section.label)
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(UNESColor.ink2)
            Text("· \(pick.discipline.workload)h")
                .font(.system(size: 11, weight: .medium))
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink4)

            Spacer()

            if !readonly {
                Button {
                    onRemove()
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(UNESColor.ink3)
                        .frame(width: 28, height: 28)
                        .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 9, style: .continuous))
                        .overlay {
                            RoundedRectangle(cornerRadius: 9, style: .continuous)
                                .strokeBorder(UNESColor.line)
                        }
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Remover \(pick.discipline.code)")
            }
        }
    }

    @ViewBuilder
    private var badges: some View {
        if pick.discipline.hasUnmetPrereq || pick.waitlist || !pick.section.hasSchedule {
            HStack(spacing: 8) {
                if pick.discipline.hasUnmetPrereq {
                    EnrollmentBadge(kind: .prereq, text: "Pré-req. pendente")
                }
                // Keyed on the pick, not the seats — the student may have
                // opted out of the queue for a full section.
                if pick.waitlist {
                    EnrollmentBadge(kind: .waitlist, text: waitlistBadgeText)
                }
                if !pick.section.hasSchedule {
                    EnrollmentBadge(kind: .optional, text: "Horário a definir")
                }
            }
        }
    }

    private var waitlistBadgeText: String {
        let ahead = pick.section.waitlistCount
        return ahead > 0 ? "Fila · \(ahead + 1)º" : "Fila · na espera"
    }

    private var toggles: some View {
        VStack(spacing: 12) {
            toggleRow(
                title: "Aceitar outra turma",
                subtitle: "Sem vaga na \(pick.section.label)? Me aloque em outra turma de \(pick.discipline.code).",
                isOn: pick.allowsOther,
                onChange: onAllowsOther
            )
            if pick.section.seats.isFull {
                Divider().overlay(UNESColor.line)
                toggleRow(
                    title: "Entrar na fila de espera",
                    subtitle: "Turma lotada — você concorre conforme a fila.",
                    isOn: pick.waitlist,
                    onChange: onWaitlist
                )
            }
        }
        .padding(EdgeInsets(top: 12, leading: 15, bottom: 12, trailing: 15))
        .overlay(alignment: .top) {
            Rectangle().fill(UNESColor.line).frame(height: 0.5)
        }
    }

    private func toggleRow(title: String, subtitle: String, isOn: Bool, onChange: @escaping (Bool) -> Void) -> some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 13, weight: .semibold))
                    .tracking(-0.13)
                    .foregroundStyle(UNESColor.ink)
                Text(subtitle)
                    .font(.system(size: 11.5, weight: .medium))
                    .lineSpacing(2)
                    .foregroundStyle(UNESColor.ink3)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Toggle(title, isOn: Binding(get: { isOn }, set: { onChange($0) }))
                .labelsHidden()
        }
    }
}

#Preview("Revisar") {
    NavigationStack {
        EnrollmentReviewView(
            store: Store(initialState: EnrollmentReviewFeature.State(session: .preview)) {
                EnrollmentReviewFeature()
            }
        )
    }
}

#Preview("Com bloqueios") {
    var session = EnrollmentSession.preview
    session.picks[1].sectionId = 30401
    session.picks.removeLast()
    return NavigationStack {
        EnrollmentReviewView(
            store: Store(initialState: EnrollmentReviewFeature.State(session: session)) {
                EnrollmentReviewFeature()
            }
        )
    }
}

#Preview("Comprovante") {
    var session = EnrollmentSession.preview
    session.window?.state = .closed
    return NavigationStack {
        EnrollmentReviewView(
            store: Store(initialState: EnrollmentReviewFeature.State(session: session)) {
                EnrollmentReviewFeature()
            }
        )
    }
}
