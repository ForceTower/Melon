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
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveOverviewHeaderUseCase
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
}
