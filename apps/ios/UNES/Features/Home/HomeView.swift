import SwiftUI

struct HomeView: View {
    @State private var activeTab: HomeTabKey = .home

    var body: some View {
        ZStack(alignment: .bottom) {
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
                .padding(.bottom, 110)
            }

            LiquidTabBar(active: $activeTab)
                .padding(.bottom, 22)
        }
    }
}

#Preview {
    HomeView()
}
