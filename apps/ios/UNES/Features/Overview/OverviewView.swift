import SwiftUI

struct OverviewView: View {
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
                    OverviewHeader()
                        .fadeUpOnAppear(delay: 0.02, distance: 14, duration: 0.55)

                    VStack(spacing: 14) {
                        NowCard(now: OverviewFixtures.nowClass)
                            .fadeScaleInOnAppear(delay: 0.12, from: 0.985, duration: 0.6, anchor: .top)
                        TodayTimeline(items: OverviewFixtures.today)
                            .fadeUpOnAppear(delay: 0.24, distance: 14, duration: 0.55)
                        OverviewTileGrid()
                            .fadeUpOnAppear(delay: 0.34, distance: 14, duration: 0.55)
                    }
                    .padding(.horizontal, 14)

                    DisciplinesStrip(items: OverviewFixtures.disciplines)
                        .fadeUpOnAppear(delay: 0.44, distance: 14, duration: 0.55)

                    Text("◦ ATUALIZADO HÁ 2 MIN ◦")
                        .font(UNESFont.mono(9))
                        .tracking(1.26)
                        .foregroundStyle(UNESColor.ink4)
                        .padding(.top, 8)
                        .padding(.bottom, 24)
                        .fadeUpOnAppear(delay: 0.52, distance: 14, duration: 0.55)
                }
            }
        }
    }
}

#Preview {
    OverviewView()
}
