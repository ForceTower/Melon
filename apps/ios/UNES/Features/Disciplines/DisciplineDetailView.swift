import SwiftUI

/// Single-scroll discipline detail. Top to bottom:
/// hero / stats row / grades / ementa / classes / attachments / presença.
///
/// Pushed by `DisciplinesListView`'s `NavigationStack` — navigation state
/// lives in the parent so back is just a `dismiss()`. The view owns a
/// `DisciplineDetailViewModel` seeded with the tapped list-card `Discipline`;
/// while the DB flow hydrates, the screen renders against the seed.
struct DisciplineDetailView: View {
    @State private var viewModel: DisciplineDetailViewModel

    @State private var selectedGroup: String?

    init(seed: Discipline, factory: DisciplinesFactory) {
        _viewModel = State(initialValue: factory.makeDetailViewModel(seed: seed))
    }

    #if DEBUG
        // Factory-less init for `#Preview`. Skips the KMP subscription; the
        // screen renders against the seed fixture alone.
        fileprivate init(previewSeed seed: Discipline) {
            _viewModel = State(initialValue: DisciplineDetailViewModel(seed: seed))
        }
    #endif

    private var discipline: Discipline { viewModel.discipline }

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()

            // Ambient tint carrying the discipline's color across the top of
            // the screen — radial wash, fades into the surface.
            VStack(spacing: 0) {
                ZStack {
                    RadialGradient(
                        colors: [discipline.color.opacity(0.22), .clear],
                        center: .top,
                        startRadius: 0,
                        endRadius: 320
                    )
                }
                .frame(height: 340)
                Spacer(minLength: 0)
            }
            .ignoresSafeArea(edges: .top)
            .allowsHitTesting(false)

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    DisciplineDetailHero(
                        discipline: discipline,
                        selectedGroup: $selectedGroup
                    )

                    statsRow
                        .padding(.horizontal, 16)
                        .padding(.bottom, 22)
                        .fadeUpOnAppear(delay: 0.1, distance: 12, duration: 0.55)

                    DisciplineGradesBlock(discipline: discipline, selectedGroup: selectedGroup)
                        .fadeUpOnAppear(delay: 0.18, distance: 12, duration: 0.55)

                    DisciplineEmentaBlock(discipline: discipline)
                        .fadeUpOnAppear(delay: 0.24, distance: 12, duration: 0.55)

                    DisciplineClassesBlock(discipline: discipline)
                        .fadeUpOnAppear(delay: 0.3, distance: 12, duration: 0.55)

                    DisciplineAttachmentsBlock(discipline: discipline, selectedGroup: selectedGroup)
                        .fadeUpOnAppear(delay: 0.36, distance: 12, duration: 0.55)

                    DisciplineFaltasBlock(discipline: discipline)
                        .fadeUpOnAppear(delay: 0.42, distance: 12, duration: 0.55)
                }
                .padding(.bottom, 40)
            }
        }
        // Keep the system nav bar (so the back chevron + interactive swipe
        // gesture both work), but let the hero's color wash show through.
        .toolbarBackground(.hidden, for: .navigationBar)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) { EmptyView() }
        }
        .task {
            await viewModel.observe()
        }
    }

    // MARK: - Stats row

    private var statsRow: some View {
        HStack(spacing: 8) {
            DetailStatCard(
                label: "Carga horária",
                value: "\(discipline.hours)h",
                sub: "total da disciplina"
            ) {
                Image(systemName: "clock")
                    .font(.system(size: 10, weight: .medium))
                    .foregroundStyle(discipline.color)
            }

            DetailStatCard(
                label: "Faltas",
                value: "\(discipline.absences)",
                sub: "\(max(0, discipline.allowedAbsences - discipline.absences)) ainda disponíveis",
                color: absenceColor
            ) {
                Image(systemName: "list.bullet.rectangle")
                    .font(.system(size: 10, weight: .medium))
                    .foregroundStyle(discipline.absenceRisk == .ok ? UNESColor.ink4 : DisciplineScoreColor.caution)
            }
        }
    }

    private var absenceColor: Color {
        switch discipline.absenceRisk {
        case .risk: return DisciplineScoreColor.danger
        case .warn: return DisciplineScoreColor.caution
        case .ok:   return UNESColor.ink
        }
    }
}

#if DEBUG
    #Preview {
        NavigationStack {
            DisciplineDetailView(
                previewSeed: DisciplineFixtures.semesters
                    .first(where: { $0.id == DisciplineFixtures.currentSemesterId })!
                    .disciplines.first!
            )
        }
    }
#endif
