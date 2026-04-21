@preconcurrency import Umbrella

// Bundle of the KMP use cases the focused schedule observes. Mirrors the
// `OverviewUseCases` pattern so the view model stays free of direct
// `UmbrellaGraph` knowledge.
struct ScheduleFocusedUseCases {
    let scheduleWeek: ScheduleObserveScheduleWeekUseCase
}

@MainActor
struct ScheduleFocusedFactory {
    let useCases: ScheduleFocusedUseCases

    func makeViewModel() -> ScheduleFocusedViewModel {
        ScheduleFocusedViewModel(useCases: useCases)
    }
}
