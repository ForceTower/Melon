import SwiftUI

struct OverviewView: View {
    @State private var viewModel: OverviewViewModel
    private let disciplinesFactory: DisciplinesFactory
    private let onOpenMessages: () -> Void

    init(
        factory: OverviewFactory,
        disciplinesFactory: DisciplinesFactory,
        onOpenMessages: @escaping () -> Void = {}
    ) {
        _viewModel = State(initialValue: factory.makeViewModel())
        self.disciplinesFactory = disciplinesFactory
        self.onOpenMessages = onOpenMessages
    }

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
                    OverviewHeader(
                        greeting: viewModel.greeting,
                        dateEyebrow: viewModel.dateEyebrow,
                        name: viewModel.firstName,
                        avatarInitial: viewModel.avatarInitial
                    )
                    .fadeUpOnAppear(delay: 0.02, distance: 14, duration: 0.55)

                    VStack(spacing: 14) {
                        if let now = viewModel.now {
                            NowCard(now: now)
                                .fadeScaleInOnAppear(delay: 0.12, from: 0.985, duration: 0.6, anchor: .top)
                        }
                      
                        if !viewModel.today.isEmpty {
                            TodayTimeline(items: viewModel.today)
                                .fadeUpOnAppear(delay: 0.24, distance: 14, duration: 0.55)
                        }
                      
                        OverviewTileGrid(
                            grade: viewModel.gradeTile,
                            messages: viewModel.messagesTile,
                            nextTest: viewModel.nextTestTile,
                            attendance: viewModel.attendanceTile,
                            onOpenMessages: onOpenMessages
                        )
                        .fadeUpOnAppear(delay: 0.34, distance: 14, duration: 0.55)
                    }
                    .padding(.horizontal, 14)

                    DisciplinesStrip(
                        items: viewModel.disciplines,
                        semesterLabel: viewModel.semesterLabel
                    )
                    .fadeUpOnAppear(delay: 0.44, distance: 14, duration: 0.55)

                    Text(viewModel.lastUpdatedLabel)
                        .font(UNESFont.mono(9))
                        .tracking(1.26)
                        .foregroundStyle(UNESColor.ink4)
                        .padding(.top, 8)
                        .padding(.bottom, 24)
                        .fadeUpOnAppear(delay: 0.52, distance: 14, duration: 0.55)
                }
            }
        }
        .task {
            await viewModel.observe()
        }
    }
}
