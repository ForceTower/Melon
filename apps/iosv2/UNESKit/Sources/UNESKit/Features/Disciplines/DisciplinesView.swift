import ComposableArchitecture
import SwiftUI

struct DisciplinesView: View {
    @Bindable var store: StoreOf<DisciplinesFeature>

    var body: some View {
        NavigationStack(path: $store.scope(state: \.path, action: \.path)) {
            ZStack(alignment: .top) {
                UNESColor.surface.ignoresSafeArea()
                ambientWash

                if let overview = store.overview {
                    loaded(overview)
                } else if let message = store.errorMessage {
                    errorState(message)
                } else {
                    SpinnerRing(size: 28, color: UNESColor.accent, trackColor: UNESColor.surface3)
                        .frame(maxHeight: .infinity)
                }
            }
            .navigationTitle("Turmas")
            .alert($store.scope(state: \.alert, action: \.alert))
        } destination: { store in
            switch store.case {
            case let .detail(store):
                DisciplineDetailView(store: store)
            }
        }
        .task { await store.send(.task).finish() }
    }

    // MARK: Content

    private func loaded(_ overview: DisciplinesOverview) -> some View {
        ScrollView {
            VStack(spacing: 0) {
                if let current = overview.current {
                    eyebrow(code: current.code)
                        .fadeUp(delay: 0.02)
                }

                VStack(spacing: 0) {
                    if let current = overview.current {
                        ScoreHeroCard(semester: current)
                            .fadeUp(delay: 0.1)
                            .padding(.bottom, 22)

                        VStack(spacing: 12) {
                            ForEach(Array(current.disciplines.enumerated()), id: \.element.id) { index, discipline in
                                DisciplineSummaryCard(discipline: discipline) {
                                    store.send(.disciplineTapped(semesterId: current.id, discipline: discipline))
                                }
                                .fadeUp(delay: 0.18 + Double(index) * 0.06)
                            }
                        }
                    }

                    if !overview.past.isEmpty || !overview.pending.isEmpty {
                        DividerLabel(text: "Histórico")
                            .fadeUp(delay: 0.5)
                            .padding(EdgeInsets(top: 26, leading: 4, bottom: 12, trailing: 4))

                        history(overview)
                            .fadeUp(delay: 0.56)
                    }

                    if overview.isEmpty {
                        emptyState
                    }
                }
                .padding(.horizontal, 16)
            }
            .padding(.bottom, 12)
        }
        .refreshable {
            await store.send(.refreshPulled).finish()
        }
    }

    /// The accent semester line under the system large title.
    private func eyebrow(code: String) -> some View {
        Text("Semestre \(DisciplinesFormat.semesterLabel(code))")
            .textCase(.uppercase)
            .font(.system(size: 13, weight: .semibold))
            .tracking(0.2)
            .foregroundStyle(UNESColor.accent)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(EdgeInsets(top: 2, leading: 20, bottom: 16, trailing: 20))
    }

    private func history(_ overview: DisciplinesOverview) -> some View {
        VStack(spacing: 10) {
            ForEach(Array(overview.past.enumerated()), id: \.element.id) { index, semester in
                PastSemesterCard(
                    semester: semester,
                    initiallyExpanded: index == 0 || store.recentlyDownloadedIds.contains(semester.id)
                ) { discipline in
                    store.send(.disciplineTapped(semesterId: semester.id, discipline: discipline))
                }
            }
            ForEach(overview.pending) { pending in
                DownloadSemesterCard(
                    semester: pending,
                    isDownloading: store.downloadingSemesterIds.contains(pending.id)
                ) {
                    store.send(.downloadSemesterTapped(pending.id))
                }
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 8) {
            Text("Nenhuma turma por aqui")
                .font(.system(size: 17, weight: .semibold))
                .tracking(-0.34)
                .foregroundStyle(UNESColor.ink)
            Text("Suas disciplinas aparecem assim que a primeira sincronização terminar.")
                .font(.system(size: 13))
                .foregroundStyle(UNESColor.ink3)
                .multilineTextAlignment(.center)
        }
        .padding(.horizontal, 32)
        .padding(.top, 80)
    }

    /// Faint warm mesh washing down from behind the large title.
    private var ambientWash: some View {
        MeshView(variant: .warm, intensity: 0.5)
            .frame(height: 300)
            .padding(.horizontal, -50)
            .mask {
                LinearGradient(
                    stops: [
                        .init(color: .white, location: 0),
                        .init(color: .clear, location: 0.92),
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
            .opacity(0.26)
            .offset(y: -80)
            .ignoresSafeArea()
    }

    private func errorState(_ message: String) -> some View {
        VStack(spacing: 8) {
            Text("Não deu para carregar suas turmas")
                .font(.system(size: 17, weight: .semibold))
                .tracking(-0.34)
                .foregroundStyle(UNESColor.ink)
            Text(message)
                .font(.system(size: 13))
                .foregroundStyle(UNESColor.ink3)
                .multilineTextAlignment(.center)
            Button("Tentar novamente") {
                store.send(.refreshPulled)
            }
            .font(.system(size: 15, weight: .semibold))
            .foregroundStyle(UNESColor.accent)
            .padding(.top, 8)
        }
        .padding(.horizontal, 32)
        .frame(maxHeight: .infinity)
    }
}

/// "── HISTÓRICO ──" — hairlines flanking an uppercase label.
struct DividerLabel: View {
    var text: String

    var body: some View {
        HStack(spacing: 12) {
            Rectangle().fill(UNESColor.line).frame(height: 1)
            Text(text)
                .textCase(.uppercase)
                .font(.system(size: 12, weight: .semibold))
                .tracking(0.72)
                .foregroundStyle(UNESColor.ink4)
            Rectangle().fill(UNESColor.line).frame(height: 1)
        }
    }
}

#Preview {
    DisciplinesView(
        store: Store(initialState: DisciplinesFeature.State()) {
            DisciplinesFeature()
        }
    )
}
