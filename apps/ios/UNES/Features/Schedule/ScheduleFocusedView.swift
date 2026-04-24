import SwiftUI

/// Day-focused schedule variant. Week pills up top, a single expanded day
/// column below. Mirrors `UNES Schedule.html` from the design handoff — an
/// alternative to `ScheduleGridView` that trades the at-a-glance matrix for
/// a deeper read on the currently-selected day.
struct ScheduleFocusedView: View {
    private let disciplinesFactory: DisciplinesFactory
    @State private var viewModel: ScheduleFocusedViewModel
    @State private var activeIdx: Int
    @State private var entering: Bool = true
    @State private var hasEntered: Bool = false

    init(factory: ScheduleFocusedFactory, disciplinesFactory: DisciplinesFactory) {
        self.disciplinesFactory = disciplinesFactory
        _viewModel = State(initialValue: factory.makeViewModel())
        // Seed the active pill with today's Mon..Sun index so the view renders
        // in the right place before the KMP flow emits its first snapshot.
        _activeIdx = State(initialValue: Self.todayMondayFirstIndex())
    }

    private var weekRange: String { viewModel.weekRangeLabel }

    var body: some View {
        NavigationStack {
            screenBody
                .navigationDestination(for: Discipline.self) { seed in
                    DisciplineDetailView(seed: seed, factory: disciplinesFactory)
                }
                .toolbar(.hidden, for: .navigationBar)
        }
    }

    private var screenBody: some View {
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
                    WeekSpine(
                        activeIdx: $activeIdx,
                        week: viewModel.week,
                        dates: viewModel.dates,
                        todayIdx: viewModel.todayIdx,
                        entering: entering
                    )
                    DayColumn(
                        classes: classesForActiveDay,
                        isToday: activeIdx == viewModel.todayIdx,
                        nowMin: viewModel.nowMin,
                        entering: entering
                    )
                    .id(activeIdx)
                }
                .padding(.bottom, 100)
            }
        }
        .task {
            await viewModel.observe()
        }
        .onAppear {
            guard !hasEntered else { return }
            hasEntered = true
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.4) {
                entering = false
            }
        }
    }

    private var classesForActiveDay: [ScheduleClass] {
        guard viewModel.week.indices.contains(activeIdx) else { return [] }
        return viewModel.week[activeIdx]
    }

    // Mon(0)..Sun(6) index of today, derived from the Cocoa weekday field
    // which is 1=Sunday..7=Saturday — the offset `(w + 5) % 7` lands on
    // Monday-first indexing without a lookup table.
    private static func todayMondayFirstIndex() -> Int {
        let weekday = Calendar.current.component(.weekday, from: Date())
        return (weekday + 5) % 7
    }

    // MARK: - Header

    private var header: some View {
        HStack(alignment: .bottom, spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text("◦ SEMANA \(String(format: "%02d", viewModel.weekNumber))")
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

            if viewModel.todayIdx != -1 && activeIdx != viewModel.todayIdx {
                Button {
                    withAnimation(.spring(response: 0.4, dampingFraction: 0.78)) {
                        activeIdx = viewModel.todayIdx
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
        .padding(.top, 16)
        .padding(.bottom, 16)
    }
}

