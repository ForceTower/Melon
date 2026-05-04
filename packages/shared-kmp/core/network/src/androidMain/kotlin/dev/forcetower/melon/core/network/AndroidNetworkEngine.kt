package dev.forcetower.melon.core.network

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

@ContributesTo(AppScope::class)
interface AndroidNetworkEngineGraph {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun engine(): HttpClientEngine = OkHttp.create {
            // Production trust path is the platform default — we never install a
            // custom X509TrustManager. The interceptor only runs AFTER OkHttp's
            // own TLS path has already accepted or rejected the connection.
            config {
                addInterceptor(TlsDiagnosticInterceptor())
            }
        }
    }
}
