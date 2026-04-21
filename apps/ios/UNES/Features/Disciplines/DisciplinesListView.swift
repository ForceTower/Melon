import SwiftUI

/// Disciplines ("Boletim") list screen. Mirrors `UNES Disciplines.html`:
/// current-semester cards up top, then a "Histórico" stack of collapsible
/// past-semester cards — with undownloaded placeholders that fetch lazily.
///
/// Owns a `NavigationStack` internally so that tapping into any discipline
/// pushes the detail screen — satisfying the "use NavigationStack for proper
/// navigation" requirement from the hand-off.
struct DisciplinesListView: View {
    private let factory: DisciplinesFactory
    @State private var viewModel: DisciplinesListViewModel
    @State private var path = NavigationPath()
    @State private var justDownloaded: Set<String> = []

    init(factory: DisciplinesFactory) {
        self.factory = factory
        _viewModel = State(initialValue: factory.makeViewModel())
    }

    var body: some View {
        NavigationStack(path: $path) {
            screenBody
                .navigationDestination(for: Discipline.self) { discipline in
                    DisciplineDetailView(seed: discipline, factory: factory)
                }
                .navigationTitle("Disciplinas")
                .toolbar(.hidden, for: .navigationBar)
        }
        .task {
            await viewModel.observe()
        }
    }

    private var screenBody: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()

            // Ambient mesh pinned to the top, fading out so the mesh edge
            // never reads as a hard seam against the surface.
            VStack(spacing: 0) {
                ZStack {
                    MeshGradientView(variant: .warm, intensity: 0.22)
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
            .ignoresSafeArea(edges: .top)
            .allowsHitTesting(false)

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    header
                        .fadeUpOnAppear(delay: 0.02, distance: 14, duration: 0.55)

                    if let current = viewModel.current {
                        CurrentSemesterSummary(disciplines: current.disciplines)
                            .padding(.bottom, 14)
                            .fadeUpOnAppear(delay: 0.08, distance: 14, duration: 0.55)

                        VStack(spacing: 10) {
                            ForEach(Array(current.disciplines.enumerated()), id: \.element.id) { idx, d in
                                NavigationLink(value: d) {
                                    ActiveDisciplineCard(discipline: d)
                                }
                                .buttonStyle(.plain)
                                .fadeUpOnAppear(delay: 0.15 + Double(idx) * 0.06,
                                                distance: 12, duration: 0.55)
                            }
                        }
                        .padding(.horizontal, 16)
                    }

                    historyDivider
                        .fadeUpOnAppear(delay: 0.5, distance: 10, duration: 0.55)

                    VStack(spacing: 10) {
                        ForEach(Array(viewModel.past.enumerated()), id: \.element.id) { idx, sem in
                            PastSemesterCard(
                                semesterId: sem.id,
                                disciplines: sem.disciplines,
                                defaultOpen: shouldAutoOpen(sem, at: idx),
                                onOpen: { path.append($0) }
                            )
                        }

                        ForEach(viewModel.pending) { sem in
                            UndownloadedSemesterCard(
                                semesterId: sem.id,
                                estimatedCount: sem.estimatedCount,
                                onDownload: { handleDownload($0) }
                            )
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                    .fadeUpOnAppear(delay: 0.56, distance: 12, duration: 0.55)
                }
                .padding(.bottom, 32)
            }
        }
    }

    // MARK: - Header

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("◦ SEMESTRE \(viewModel.current?.id ?? "—")")
                .font(UNESFont.mono(10, weight: .medium))
                .tracking(1.2)
                .foregroundStyle(UNESColor.ink3)

            Text("Disciplinas")
                .font(UNESFont.serif(32))
                .tracking(-0.64)
                .foregroundStyle(UNESColor.ink)
        }
        .padding(.horizontal, 20)
        .padding(.top, 60)
        .padding(.bottom, 16)
    }

    private var historyDivider: some View {
        HStack(spacing: 10) {
            Rectangle().fill(UNESColor.line).frame(height: 1)
            Text("HISTÓRICO")
                .font(UNESFont.mono(10, weight: .semibold))
                .tracking(1.4)
                .foregroundStyle(UNESColor.ink4)
            Rectangle().fill(UNESColor.line).frame(height: 1)
        }
        .padding(.horizontal, 20)
        .padding(.top, 28)
        .padding(.bottom, 10)
    }

    // MARK: - Actions

    /// The most recent past semester opens by default unless the user just
    /// downloaded a newer semester (which takes precedence).
    private func shouldAutoOpen(_ sem: Semester, at idx: Int) -> Bool {
        if justDownloaded.contains(sem.id) { return true }
        return idx == 0 && !justDownloaded.contains(viewModel.past.first?.id ?? "")
    }

    private func handleDownload(_ semesterId: String) {
        justDownloaded.insert(semesterId)
        Task { await viewModel.download(semesterId) }
    }
}
