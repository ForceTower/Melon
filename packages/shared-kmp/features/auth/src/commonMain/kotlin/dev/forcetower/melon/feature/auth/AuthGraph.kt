package dev.forcetower.melon.feature.auth

import dev.forcetower.melon.core.session.SessionStore
import dev.forcetower.melon.feature.auth.data.AuthApi
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient

@ContributesTo(AppScope::class)
interface AuthGraph {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun authRepository(
            client: HttpClient,
            sessionStore: SessionStore,
        ): AuthRepository = AuthRepositoryImpl(AuthApi(client), sessionStore)
    }
}
