import ComposableArchitecture
import SwiftUI

struct ParadoxoView: View {
    @Bindable var store: StoreOf<ParadoxoFeature>

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            ambientWash
            content
        }
        .navigationTitle(Text(.paradoxoTitle))
        .navigationBarTitleDisplayMode(.large)
        .searchable(
            text: $store.searchQuery,
            placement: Self.searchPlacement,
            prompt: Text(.paradoxoSearchPrompt)
        )
        .task { await store.send(.task).finish() }
    }

    /// The pinned drawer placement doesn't exist on watchOS, where this
    /// screen is never mounted anyway.
    private static var searchPlacement: SearchFieldPlacement {
        #if os(watchOS)
        .automatic
        #else
        .navigationBarDrawer(displayMode: .always)
        #endif
    }

    @ViewBuilder
    private var content: some View {
        if store.isSearching {
            searchResults
        } else if let overview = store.overview {
            home(overview)
        } else if store.loadFailed {
            ParadoxoFailureView { store.send(.retryTapped) }
        } else {
            ParadoxoLoadingView()
        }
    }

    // MARK: Home

    private func home(_ overview: ParadoxoOverview) -> some View {
        ScrollView {
            VStack(spacing: 0) {
                header
                    .fadeUp(delay: 0.02)
                    .padding(.bottom, 18)

                ParadoxoPulseHero(facts: overview.pulse) { fact in
                    store.send(.pulseFactTapped(fact))
                }
                .scaleIn(delay: 0.1, duration: 0.62)
                .padding(.bottom, 24)

                VStack(spacing: 0) {
                    ParadoxoSectionHeader(.paradoxoSectionExplore)
                    ParadoxoExploreGrid { kind in
                        store.send(.exploreTapped(kind))
                    }
                }
                .fadeUp(delay: 0.2)
                .padding(.bottom, 26)

                if !overview.myDisciplines.isEmpty {
                    VStack(spacing: 0) {
                        ParadoxoSectionHeader(.paradoxoSectionMine, note: .paradoxoSectionMineNote)
                        ParadoxoRowGroup(rows: overview.myDisciplines) { summary in
                            ParadoxoDisciplineSummaryRow(summary: summary) {
                                store.send(.myDisciplineTapped(summary))
                            }
                        }
                    }
                    .fadeUp(delay: 0.28)
                    .padding(.bottom, 22)
                }

                if overview.studentCount > 0 {
                    Text(.paradoxoFooter(
                        ParadoxoFormat.count(overview.studentCount),
                        ParadoxoFormat.count(overview.meanCount)
                    ))
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
                    .fadeUp(delay: 0.36)
                }
            }
            .padding(EdgeInsets(top: 8, leading: 16, bottom: 24, trailing: 16))
        }
        .scrollIndicators(.hidden)
        .refreshable { await store.send(.refreshPulled).finish() }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 6) {
            Eyebrow(text: .localized(.paradoxoEyebrow))
            Text(.paradoxoSubtitle)
                .font(.system(size: 15, weight: .medium))
                .tracking(-0.15)
                .foregroundStyle(UNESColor.ink3)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 4)
    }

    // MARK: Search

    private var searchResults: some View {
        ScrollView {
            let results = store.searchResults
            VStack(alignment: .leading, spacing: 16) {
                if !results.disciplines.isEmpty {
                    resultSection(.paradoxoSearchDisciplines, entries: results.disciplines)
                }
                if !results.teachers.isEmpty {
                    resultSection(.paradoxoSearchTeachers, entries: results.teachers)
                }
                if results.disciplines.isEmpty, results.teachers.isEmpty {
                    searchEmpty
                }
            }
            .padding(EdgeInsets(top: 14, leading: 16, bottom: 28, trailing: 16))
        }
        .scrollIndicators(.hidden)
        .scrollDismissesKeyboard(.immediately)
    }

    private func resultSection(_ title: LocalizedStringResource, entries: [ParadoxoIndexEntry]) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.system(size: 13, weight: .semibold))
                .tracking(-0.13)
                .foregroundStyle(UNESColor.ink3)
                .padding(.horizontal, 4)
            ParadoxoRowGroup(rows: entries) { entry in
                ParadoxoIndexEntryRow(entry: entry) {
                    store.send(.searchResultTapped(entry))
                }
            }
        }
    }

    private var searchEmpty: some View {
        VStack(spacing: 14) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 20, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .frame(width: 52, height: 52)
                .background(UNESColor.surface2, in: Circle())
            Text(.paradoxoSearchEmpty(store.searchQuery))
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 56)
    }

    /// Faint cool mesh washing down from behind the large title.
    private var ambientWash: some View {
        MeshView(variant: .cool, intensity: 0.5)
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
}

#Preview("Paradoxo") {
    NavigationStack {
        ParadoxoView(
            store: Store(initialState: ParadoxoFeature.State()) {
                ParadoxoFeature()
            }
        )
    }
}

#Preview("Carregando") {
    NavigationStack {
        ParadoxoView(
            store: Store(initialState: ParadoxoFeature.State()) {
                ParadoxoFeature()
            } withDependencies: {
                $0.paradoxoRepository.overview = {
                    try await Task.sleep(for: .seconds(86_400))
                    return .preview()
                }
                $0.paradoxoRepository.index = { [] }
            }
        )
    }
}

#Preview("Falha") {
    NavigationStack {
        ParadoxoView(
            store: Store(initialState: ParadoxoFeature.State()) {
                ParadoxoFeature()
            } withDependencies: {
                $0.paradoxoRepository.overview = { throw APIError.emptyEnvelope }
                $0.paradoxoRepository.index = { [] }
            }
        )
    }
}
