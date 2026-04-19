package dev.forcetower.melon.umbrella

import dev.forcetower.melon.core.network.BaseUrl
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.feature.auth.domain.usecase.LoginUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.RefreshActiveSemestersUseCase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory

@DependencyGraph(AppScope::class)
interface SharedGraph {
    val loginUseCase: LoginUseCase
    val refreshUseCase: RefreshActiveSemestersUseCase
    val sessionStore: SessionStore

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides baseUrl: BaseUrl): SharedGraph
    }
}

fun SharedGraph(config: SharedConfig): SharedGraph =
    createGraphFactory<SharedGraph.Factory>().create(BaseUrl(config.baseUrl))
