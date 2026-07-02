import ComposableArchitecture
import SwiftUI

struct HomeView: View {
    @Bindable var store: StoreOf<HomeFeature>

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
            .navigationTitle("Hoje")
            .toolbar {
                ToolbarItem(placement: .trailingCompat) {
                    avatarButton
                }
            }
        } destination: { store in
            switch store.case {
            case let .detail(store):
                DisciplineDetailView(store: store)
            }
        }
        .task { await store.send(.task).finish() }
    }

    // MARK: Content

    private func loaded(_ overview: HomeOverview) -> some View {
        ScrollView {
            VStack(spacing: 0) {
                eyebrow
                    .fadeUp(delay: 0.02)

                VStack(spacing: 0) {
                    if let hero = overview.hero {
                        HomeHeroCard(hero: hero) {
                            guard let id = hero.disciplineId else { return }
                            store.send(.disciplineTapped(id: id, name: hero.disciplineName))
                        }
                        .scaleIn(delay: 0.1, duration: 0.62)
                        .padding(.bottom, 22)
                    }

                    HomeWidgetGrid(overview: overview) {
                        store.send(.messagesWidgetTapped)
                    }
                    .fadeUp(delay: 0.2)
                    .padding(.bottom, 26)

                    HomeDaySection(today: overview.today) {
                        store.send(.seeScheduleTapped)
                    } onOpenClass: { item in
                        store.send(.disciplineTapped(id: item.disciplineId, name: item.title))
                    }
                    .fadeUp(delay: 0.3)
                    .padding(.bottom, 26)
                }
                .padding(.horizontal, 16)

                if !overview.disciplines.isEmpty {
                    HomeClassesCarousel(disciplines: overview.disciplines) {
                        store.send(.seeAllClassesTapped)
                    } onOpen: { discipline in
                        store.send(.disciplineTapped(id: discipline.id, name: discipline.name))
                    }
                    .fadeUp(delay: 0.38)
                    .padding(.bottom, 20)
                }

                footer
                    .fadeUp(delay: 0.46)
            }
            .padding(.bottom, 12)
        }
        .scrollIndicators(.hidden)
        .refreshable {
            await store.send(.refreshPulled).finish()
        }
    }

    /// The accent date line under the system large title.
    private var eyebrow: some View {
        Text(HomeFormat.dayEyebrow(for: .now))
            .textCase(.uppercase)
            .font(.system(size: 13, weight: .semibold))
            .tracking(0.2)
            .foregroundStyle(UNESColor.accent)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(EdgeInsets(top: 2, leading: 20, bottom: 16, trailing: 20))
    }

    private var avatarButton: some View {
        Button {
            store.send(.avatarTapped)
        } label: {
            Text(avatarInitial)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(.white)
                .frame(width: 32, height: 32)
                .background(
                    LinearGradient.css(
                        stops: [
                            .init(color: UNESColor.coral, location: 0),
                            .init(color: UNESColor.amber, location: 1),
                        ],
                        angle: 135
                    ),
                    in: Circle()
                )
        }
        .buttonStyle(.plain)
    }

    private var avatarInitial: String {
        store.userName.flatMap { $0.first.map { String($0).uppercased() } } ?? "•"
    }

    private var footer: some View {
        TimelineView(.everyMinute) { context in
            if let lastRefreshed = store.lastRefreshed {
                Text(HomeFormat.updatedLabel(lastRefreshed: lastRefreshed, now: context.date))
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 4)
            }
        }
    }

    /// Faint warm mesh washing down from behind the large title.
    private var ambientWash: some View {
        MeshView(variant: .warm, intensity: 0.5)
            .frame(height: 320)
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
            .opacity(0.28)
            .offset(y: -80)
            .ignoresSafeArea()
    }

    private func errorState(_ message: String) -> some View {
        VStack(spacing: 8) {
            Text("Não deu para carregar seu dia")
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

#Preview {
    HomeView(
        store: Store(initialState: HomeFeature.State()) {
            HomeFeature()
        }
    )
}
