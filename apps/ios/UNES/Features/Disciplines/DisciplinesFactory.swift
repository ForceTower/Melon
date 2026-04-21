@preconcurrency import Umbrella

// Bundle of the KMP use cases the Disciplinas screen leans on. The list is
// observed via `observeList`; `syncSemester` powers the "BAIXAR" action on
// placeholder cards. Mirrors OverviewUseCases / ScheduleFocusedUseCases so
// the view model stays free of direct UmbrellaGraph knowledge.
struct DisciplinesUseCases {
    let observeList: DisciplinesObserveDisciplinesListUseCase
    let syncSemester: SyncSyncSemesterUseCase
}

@MainActor
struct DisciplinesFactory {
    let useCases: DisciplinesUseCases

    func makeViewModel() -> DisciplinesListViewModel {
        DisciplinesListViewModel(useCases: useCases)
    }
}
