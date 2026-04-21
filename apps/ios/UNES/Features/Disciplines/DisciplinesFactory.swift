@preconcurrency import Umbrella

// Bundle of the KMP use cases the Disciplinas screen leans on. `observeList`
// feeds the list; `observeDetail` feeds the detail screen (scoped by
// offerId); `syncSemester` powers the "BAIXAR" action on placeholder cards.
// Mirrors OverviewUseCases / ScheduleFocusedUseCases so the view model stays
// free of direct UmbrellaGraph knowledge.
struct DisciplinesUseCases {
    let observeList: DisciplinesObserveDisciplinesListUseCase
    let observeDetail: DisciplinesObserveDisciplineDetailUseCase
    let syncSemester: SyncSyncSemesterUseCase
}

@MainActor
struct DisciplinesFactory {
    let useCases: DisciplinesUseCases

    func makeViewModel() -> DisciplinesListViewModel {
        DisciplinesListViewModel(useCases: useCases)
    }

    // Seeded with the tapped list-card `Discipline` so the detail screen
    // renders instantly against the list data before the DB flow emits its
    // first hydrated payload.
    func makeDetailViewModel(seed: Discipline) -> DisciplineDetailViewModel {
        DisciplineDetailViewModel(seed: seed, useCases: useCases)
    }
}
