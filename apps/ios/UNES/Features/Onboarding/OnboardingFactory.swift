import SwiftUI
@preconcurrency import Umbrella

struct SyncUseCases {
    let ping: SyncPingActivityUseCase
    let profile: SyncSyncProfileUseCase
    let semesterList: SyncSyncSemesterListUseCase
    let semester: SyncSyncSemesterUseCase
    let messages: SyncSyncMessagesUseCase
    let onboardingStatus: SyncFetchOnboardingStatusUseCase
    let registerToken: NotificationsRegisterNotificationTokenUseCase
}

@MainActor
struct OnboardingFactory {
    let loginUseCase: AuthLoginUseCase
    let getReadyOverviewUseCase: DashboardGetReadyOverviewUseCase
    let syncUseCases: SyncUseCases

    func makeLogin(onSubmit: @escaping (String) -> Void) -> LoginView {
        LoginView(loginUseCase: loginUseCase, onSubmit: onSubmit)
    }

    func makeSync(
        displayName: String,
        onDone: @escaping () -> Void,
        onAuthFailed: @escaping () -> Void
    ) -> SyncView {
        let viewModel = SyncViewModel(
            useCases: syncUseCases,
            onDone: onDone,
            onAuthFailed: onAuthFailed
        )
        return SyncView(name: displayName, viewModel: viewModel)
    }

    func makeReady(userName: String, onEnter: @escaping () -> Void) -> ReadyView {
        ReadyView(
            userName: userName,
            onEnter: onEnter,
            viewModel: ReadyViewModel(useCase: getReadyOverviewUseCase)
        )
    }
}
