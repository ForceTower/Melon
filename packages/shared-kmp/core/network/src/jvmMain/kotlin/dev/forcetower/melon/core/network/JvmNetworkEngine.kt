package dev.forcetower.melon.core.network

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

@ContributesTo(AppScope::class)
interface JvmNetworkEngineGraph {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun engine(): HttpClientEngine = OkHttp.create()
    }
}
