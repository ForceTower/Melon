@preconcurrency import Umbrella

// Bundle of the KMP use cases Overview observes. Kept un-isolated so the
// ViewModel's background iteration tasks can call `invoke()` without hopping
// actors on every step. Mirrors the `SyncUseCases` pattern in
// `OnboardingFactory.swift`.
struct OverviewUseCases {
    let header: OverviewObserveOverviewHeaderUseCase
    let nowClass: OverviewObserveNowClassUseCase
    let today: OverviewObserveTodayTimelineUseCase
    let disciplines: OverviewObserveDisciplinesUseCase
    let messagesTile: OverviewObserveUnreadMessagesTileUseCase
    let nextTestTile: OverviewObserveNextTestTileUseCase
    let attendanceTile: OverviewObserveAttendanceTileUseCase
    let gradeTile: OverviewObserveGradeTileUseCase
    let lastSync: OverviewObserveLastSyncUseCase
}

// Feature-scoped factory. Marked @MainActor to match OnboardingFactory —
// factories are constructed and consumed exclusively on the main actor.
@MainActor
struct OverviewFactory {
    let useCases: OverviewUseCases

    func makeViewModel() -> OverviewViewModel {
        OverviewViewModel(useCases: useCases)
    }
}
