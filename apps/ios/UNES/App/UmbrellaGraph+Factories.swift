@preconcurrency import Umbrella

// Decomposes the KMP-owned DI graph into feature-scoped factories. This file
// and the app entry point are the only places that should reference
// `UmbrellaGraph` directly — feature code takes a factory.
extension UmbrellaGraph {
    @MainActor
    var onboardingFactory: OnboardingFactory {
        OnboardingFactory(
            loginUseCase: loginUseCase,
            getReadyOverviewUseCase: getReadyOverviewUseCase,
            syncUseCases: SyncUseCases(
                ping: pingActivityUseCase,
                profile: syncProfileUseCase,
                semesterList: syncSemesterListUseCase,
                semester: syncSemesterUseCase,
                messages: syncMessagesUseCase,
                onboardingStatus: fetchOnboardingStatusUseCase,
                registerToken: registerNotificationTokenUseCase
            )
        )
    }

    @MainActor
    var overviewFactory: OverviewFactory {
        OverviewFactory(
            useCases: OverviewUseCases(
                header: observeOverviewHeaderUseCase,
                nowClass: observeNowClassUseCase,
                today: observeTodayTimelineUseCase,
                disciplines: observeDisciplinesUseCase,
                messagesTile: observeUnreadMessagesTileUseCase,
                nextTestTile: observeNextTestTileUseCase,
                attendanceTile: observeAttendanceTileUseCase,
                lastSync: observeLastSyncUseCase
            )
        )
    }

    @MainActor
    var scheduleFocusedFactory: ScheduleFocusedFactory {
        ScheduleFocusedFactory(
            useCases: ScheduleFocusedUseCases(
                scheduleWeek: observeScheduleWeekUseCase
            )
        )
    }

    @MainActor
    var disciplinesFactory: DisciplinesFactory {
        DisciplinesFactory(
            useCases: DisciplinesUseCases(
                observeList: observeDisciplinesListUseCase,
                syncSemester: syncSemesterUseCase
            )
        )
    }
}
