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
import dev.forcetower.melon.feature.notifications.domain.usecase.RegisterNotificationTokenUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveOverviewHeaderUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.FetchOnboardingStatusUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.PingActivityUseCase
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

    // Overview — only the header is wired for the placeholder Home screen.
    @Provides fun provideObserveOverviewHeaderUseCase(graph: UmbrellaGraph): ObserveOverviewHeaderUseCase =
        graph.observeOverviewHeaderUseCase

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

    // Dashboard — Ready screen snapshot at the end of onboarding.
    @Provides fun provideGetReadyOverviewUseCase(graph: UmbrellaGraph): GetReadyOverviewUseCase =
        graph.getReadyOverviewUseCase

    // Notifications — token registration during the auth phase of sync.
    @Provides fun provideRegisterNotificationTokenUseCase(graph: UmbrellaGraph): RegisterNotificationTokenUseCase =
        graph.registerNotificationTokenUseCase
}
