@preconcurrency import Umbrella

// Bundle of KMP use cases the "Eu" hub leans on. Keeping a single-field struct
// instead of surfacing the use case directly lines the feature up with the
// Overview / Messages pattern and gives us room to grow (pinned-shortcut
// mutations, sign-out flows) without churning call sites.
struct MeUseCases {
    let observeProfile: MeObserveMeProfileUseCase
    let observeLastSync: OverviewObserveLastSyncUseCase
}

@MainActor
struct MeFactory {
    let useCases: MeUseCases
    let sessionStore: SessionSessionStore
    // Carried so `MeView` can hand it down to the pushed Settings screen
    // without `MeView` itself depending on `UmbrellaGraph`.
    let settingsFactory: SettingsFactory
    // Same idea for the "Calendário" shortcut destination.
    let calendarFactory: CalendarFactory

    func makeViewModel() -> MeViewModel {
        MeViewModel(useCases: useCases, sessionStore: sessionStore)
    }
}
