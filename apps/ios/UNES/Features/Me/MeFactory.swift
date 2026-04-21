@preconcurrency import Umbrella

// Bundle of KMP use cases the "Eu" hub leans on. Keeping a single-field struct
// instead of surfacing the use case directly lines the feature up with the
// Overview / Messages pattern and gives us room to grow (pinned-shortcut
// mutations, sign-out flows) without churning call sites.
struct MeUseCases {
    let observeProfile: MeObserveMeProfileUseCase
}

@MainActor
struct MeFactory {
    let useCases: MeUseCases

    func makeViewModel() -> MeViewModel {
        MeViewModel(useCases: useCases)
    }
}
