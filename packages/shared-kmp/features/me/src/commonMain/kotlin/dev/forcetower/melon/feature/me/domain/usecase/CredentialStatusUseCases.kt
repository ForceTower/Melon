package dev.forcetower.melon.feature.me.domain.usecase

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.network.ApiEnvelope
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.feature.me.data.network.CredentialStatusResponse
import dev.forcetower.melon.feature.me.data.network.CredentialStatusService
import dev.forcetower.melon.feature.me.data.network.ReauthRequest
import dev.forcetower.melon.feature.me.domain.model.CredentialHealth
import dev.forcetower.melon.feature.me.domain.model.CredentialStatus
import dev.forcetower.melon.feature.me.domain.model.ReauthError
import dev.zacsweers.metro.Inject
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/**
 * Polls `api/me/status` and writes the answer into the session store, so the
 * banner is driven by persisted state rather than by whoever happens to be on
 * screen. Failures are silent: the last known answer stands.
 */
@Inject
class RefreshCredentialStatusUseCase internal constructor(
    private val service: CredentialStatusService,
    private val sessionStore: SessionStore,
    logger: Logger,
) {
    private val log = logger.withTag("CredentialStatus")

    suspend operator fun invoke() {
        val health = try {
            val response = service.status()
            if (!response.status.isSuccess()) return
            response.body<ApiEnvelope<CredentialStatusResponse>>().data?.credentials ?: return
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            log.w(t) { "status poll failed, keeping the last answer" }
            return
        }

        health.username?.let { sessionStore.setUpstreamUsername(it) }
        when (health.status.toDomain()) {
            CredentialStatus.Invalid -> sessionStore.setCredentialsInvalid(true)
            CredentialStatus.Ok -> sessionStore.setCredentialsInvalid(false)
            // Passkey-only accounts with nothing on file — not a problem to
            // nag about, and not a reason to clear a flag either.
            CredentialStatus.None -> Unit
        }
    }
}

/** Reactive banner state, persisted so it survives a cold start offline. */
@Inject
class ObserveCredentialHealthUseCase internal constructor(
    private val sessionStore: SessionStore,
) {
    operator fun invoke(): Flow<Boolean> = sessionStore.credentialsInvalid

    fun username(): Flow<String?> = sessionStore.upstreamUsername
}

/**
 * Submits a new portal password. The username is never sent — the server takes
 * it off the stored row, so this can't re-link a different account.
 */
@Inject
class ReauthenticateUpstreamUseCase internal constructor(
    private val service: CredentialStatusService,
    private val sessionStore: SessionStore,
    logger: Logger,
) {
    private val log = logger.withTag("CredentialStatus")

    suspend operator fun invoke(password: String, captchaToken: String? = null): Outcome<Unit, ReauthError> {
        val response = try {
            service.reauthenticate(ReauthRequest(password = password, captchaToken = captchaToken))
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            log.w(t) { "reauth transport failure" }
            return Outcome.Err(ReauthError.NoConnection)
        }

        return when {
            response.status.isSuccess() -> {
                sessionStore.setCredentialsInvalid(false)
                log.i { "reauth ok, sync resumes server-side" }
                Outcome.Ok(Unit)
            }
            // The server only answers 400 here when upstream actually rejected
            // the password; transport and outage cases come back as 503.
            response.status == HttpStatusCode.BadRequest -> Outcome.Err(ReauthError.InvalidPassword)
            response.status == HttpStatusCode.ServiceUnavailable -> Outcome.Err(ReauthError.Unavailable)
            else -> Outcome.Err(ReauthError.Server(response.status.description))
        }
    }
}

private fun String.toDomain(): CredentialStatus = when (lowercase()) {
    "invalid" -> CredentialStatus.Invalid
    "none" -> CredentialStatus.None
    else -> CredentialStatus.Ok
}
