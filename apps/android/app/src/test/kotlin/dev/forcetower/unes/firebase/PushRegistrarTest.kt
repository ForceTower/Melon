package dev.forcetower.unes.firebase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.feature.notifications.domain.usecase.RegisterNotificationTokenUseCase
import dev.forcetower.melon.feature.notifications.domain.usecase.UnregisterNotificationTokenUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
internal class PushRegistrarTest {
    private var accessToken: String? = "departing-bearer"
    private val sessionStore = mockk<SessionStore>().also { store ->
        coEvery { store.getAccessToken() } answers { accessToken }
        coEvery { store.logout() } coAnswers {
            delay(10)
            accessToken = null
        }
    }
    private val identifierStore = mockk<PushIdentifierStore>(relaxed = true).also { store ->
        coEvery { store.token() } returns "fcm-token"
        coEvery { store.fid() } returns null
        coEvery { store.pendingDeletes() } returns emptySet()
    }
    private val register = mockk<RegisterNotificationTokenUseCase>().also { useCase ->
        coEvery { useCase(any(), any(), any(), any(), any(), any()) } returns Outcome.Ok(Unit)
    }
    private val deleted = CompletableDeferred<Unit>()
    private val unregister = mockk<UnregisterNotificationTokenUseCase>().also { useCase ->
        coEvery { useCase(any()) } coAnswers {
            deleted.await()
            Outcome.Ok(Unit)
        }
    }
    private val registrar = PushRegistrar(identifierStore, sessionStore, register, unregister)

    @Test
    fun aReconcileQueuedDuringLogoutDoesNotReRegisterTheDepartingAccount() =
        runTest {
            launch { registrar.unregisterAll(endSession = sessionStore::logout) }
            runCurrent()
            launch { registrar.reconcile() }
            runCurrent()

            deleted.complete(Unit)
            advanceUntilIdle()

            coVerify(exactly = 0) { register(any(), any(), any(), any(), any(), any()) }
            coVerify(exactly = 1) { sessionStore.logout() }
        }

    @Test
    fun theSessionEndsEvenWhenTheTeardownFails() =
        runTest {
            deleted.complete(Unit)
            coEvery { identifierStore.clear() } throws IllegalStateException("disk full")

            runCatching { registrar.unregisterAll(endSession = sessionStore::logout) }

            coVerify(exactly = 1) { sessionStore.logout() }
        }
}
