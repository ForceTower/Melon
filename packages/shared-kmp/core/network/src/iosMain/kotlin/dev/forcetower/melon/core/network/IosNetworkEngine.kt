@file:OptIn(kotlinx.cinterop.UnsafeNumber::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.forcetower.melon.core.network

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.convert
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling

@ContributesTo(AppScope::class)
interface IosNetworkEngineGraph {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun engine(): HttpClientEngine = Darwin.create {
            // We supply a challenge handler purely to inspect the server trust for
            // diagnostics. The handler ALWAYS returns PerformDefaultHandling so the
            // system performs the actual trust decision — our inspection is a
            // side-effect that records a TlsDiagnostic on failure but never
            // accepts or rejects a connection on its own.
            handleChallenge { _, _, challenge, completionHandler ->
                inspectTlsChallengeForDiagnostic(challenge)
                completionHandler(NSURLSessionAuthChallengePerformDefaultHandling.convert(), null)
            }
        }
    }
}
