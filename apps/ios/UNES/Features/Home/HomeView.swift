import SwiftUI

struct HomeView: View {
    @State private var activeTab: HomeTabKey = .home

    var body: some View {
        TabView(selection: $activeTab) {
            Tab(HomeTabKey.home.label, systemImage: HomeTabKey.home.icon, value: .home) {
                HomeTabContent()
            }
            Tab(HomeTabKey.schedule.label, systemImage: HomeTabKey.schedule.icon, value: .schedule) {
                PlaceholderTab(title: HomeTabKey.schedule.label)
            }
            Tab(HomeTabKey.classes.label, systemImage: HomeTabKey.classes.icon, value: .classes) {
                PlaceholderTab(title: HomeTabKey.classes.label)
            }
            Tab(HomeTabKey.messages.label, systemImage: HomeTabKey.messages.icon, value: .messages) {
                PlaceholderTab(title: HomeTabKey.messages.label)
            }
            .badge(HomeTabKey.messages.badge ?? 0)
            Tab(HomeTabKey.me.label, systemImage: HomeTabKey.me.icon, value: .me) {
                PlaceholderTab(title: HomeTabKey.me.label)
            }
        }
        .tint(UNESColor.accent)
    }
}

private struct HomeTabContent: View {
    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()

            // Ambient mesh pinned to the top. Dimness is baked into the mesh
            // intensity so the gradient can fade all the way to fully-opaque
            // surface at the bottom — avoids a seam where the block ends.
            VStack(spacing: 0) {
                ZStack {
                    MeshGradientView(variant: .warm, intensity: 0.2)
                    LinearGradient(
                        stops: [
                            .init(color: .clear, location: 0.0),
                            .init(color: UNESColor.surface, location: 1.0),
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                }
                .frame(height: 260)
                Spacer(minLength: 0)
            }
            .frame(maxWidth: .infinity)
            .ignoresSafeArea(edges: .top)
            .allowsHitTesting(false)

            ScrollView(showsIndicators: false) {
                VStack(spacing: 14) {
                    HomeHeader()

                    VStack(spacing: 14) {
                        NowCard(now: HomeFixtures.nowClass)
                        TodayTimeline(items: HomeFixtures.today)
                        HomeTileGrid()
                    }
                    .padding(.horizontal, 14)

                    DisciplinesStrip(items: HomeFixtures.disciplines)

                    Text("◦ ATUALIZADO HÁ 2 MIN ◦")
                        .font(UNESFont.mono(9))
                        .tracking(1.26)
                        .foregroundStyle(UNESColor.ink4)
                        .padding(.top, 8)
                        .padding(.bottom, 24)
                }
            }
        }
    }
}

private struct PlaceholderTab: View {
    let title: String
    var body: some View {
        ZStack {
            UNESColor.surface.ignoresSafeArea()
            Text(title)
                .font(UNESFont.serif(32))
                .foregroundStyle(UNESColor.ink3)
        }
    }
}

#Preview {
    HomeView()
}
