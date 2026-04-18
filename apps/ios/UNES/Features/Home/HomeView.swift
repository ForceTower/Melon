import SwiftUI

struct HomeView: View {
    @State private var activeTab: HomeTabKey = .home

    var body: some View {
        ZStack(alignment: .bottom) {
            UNESColor.surface.ignoresSafeArea()

            // Ambient mesh at the top, fading into the surface.
            ZStack {
                MeshGradientView(variant: .warm, intensity: 0.55)
                LinearGradient(
                    stops: [
                        .init(color: .clear, location: 0),
                        .init(color: UNESColor.surface, location: 0.9),
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
            .frame(height: 300)
            .opacity(0.35)
            .offset(y: -60)
            .frame(maxHeight: .infinity, alignment: .top)
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
