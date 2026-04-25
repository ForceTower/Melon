@preconcurrency import Umbrella

// Bundle of KMP use cases the Settings screen leans on. Mirrors the
// `MeUseCases`/`MeFactory` pattern so the Configurações screen has its own
// factory wiring instead of leaning on Me's view model — keeps the two
// screens decoupled even though the credentials use case lives in
// `feature/me` for now.
struct SettingsUseCases {
    let observeCredentials: MeObserveCurrentCredentialsUseCase
    let observeLastSync: OverviewObserveLastSyncUseCase
    let observeSettings: SettingsObserveSettingsUseCase
    let updateSettings: SettingsUpdateSettingsUseCase
}

@MainActor
struct SettingsFactory {
    let useCases: SettingsUseCases

    func makeViewModel() -> SettingsViewModel {
        SettingsViewModel(useCases: useCases)
    }
}
