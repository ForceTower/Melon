import ComposableArchitecture
import SwiftUI

/// One "Explorar" category as a ranked list, fed by the overview payload.
@Reducer
struct ParadoxoExploreFeature {
    @ObservableState
    struct State: Equatable {
        let ranking: ParadoxoRanking
    }

    enum Action: Equatable {
        case task
        case entryTapped(ParadoxoRankedEntry)
        case delegate(Delegate)

        enum Delegate: Equatable {
            case open(ParadoxoEntityRef, name: String)
        }
    }

    @Dependency(\.analytics) var analytics

    private let log = Log.scoped("ParadoxoExploreFeature")

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                // Matches the Android route's `kind.name` (Pascal-case), not the
                // lowercase `rawValue` used for select_content — mirrors the
                // Android screen_view payload verbatim.
                analytics.screen(
                    name: Screens.paradoxoExplore,
                    properties: ["kind": state.ranking.kind.rawValue.capitalized]
                )
                return .none
            case let .entryTapped(entry):
                log.info("open ranked entry kind=\(entry.ref.kind.rawValue) id=\(entry.ref.id)")
                analytics.selectContent(
                    contentType: ContentTypes.paradoxoEntity,
                    itemId: entry.ref.id,
                    properties: ["kind": entry.ref.kind.rawValue]
                )
                return .send(.delegate(.open(entry.ref, name: entry.name)))
            case .delegate:
                return .none
            }
        }
    }
}

struct ParadoxoExploreView: View {
    let store: StoreOf<ParadoxoExploreFeature>

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    HStack(spacing: 10) {
                        Image(systemName: store.ranking.kind.icon)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(store.ranking.kind.tone)
                            .frame(width: 32, height: 32)
                            .background(
                                store.ranking.kind.tone.opacity(0.13),
                                in: RoundedRectangle(cornerRadius: 10, style: .continuous)
                            )
                        Text(store.ranking.kind.subtitle)
                            .font(.system(size: 14, weight: .medium))
                            .foregroundStyle(UNESColor.ink3)
                    }
                    .fadeUp(delay: 0.02)
                    .padding(EdgeInsets(top: 4, leading: 4, bottom: 14, trailing: 4))

                    ParadoxoRowGroup(rows: store.ranking.entries) { entry in
                        ParadoxoRankedEntryRow(entry: entry) {
                            store.send(.entryTapped(entry))
                        }
                    }
                    .fadeUp(delay: 0.1)
                }
                .padding(EdgeInsets(top: 8, leading: 16, bottom: 24, trailing: 16))
            }
            .scrollIndicators(.hidden)
        }
        .navigationTitle(Text(store.ranking.kind.title))
        .navigationBarTitleDisplayMode(.large)
        .task { await store.send(.task).finish() }
    }
}

#Preview("Explorar lista") {
    NavigationStack {
        ParadoxoExploreView(
            store: Store(
                initialState: ParadoxoExploreFeature.State(
                    ranking: ParadoxoOverview.preview().rankings[0]
                )
            ) {
                ParadoxoExploreFeature()
            }
        )
    }
}
