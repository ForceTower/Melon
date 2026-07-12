import ComposableArchitecture
import SwiftUI

/// The matrícula entry screen: identity strip, the window hero, the running
/// proposal tallies, and the call to action for the current window state.
struct EnrollmentView: View {
    @Bindable var store: StoreOf<EnrollmentFeature>
    @State private var titleProgress: CGFloat = 0

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            EnrollmentAmbientWash(variant: .warm, opacity: 0.3)
            content
        }
        .toolbar {
            ToolbarItem(placement: .principalCompat) {
                Text(.enrollmentTitle)
                    .font(.system(size: 16, weight: .semibold))
                    .tracking(-0.32)
                    .foregroundStyle(UNESColor.ink)
                    .opacity(titleProgress)
                    .offset(y: (1 - titleProgress) * 6)
            }
        }
        .inlineNavigationBar()
        .hiddenTabBar()
        .task { await store.send(.task).finish() }
    }

    private var content: some View {
        ScrollView {
            VStack(spacing: 0) {
                header
                    .fadeUp(delay: 0.02)

                VStack(spacing: 0) {
                    if let window = store.session.window {
                        loaded(window)
                    } else if store.isLoading {
                        loadingState
                    } else if let message = store.errorMessage {
                        errorState(message)
                    } else {
                        emptyState
                    }
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

    // MARK: Loaded

    @ViewBuilder
    private func loaded(_ window: EnrollmentWindow) -> some View {
        let session = store.session
        let conflictCount = session.conflicts.count

        EnrollmentHeroCard(window: window, now: store.referenceDate)
            .scaleIn(delay: 0.08, duration: 0.62)
            .padding(.bottom, 20)

        if store.isLoading {
            loadingState
        } else if let message = store.errorMessage {
            // The window resolved but the offers fetch failed — surface the
            // retry instead of an empty catalogue.
            errorState(message)
        } else {
            HStack(spacing: 12) {
                EnrollmentStatTile(
                    label: .localized(.enrollmentStatDisciplines),
                    value: session.picks.count,
                    hint: .localized(session.picks.count == 1 ? .enrollmentStatClassesHintOne : .enrollmentStatClassesHintOther)
                )
                EnrollmentStatTile(
                    label: .localized(.enrollmentStatConflicts),
                    value: conflictCount,
                    hint: .localized(conflictCount == 0 ? .enrollmentStatAllClear : .enrollmentStatResolve),
                    tone: conflictCount == 0 ? EnrollmentTone.ok : EnrollmentTone.danger
                )
                EnrollmentStatTile(label: .localized(.enrollmentStatQueue), value: session.waitlistedCount, hint: .localized(.enrollmentStatWaitingHint))
            }
            .fadeUp(delay: 0.16)
            .padding(.bottom, 20)

            EnrollmentWorkloadCard(
                totalHours: session.totalHours,
                minHours: window.minHours,
                maxHours: window.maxHours,
                compact: true
            )
            .fadeUp(delay: 0.22)
            .padding(.bottom, 22)

            if !session.resolvedPicks.isEmpty {
                proposalPreview(isReadonly: session.isReadonly)
                    .fadeUp(delay: 0.28)
                    .padding(.bottom, 22)
            }

            callToAction(window)
                .fadeUp(delay: 0.34)

            Text(.enrollmentEntryWorkloadBounds(window.minHours, window.maxHours))
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .frame(maxWidth: .infinity)
                .padding(EdgeInsets(top: 18, leading: 24, bottom: 4, trailing: 24))
                .fadeUp(delay: 0.4)
        }
    }

    private func proposalPreview(isReadonly: Bool) -> some View {
        VStack(spacing: 0) {
            EnrollmentSectionHeader(
                title: .enrollmentEntryProposalTitle,
                action: isReadonly ? nil : .commonEdit,
                onAction: { store.send(.startTapped) }
            )
            VStack(spacing: 0) {
                let picks = store.session.resolvedPicks
                ForEach(picks) { pick in
                    EnrollmentProposalRow(pick: pick) {
                        store.send(.proposalRowTapped(pick.discipline.id))
                    }
                    if pick.id != picks.last?.id {
                        Divider()
                            .overlay(UNESColor.line)
                            .padding(.leading, 30)
                    }
                }
            }
            .enrollmentCard()
        }
    }

    @ViewBuilder
    private func callToAction(_ window: EnrollmentWindow) -> some View {
        if store.session.isReadonly {
            VStack(spacing: 10) {
                Button {
                    store.send(.reviewTapped)
                } label: {
                    Text(.enrollmentEntryViewReceipt)
                }
                .buttonStyle(.unesAccent)
                Button {
                    store.send(.reopenTapped)
                } label: {
                    Text(.enrollmentEntryReopen)
                }
                .buttonStyle(.unesNeutral)
            }
        } else if window.state == .upcoming {
            Text(.enrollmentEntryUpcomingNotice)
                .font(.system(size: 16, weight: .semibold))
                .tracking(-0.16)
                .foregroundStyle(UNESColor.ink3)
                .frame(maxWidth: .infinity)
                .frame(height: 54)
                .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .strokeBorder(UNESColor.line)
                }
        } else {
            // Open, or closed-but-reopened — both continue into the catalogue.
            Button {
                store.send(.startTapped)
            } label: {
                UNESButtonLabel(text: store.session.picks.isEmpty ? .enrollmentBuildStart : .enrollmentBuildContinue)
            }
            .buttonStyle(.unesAccent)
        }
    }

    // MARK: Load states

    private var loadingState: some View {
        VStack(spacing: 14) {
            SpinnerRing(size: 26, color: UNESColor.accent, trackColor: UNESColor.surface3)
            Text(.enrollmentLoading)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 64)
    }

    private func errorState(_ message: String) -> some View {
        VStack(spacing: 14) {
            EnrollmentBanner(tone: .danger, title: String.localized(.enrollmentEntryLoadFailTitle)) {
                Text(message)
            }
            Button {
                store.send(.retryTapped)
            } label: {
                Text(.commonTryAgain)
            }
            .buttonStyle(.unesNeutral)
        }
        .padding(.top, 16)
    }

    private var emptyState: some View {
        VStack(spacing: 8) {
            Image(systemName: "moon.zzz")
                .font(.system(size: 26, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .padding(.bottom, 6)
            Text(.enrollmentEntryEmptyTitle)
                .font(.system(size: 17, weight: .bold))
                .tracking(-0.34)
                .foregroundStyle(UNESColor.ink)
            Text(.enrollmentEntryEmptyBody)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
                .multilineTextAlignment(.center)
                .frame(maxWidth: 260)
        }
        .frame(maxWidth: .infinity)
        .padding(EdgeInsets(top: 44, leading: 16, bottom: 44, trailing: 16))
        .enrollmentCard()
        .padding(.top, 8)
    }

    // MARK: Header

    private var header: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 5) {
                Text(.enrollmentTitle)
                    .font(.system(size: 40, weight: .bold))
                    .tracking(-1.6)
                    .foregroundStyle(UNESColor.ink)
                if let course = store.profile?.course {
                    Text(course)
                        .font(.system(size: 14, weight: .medium))
                        .tracking(-0.14)
                        .foregroundStyle(UNESColor.ink3)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            if let initial = store.profile?.name.first.map(String.init) {
                Text(initial)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 40, height: 40)
                    .background(
                        LinearGradient.css(
                            stops: [
                                .init(color: UNESColor.teal, location: 0),
                                .init(color: Color(hex: 0x5B3B8C), location: 1),
                            ],
                            angle: 135
                        ),
                        in: Circle()
                    )
                    .shadow(color: UNESColor.teal.opacity(0.3), radius: 6, y: 4)
                    .padding(.top, 6)
            }
        }
        .padding(EdgeInsets(top: 2, leading: 20, bottom: 16, trailing: 20))
    }

}

/// One row of the "Sua proposta" preview: tint rail, code + turma, name,
/// hours and the drill-in chevron.
struct EnrollmentProposalRow: View {
    var pick: EnrollmentResolvedPick
    var onTap: () -> Void

    var body: some View {
        Button {
            onTap()
        } label: {
            HStack(spacing: 12) {
                RoundedRectangle(cornerRadius: 2, style: .continuous)
                    .fill(pick.discipline.tint)
                    .frame(width: 4, height: 34)

                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 7) {
                        Text(pick.discipline.code)
                            .font(.system(size: 10, weight: .bold))
                            .tracking(0.3)
                            .foregroundStyle(pick.discipline.tint)
                        Text(pick.section.label)
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundStyle(UNESColor.ink4)
                    }
                    Text(pick.discipline.name)
                        .font(.system(size: 15, weight: .semibold))
                        .tracking(-0.3)
                        .foregroundStyle(UNESColor.ink)
                        .lineLimit(1)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Text(verbatim: "\(pick.discipline.workload)h")
                    .font(.system(size: 12, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink4)
                Image(systemName: "chevron.right")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(UNESColor.ink4)
            }
            .padding(EdgeInsets(top: 12, leading: 14, bottom: 12, trailing: 14))
            .contentShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

#Preview("Aberta") {
    NavigationStack {
        EnrollmentView(
            store: Store(initialState: EnrollmentFeature.State(profile: .preview, session: .preview)) {
                EnrollmentFeature()
            }
        )
    }
}

#Preview("Carregando") {
    NavigationStack {
        EnrollmentView(
            store: Store(initialState: EnrollmentFeature.State(profile: .preview)) {
                EnrollmentFeature()
            } withDependencies: {
                $0.enrollmentRepository.window = {
                    try await Task.never()
                }
            }
        )
    }
}

#Preview("Sem janela") {
    NavigationStack {
        EnrollmentView(
            store: Store(initialState: EnrollmentFeature.State(profile: .preview)) {
                EnrollmentFeature()
            } withDependencies: {
                $0.enrollmentRepository.window = { nil }
            }
        )
    }
}
