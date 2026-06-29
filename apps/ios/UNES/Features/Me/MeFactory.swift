@preconcurrency import Umbrella

// Bundle of KMP use cases the "Eu" hub leans on. Keeping a single-field struct
// instead of surfacing the use case directly lines the feature up with the
// Overview / Messages pattern and gives us room to grow (pinned-shortcut
// mutations, sign-out flows) without churning call sites.
struct MeUseCases {
    let observeProfile: MeObserveMeProfileUseCase
    let observeLastSync: OverviewObserveLastSyncUseCase
    let overallScore: DisciplinesCalculateOverallScoreUseCase
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
    // And for the "Matrícula" shortcut — the enrollment flow lives on the Me
    // hub's stack, so it gets its use cases through here.
    let enrollmentFactory: EnrollmentFactory

    func makeViewModel() -> MeViewModel {
        MeViewModel(useCases: useCases, sessionStore: sessionStore)
    }
}
