package dev.forcetower.melon.feature.notifications.data.repository

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.network.ApiEnvelope
import dev.forcetower.melon.feature.notifications.data.dto.RegisterNotificationTokenRequest
import dev.forcetower.melon.feature.notifications.data.dto.UnregisterNotificationTokenRequest
import dev.forcetower.melon.feature.notifications.data.network.NotificationTokenService
import dev.forcetower.melon.feature.notifications.domain.model.NotificationTokenError
import dev.forcetower.melon.feature.notifications.domain.repository.NotificationTokenRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
internal class NotificationTokenRepositoryImpl(
    private val api: NotificationTokenService,
    logger: Logger,
) : NotificationTokenRepository {

    private val log = logger.withTag("NotificationTokenRepositoryImpl")

    override suspend fun register(
        request: RegisterNotificationTokenRequest,
    ): Outcome<Unit, NotificationTokenError> = call("register") {
        log.i { "register start platform=${request.platform} type=${request.identifierType}" }
        api.registerToken(request)
    }

    override suspend fun unregister(
        request: UnregisterNotificationTokenRequest,
    ): Outcome<Unit, NotificationTokenError> = call("unregister") {
        log.i { "unregister start" }
        api.unregisterToken(request)
    }

    private suspend fun call(
        op: String,
        request: suspend () -> HttpResponse,
    ): Outcome<Unit, NotificationTokenError> = try {
        classifyResponse(op, request())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (ex: SerializationException) {
        log.e(throwable = ex) { "$op failed: envelope deserialization" }
        Outcome.Err(NotificationTokenError.Kind.Unexpected)
    } catch (ex: Throwable) {
        log.w(throwable = ex) { "$op failed: transport" }
        Outcome.Err(NotificationTokenError.Kind.NoConnection)
    }

    private suspend fun classifyResponse(op: String, response: HttpResponse): Outcome<Unit, NotificationTokenError> {
        val status = response.status.value
        return when (status) {
            in 200..299 -> {
                log.i { "$op ok" }
                Outcome.Ok(Unit)
            }
            401 -> {
                log.w { "$op unauthorized" }
                Outcome.Err(NotificationTokenError.Kind.Unauthorized)
            }
            in 500..599 -> {
                val envelope = runCatching { response.body<ApiEnvelope<Unit>>() }.getOrNull()
                log.w { "$op server $status message=${envelope?.message ?: "<none>"}" }
                Outcome.Err(NotificationTokenError.Server(envelope?.message))
            }
            else -> {
                log.w { "$op unexpected status $status" }
                Outcome.Err(NotificationTokenError.Kind.Unexpected)
            }
        }
    }
}
