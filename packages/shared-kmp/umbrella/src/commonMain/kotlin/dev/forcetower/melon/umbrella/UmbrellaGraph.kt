package dev.forcetower.melon.umbrella

import dev.forcetower.melon.core.network.BaseUrl
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.feature.auth.domain.usecase.LoginUseCase
import dev.forcetower.melon.feature.notifications.domain.usecase.RegisterNotificationTokenUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.RefreshActiveSemestersUseCase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory

@DependencyGraph(AppScope::class)
interface UmbrellaGraph {
    val loginUseCase: LoginUseCase
    val refreshUseCase: RefreshActiveSemestersUseCase
    val registerNotificationTokenUseCase: RegisterNotificationTokenUseCase
    val sessionStore: SessionStore

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides baseUrl: BaseUrl): UmbrellaGraph
    }
}

fun UmbrellaGraph(config: UmbrellaConfig): UmbrellaGraph =
    createGraphFactory<UmbrellaGraph.Factory>().create(BaseUrl(config.baseUrl))
