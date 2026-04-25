@preconcurrency import Umbrella

// Decomposes the KMP-owned DI graph into feature-scoped factories. This file
// and the app entry point are the only places that should reference
// `UmbrellaGraph` directly — feature code takes a factory.
extension UmbrellaGraph {
    @MainActor
    var onboardingFactory: OnboardingFactory {
        OnboardingFactory(
            loginUseCase: loginUseCase,
            beginPasskeyLogin: beginPasskeyLoginUseCase,
            completePasskeyLogin: completePasskeyLoginUseCase,
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
                gradeTile: observeGradeTileUseCase,
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
                observeDetail: observeDisciplineDetailUseCase,
                syncSemester: syncSemesterUseCase
            )
        )
    }

    @MainActor
    var messagesFactory: MessagesFactory {
        MessagesFactory(
            useCases: MessagesUseCases(
                observeInbox: observeMessagesInboxUseCase,
                observeDetail: observeMessageDetailUseCase,
                markRead: markMessageAsReadUseCase
            )
        )
    }

    @MainActor
    var meFactory: MeFactory {
        MeFactory(
            useCases: MeUseCases(
                observeProfile: observeMeProfileUseCase,
                observeLastSync: observeLastSyncUseCase,
                overallScore: calculateOverallScoreUseCase
            ),
            sessionStore: sessionStore,
            settingsFactory: settingsFactory,
            calendarFactory: calendarFactory
        )
    }

    @MainActor
    var calendarFactory: CalendarFactory {
        CalendarFactory(
            useCases: CalendarUseCases(
                observeEvents: observeCalendarEventsUseCase,
                observeActiveSemesterCode: observeActiveSemesterCodeUseCase
            )
        )
    }

    @MainActor
    var settingsFactory: SettingsFactory {
        SettingsFactory(
            useCases: SettingsUseCases(
                observeCredentials: observeCurrentCredentialsUseCase,
                observeLastSync: observeLastSyncUseCase,
                observeSettings: observeSettingsUseCase,
                updateSettings: updateSettingsUseCase
            )
        )
    }
}
