@preconcurrency import Umbrella

// Bundle of the KMP use cases the matrícula flow leans on. `window` is the cheap
// availability + window-status check; `offers` is the full disciplines tree;
// `submit` runs the open → publish → close transaction server-side. Mirrors
// DisciplinesUseCases / SettingsUseCases so the view model stays free of direct
// UmbrellaGraph knowledge.
struct EnrollmentUseCases {
    let window: EnrollmentGetEnrollmentWindowUseCase
    let offers: EnrollmentGetEnrollmentOffersUseCase
    let submit: EnrollmentSubmitEnrollmentUseCase
}

@MainActor
struct EnrollmentFactory {
    let useCases: EnrollmentUseCases

    func makeViewModel() -> EnrollmentViewModel {
        EnrollmentViewModel(useCases: useCases)
    }
}
