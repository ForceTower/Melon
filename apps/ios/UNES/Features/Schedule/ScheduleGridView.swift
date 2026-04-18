import SwiftUI

/// Week grid screen: warm mesh header → matrix at a glance → stacked day
/// sections for Mon–Fri. Matches the `ScheduleGridScreen` prototype from the
/// UNES design handoff.
struct ScheduleGridView: View {
    @State private var activeIdx: Int = ScheduleFixtures.todayIdx

    private var weekRange: String {
        "\(ScheduleFixtures.dates.first ?? 0) – \(ScheduleFixtures.dates.last ?? 0) abr"
    }

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()

            VStack(spacing: 0) {
                ZStack {
                    MeshGradientView(variant: .warm, intensity: 0.25)
                    LinearGradient(
                        stops: [
                            .init(color: .clear, location: 0),
                            .init(color: UNESColor.surface, location: 0.95),
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                }
                .frame(height: 280)
                Spacer(minLength: 0)
            }
            .frame(maxWidth: .infinity)
            .ignoresSafeArea(edges: .top)
            .allowsHitTesting(false)

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    header
                        .fadeUpOnAppear(delay: 0.02, distance: 14, duration: 0.55)
                    WeekMatrix(activeIdx: $activeIdx, onPick: { activeIdx = $0 })
                        .padding(.bottom, 24)
                        .fadeScaleInOnAppear(delay: 0.12, from: 0.985, duration: 0.6, anchor: .top)
                    ForEach([0, 1, 2, 3, 4], id: \.self) { i in
                        DaySection(dayIdx: i)
                            .fadeUpOnAppear(delay: 0.32 + Double(i) * 0.07, distance: 14, duration: 0.55)
                    }
                }
                .padding(.bottom, 100)
            }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("◦ SEMANA \(String(format: "%02d", ScheduleFixtures.weekNumber))")
                .font(UNESFont.mono(10, weight: .medium))
                .tracking(1.2)
                .foregroundStyle(UNESColor.ink3)
            Text("Horário")
                .font(UNESFont.serif(32))
                .tracking(-0.64)
                .foregroundStyle(UNESColor.ink)
            Text(weekRange)
                .font(UNESFont.sans(13))
                .foregroundStyle(UNESColor.ink3)
        }
        .padding(.horizontal, 20)
        .padding(.top, 60)
        .padding(.bottom, 18)
    }
}

#Preview {
    ScheduleGridView()
}
