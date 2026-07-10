import ComposableArchitecture
import SwiftUI

struct ParadoxoTeacherView: View {
    let store: StoreOf<ParadoxoTeacherFeature>

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            if let details = store.details {
                ParadoxoToneWash(tone: paradoxoTone(details.mean))
            }
            content
        }
        .navigationTitle(store.details?.name ?? store.name ?? "")
        .navigationBarTitleDisplayMode(.inline)
        .task { await store.send(.task).finish() }
    }

    @ViewBuilder
    private var content: some View {
        if let details = store.details {
            loaded(details)
        } else if store.loadFailed {
            ParadoxoFailureView { store.send(.retryTapped) }
        } else {
            ParadoxoLoadingView()
        }
    }

    private func loaded(_ details: ParadoxoTeacherDetails) -> some View {
        let tier = ParadoxoTier(mean: details.mean)
        return ScrollView {
            VStack(spacing: 18) {
                hero(details, tier: tier)
                    .scaleIn(delay: 0.02, duration: 0.62)

                stats(details)
                    .fadeUp(delay: 0.1)

                outcomes(details)
                    .fadeUp(delay: 0.16)

                if let signature = details.disciplines.max(by: { $0.sampleCount < $1.sampleCount }) {
                    ParadoxoInsightRow(
                        icon: "star.fill",
                        tone: tier.tone,
                        text: .paradoxoTeacherTeachesMost(
                            signature.name,
                            ParadoxoFormat.count(signature.sampleCount)
                        )
                    )
                    .paradoxoCard()
                    .fadeUp(delay: 0.22)
                }

                if !details.distribution.isEmpty {
                    signatureCard(details, tone: tier.tone)
                        .fadeUp(delay: 0.28)
                }

                if !details.disciplines.isEmpty {
                    disciplines(details)
                        .fadeUp(delay: 0.34)
                }
            }
            .padding(EdgeInsets(top: 8, leading: 16, bottom: 32, trailing: 16))
        }
        .scrollIndicators(.hidden)
    }

    // MARK: Hero

    private func hero(_ details: ParadoxoTeacherDetails, tier: ParadoxoTier) -> some View {
        VStack(spacing: 0) {
            ParadoxoDonut(
                score: details.mean,
                tone: tier.tone,
                label: String.localized(.paradoxoStudents(ParadoxoFormat.count(details.studentCount)))
            )
            Text(details.name)
                .font(.system(size: 21, weight: .bold))
                .tracking(-0.63)
                .foregroundStyle(UNESColor.ink)
                .multilineTextAlignment(.center)
                .padding(.top, 16)
            ParadoxoTierChip(mean: details.mean)
                .padding(.top, 10)
        }
        .frame(maxWidth: .infinity)
        .padding(EdgeInsets(top: 22, leading: 20, bottom: 20, trailing: 20))
        .background(alignment: .topTrailing) {
            Circle()
                .fill(tier.tone.opacity(0.12))
                .frame(width: 160, height: 160)
                .blur(radius: 8)
                .offset(x: 40, y: -50)
        }
        .paradoxoCard()
    }

    // MARK: Stats

    private func stats(_ details: ParadoxoTeacherDetails) -> some View {
        let approval = Int(ParadoxoStats.approvalPercent(
            approved: details.approved,
            failed: details.failed,
            quit: details.quit
        ).rounded())
        let consistency = ParadoxoStats.consistency(of: details.history.map(\.mean))

        return HStack(spacing: 10) {
            ParadoxoStatTile(
                label: .paradoxoTeacherApproval,
                value: ParadoxoFormat.percent(approval),
                tone: approval >= 60 ? UNESColor.successGreen : UNESColor.coral
            )
            if let consistency {
                ParadoxoStatTile(
                    label: .paradoxoTeacherConsistency,
                    value: ParadoxoFormat.percent(consistency),
                    tone: UNESColor.teal
                ) {
                    Sparkline(
                        values: details.history.map(\.mean),
                        color: UNESColor.teal,
                        size: CGSize(width: 54, height: 14),
                        lineWidth: 1.75
                    )
                }
            }
            if let lastSemester = details.lastSemester {
                ParadoxoStatTile(
                    label: .paradoxoTeacherLastSemester,
                    value: lastSemester,
                    tone: UNESColor.ink2
                )
            }
        }
    }

    // MARK: Outcomes

    private func outcomes(_ details: ParadoxoTeacherDetails) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(.paradoxoTeacherOutcomes)
                .font(.system(size: 16, weight: .bold))
                .tracking(-0.32)
                .foregroundStyle(UNESColor.ink)
            ParadoxoOutcomesBar(approved: details.approved, failed: details.failed, quit: details.quit)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .paradoxoCard()
    }

    // MARK: Grade signature

    private func signatureCard(_ details: ParadoxoTeacherDetails, tone: Color) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(.paradoxoTeacherSignature)
                    .font(.system(size: 16, weight: .bold))
                    .tracking(-0.32)
                    .foregroundStyle(UNESColor.ink)
                Spacer()
                Text(ParadoxoStats.shapeKind(of: details.distribution).label)
                    .font(.system(size: 11.5, weight: .bold))
                    .foregroundStyle(tone)
                    .padding(EdgeInsets(top: 3, leading: 9, bottom: 3, trailing: 9))
                    .background(tone.opacity(0.1), in: RoundedRectangle(cornerRadius: 8, style: .continuous))
            }

            ParadoxoDistributionChart(distribution: details.distribution, tone: tone, height: 120)

            Text(.paradoxoTeacherSignatureCaption(ParadoxoFormat.count(details.studentCount)))
                .font(.system(size: 12.5, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
                .frame(maxWidth: .infinity)
        }
        .padding(16)
        .paradoxoCard()
    }

    // MARK: Disciplines taught

    private func disciplines(_ details: ParadoxoTeacherDetails) -> some View {
        VStack(spacing: 0) {
            ParadoxoSectionHeader(.paradoxoTeacherDisciplines, note: .paradoxoTeacherDisciplinesNote)
            ParadoxoRowGroup(rows: details.disciplines) { discipline in
                ParadoxoRow(score: discipline.mean, title: discipline.name, onTap: {
                    store.send(.disciplineTapped(discipline))
                }) {
                    Text(discipline.code)
                        .foregroundStyle(UNESColor.ink4)
                    ParadoxoDot()
                    Text(.paradoxoSamples(ParadoxoFormat.count(discipline.sampleCount)))
                } accessory: {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(UNESColor.ink4)
                }
            }
        }
    }
}

#Preview("Professor") {
    NavigationStack {
        ParadoxoTeacherView(
            store: Store(initialState: ParadoxoTeacherFeature.State(teacherId: "t1")) {
                ParadoxoTeacherFeature()
            }
        )
    }
}

#Preview("Falha") {
    NavigationStack {
        ParadoxoTeacherView(
            store: Store(
                initialState: ParadoxoTeacherFeature.State(teacherId: "t1", name: "Joilma Silva Carneiro")
            ) {
                ParadoxoTeacherFeature()
            } withDependencies: {
                $0.paradoxoRepository.teacher = { _ in throw APIError.emptyEnvelope }
            }
        )
    }
}
