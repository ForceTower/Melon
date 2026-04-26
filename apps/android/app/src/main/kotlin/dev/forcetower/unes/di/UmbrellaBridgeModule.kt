package dev.forcetower.unes.di

import android.content.Context
import co.touchlab.kermit.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext as HiltApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.forcetower.melon.core.common.ApplicationContext
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.feature.auth.domain.usecase.BeginPasskeyLoginUseCase
import dev.forcetower.melon.feature.auth.domain.usecase.CompletePasskeyLoginUseCase
import dev.forcetower.melon.feature.auth.domain.usecase.LoginUseCase
import dev.forcetower.melon.feature.dashboard.domain.usecase.GetReadyOverviewUseCase
import dev.forcetower.melon.feature.disciplines.domain.usecase.CalculateOverallScoreUseCase
import dev.forcetower.melon.feature.disciplines.domain.usecase.ObserveDisciplineDetailUseCase
import dev.forcetower.melon.feature.disciplines.domain.usecase.ObserveDisciplinesListUseCase
import dev.forcetower.melon.feature.me.domain.usecase.ObserveMeProfileUseCase
import dev.forcetower.melon.feature.messages.domain.usecase.MarkMessageAsReadUseCase
import dev.forcetower.melon.feature.messages.domain.usecase.ObserveMessageDetailUseCase
import dev.forcetower.melon.feature.messages.domain.usecase.ObserveMessagesInboxUseCase
import dev.forcetower.melon.feature.notifications.domain.usecase.RegisterNotificationTokenUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveAttendanceTileUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveDisciplinesUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveGradeTileUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveLastSyncUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveNextTestTileUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveNowClassUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveOverviewHeaderUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveTodayTimelineUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveUnreadMessagesTileUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.BackfillMirrorUseCase
import dev.forcetower.melon.feature.schedule.domain.usecase.ObserveScheduleWeekUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.FetchOnboardingStatusUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.PingActivityUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.RefreshSessionUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.SyncMessagesUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.SyncProfileUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.SyncSemesterListUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.SyncSemesterUseCase
import dev.forcetower.melon.umbrella.UmbrellaConfig
import dev.forcetower.melon.umbrella.UmbrellaGraph
import javax.inject.Singleton

// Bridges the Metro `UmbrellaGraph` from `:packages:shared-kmp:umbrella` into
// Hilt's SingletonComponent. The graph is created once at app startup; each
// use case is exposed as its own Hilt binding so screens can `@Inject` the
// specific dependency they need without ever seeing UmbrellaGraph.
//
// Pattern for adding a new use case: declare a one-line `@Provides` that
// returns `graph.<accessor>`. As features land in :app, append them here
// (or split into a per-feature bridge module if this file gets large).
@Module
@InstallIn(SingletonComponent::class)
object UmbrellaBridgeModule {

    @Provides
    @Singleton
    fun provideUmbrellaGraph(@HiltApplicationContext context: Context): UmbrellaGraph =
        UmbrellaGraph(
            UmbrellaConfig(
                // TODO: source from BuildConfig so debug builds can hit a local API.
                baseUrl = "https://melon.forcetower.dev",
                appContext = ApplicationContext(context),
            ),
        )

    @Provides fun provideSessionStore(graph: UmbrellaGraph): SessionStore = graph.sessionStore
    @Provides fun provideLogger(graph: UmbrellaGraph): Logger = graph.logger

    // Auth
    @Provides fun provideLoginUseCase(graph: UmbrellaGraph): LoginUseCase =
        graph.loginUseCase
    @Provides fun provideBeginPasskeyLoginUseCase(graph: UmbrellaGraph): BeginPasskeyLoginUseCase =
        graph.beginPasskeyLoginUseCase
    @Provides fun provideCompletePasskeyLoginUseCase(graph: UmbrellaGraph): CompletePasskeyLoginUseCase =
        graph.completePasskeyLoginUseCase

    // Overview — nine flows that drive the "Hoje" tab.
    @Provides fun provideObserveOverviewHeaderUseCase(graph: UmbrellaGraph): ObserveOverviewHeaderUseCase =
        graph.observeOverviewHeaderUseCase
    @Provides fun provideObserveNowClassUseCase(graph: UmbrellaGraph): ObserveNowClassUseCase =
        graph.observeNowClassUseCase
    @Provides fun provideObserveTodayTimelineUseCase(graph: UmbrellaGraph): ObserveTodayTimelineUseCase =
        graph.observeTodayTimelineUseCase
    @Provides fun provideObserveDisciplinesUseCase(graph: UmbrellaGraph): ObserveDisciplinesUseCase =
        graph.observeDisciplinesUseCase
    @Provides fun provideObserveUnreadMessagesTileUseCase(graph: UmbrellaGraph): ObserveUnreadMessagesTileUseCase =
        graph.observeUnreadMessagesTileUseCase
    @Provides fun provideObserveNextTestTileUseCase(graph: UmbrellaGraph): ObserveNextTestTileUseCase =
        graph.observeNextTestTileUseCase
    @Provides fun provideObserveAttendanceTileUseCase(graph: UmbrellaGraph): ObserveAttendanceTileUseCase =
        graph.observeAttendanceTileUseCase
    @Provides fun provideObserveGradeTileUseCase(graph: UmbrellaGraph): ObserveGradeTileUseCase =
        graph.observeGradeTileUseCase
    @Provides fun provideObserveLastSyncUseCase(graph: UmbrellaGraph): ObserveLastSyncUseCase =
        graph.observeLastSyncUseCase

    // Sync orchestration — six steps run by SyncViewModel during onboarding.
    @Provides fun providePingActivityUseCase(graph: UmbrellaGraph): PingActivityUseCase =
        graph.pingActivityUseCase
    @Provides fun provideSyncProfileUseCase(graph: UmbrellaGraph): SyncProfileUseCase =
        graph.syncProfileUseCase
    @Provides fun provideSyncSemesterListUseCase(graph: UmbrellaGraph): SyncSemesterListUseCase =
        graph.syncSemesterListUseCase
    @Provides fun provideSyncSemesterUseCase(graph: UmbrellaGraph): SyncSemesterUseCase =
        graph.syncSemesterUseCase
    @Provides fun provideSyncMessagesUseCase(graph: UmbrellaGraph): SyncMessagesUseCase =
        graph.syncMessagesUseCase
    @Provides fun provideFetchOnboardingStatusUseCase(graph: UmbrellaGraph): FetchOnboardingStatusUseCase =
        graph.fetchOnboardingStatusUseCase

    // Authenticated-shell sync — fired on every Connected entry and on
    // background → foreground transitions, mirroring iOS `ConnectedView`.
    @Provides fun provideRefreshSessionUseCase(graph: UmbrellaGraph): RefreshSessionUseCase =
        graph.refreshSessionUseCase
    @Provides fun provideBackfillMirrorUseCase(graph: UmbrellaGraph): BackfillMirrorUseCase =
        graph.backfillMirrorUseCase

    // Dashboard — Ready screen snapshot at the end of onboarding.
    @Provides fun provideGetReadyOverviewUseCase(graph: UmbrellaGraph): GetReadyOverviewUseCase =
        graph.getReadyOverviewUseCase

    // Notifications — token registration during the auth phase of sync.
    @Provides fun provideRegisterNotificationTokenUseCase(graph: UmbrellaGraph): RegisterNotificationTokenUseCase =
        graph.registerNotificationTokenUseCase

    // Me ("Eu") tab — single flow with the hero identity, semester strip data,
    // CR/hours rollup, and the closest upcoming evaluation. Lifetime CR (the
    // value rendered in the hero stat rail) comes from a separate use case
    // shared with the Overview grade tile.
    @Provides fun provideObserveMeProfileUseCase(graph: UmbrellaGraph): ObserveMeProfileUseCase =
        graph.observeMeProfileUseCase
    @Provides fun provideCalculateOverallScoreUseCase(graph: UmbrellaGraph): CalculateOverallScoreUseCase =
        graph.calculateOverallScoreUseCase

    // Disciplinas tab — list (current + past + pending semesters) and per-offer
    // detail. Pair with `SyncSemesterUseCase` (already provided above) for the
    // tap-to-fetch flow on pending placeholder cards. Mirrors iOS
    // `DisciplinesUseCases` in `DisciplinesFactory.swift`.
    @Provides fun provideObserveDisciplinesListUseCase(graph: UmbrellaGraph): ObserveDisciplinesListUseCase =
        graph.observeDisciplinesListUseCase
    @Provides fun provideObserveDisciplineDetailUseCase(graph: UmbrellaGraph): ObserveDisciplineDetailUseCase =
        graph.observeDisciplineDetailUseCase

    // Horário tab — single flow emitting the seven-day week, today index, and
    // the current week number; nowMin ticks in the ViewModel.
    @Provides fun provideObserveScheduleWeekUseCase(graph: UmbrellaGraph): ObserveScheduleWeekUseCase =
        graph.observeScheduleWeekUseCase

    // Mensagens tab — inbox observation, per-message detail observation, and
    // the local mark-as-read mutation (idempotent, so list and detail can
    // both invoke it without coordinating).
    @Provides fun provideObserveMessagesInboxUseCase(graph: UmbrellaGraph): ObserveMessagesInboxUseCase =
        graph.observeMessagesInboxUseCase
    @Provides fun provideObserveMessageDetailUseCase(graph: UmbrellaGraph): ObserveMessageDetailUseCase =
        graph.observeMessageDetailUseCase
    @Provides fun provideMarkMessageAsReadUseCase(graph: UmbrellaGraph): MarkMessageAsReadUseCase =
        graph.markMessageAsReadUseCase
}
