import SwiftUI

/// Day-focused schedule variant. Week pills up top, a single expanded day
/// column below. Mirrors `UNES Schedule.html` from the design handoff — an
/// alternative to `ScheduleGridView` that trades the at-a-glance matrix for
/// a deeper read on the currently-selected day.
struct ScheduleFocusedView: View {
    @State private var activeIdx: Int = ScheduleFixtures.todayIdx
    @State private var entering: Bool = true
    @State private var hasEntered: Bool = false

    private var weekRange: String {
        "\(ScheduleFixtures.dates.first ?? 0) – \(ScheduleFixtures.dates.last ?? 0) abr"
    }

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()

            // Ambient warm mesh behind the header, fading cleanly into the
            // surface so there is no visible seam at the mesh's bottom edge.
            VStack(spacing: 0) {
                ZStack {
                    MeshGradientView(variant: .warm, intensity: 0.28)
                    LinearGradient(
                        stops: [
                            .init(color: .clear, location: 0),
                            .init(color: UNESColor.surface, location: 0.95),
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                }
                .frame(height: 300)
                Spacer(minLength: 0)
            }
            .frame(maxWidth: .infinity)
            .ignoresSafeArea(edges: .top)
            .allowsHitTesting(false)

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    header
                        .fadeUpOnAppear(delay: 0.02, distance: 14, duration: 0.5)
                    WeekSpine(activeIdx: $activeIdx, entering: entering)
                    DayColumn(dayIdx: activeIdx, entering: entering)
                        .id(activeIdx)
                }
                .padding(.bottom, 100)
            }
        }
        .onAppear {
            guard !hasEntered else { return }
            hasEntered = true
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.4) {
                entering = false
            }
        }
    }

    // MARK: - Header

    private var header: some View {
        HStack(alignment: .bottom, spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text("◦ SEMANA \(String(format: "%02d", ScheduleFixtures.weekNumber))")
                    .font(UNESFont.mono(10, weight: .medium))
                    .tracking(1.2)
                    .foregroundStyle(UNESColor.ink3)
                    .lineLimit(1)
                Text("Horário")
                    .font(UNESFont.serif(32))
                    .tracking(-0.64)
                    .foregroundStyle(UNESColor.ink)
                Text(weekRange)
                    .font(UNESFont.sans(13))
                    .foregroundStyle(UNESColor.ink3)
                    .padding(.top, 2)
            }

            Spacer(minLength: 8)

            if activeIdx != ScheduleFixtures.todayIdx {
                Button {
                    withAnimation(.spring(response: 0.4, dampingFraction: 0.78)) {
                        activeIdx = ScheduleFixtures.todayIdx
                    }
                } label: {
                    HStack(spacing: 6) {
                        ZStack {
                            Circle()
                                .stroke(UNESColor.surface.opacity(0.4), lineWidth: 1)
                                .frame(width: 10, height: 10)
                            Circle()
                                .fill(UNESColor.surface)
                                .frame(width: 5, height: 5)
                        }
                        Text("hoje")
                            .font(UNESFont.sans(12, weight: .medium))
                            .tracking(-0.06)
                    }
                    .foregroundStyle(UNESColor.surface)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(
                        Capsule().fill(UNESColor.ink)
                    )
                }
                .buttonStyle(.plain)
                .transition(.opacity.combined(with: .scale(scale: 0.9)))
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 60)
        .padding(.bottom, 16)
    }
}

#Preview {
    ScheduleFocusedView()
}
