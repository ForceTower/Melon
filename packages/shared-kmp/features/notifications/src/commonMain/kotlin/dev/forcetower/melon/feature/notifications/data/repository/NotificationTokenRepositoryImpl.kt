package dev.forcetower.melon.feature.notifications.data.repository

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.network.ApiEnvelope
import dev.forcetower.melon.feature.notifications.data.dto.RegisterNotificationTokenRequest
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
) : NotificationTokenRepository {

    override suspend fun register(
        request: RegisterNotificationTokenRequest,
    ): Outcome<Unit, NotificationTokenError> = try {
        classifyResponse(api.registerToken(request))
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: SerializationException) {
        Outcome.Err(NotificationTokenError.Kind.Unexpected)
    } catch (_: Throwable) {
        Outcome.Err(NotificationTokenError.Kind.NoConnection)
    }

    private suspend fun classifyResponse(response: HttpResponse): Outcome<Unit, NotificationTokenError> {
        val status = response.status.value
        return when (status) {
            in 200..299 -> Outcome.Ok(Unit)
            401 -> Outcome.Err(NotificationTokenError.Kind.Unauthorized)
            in 500..599 -> {
                val envelope = runCatching { response.body<ApiEnvelope<Unit>>() }.getOrNull()
                Outcome.Err(NotificationTokenError.Server(envelope?.message))
            }
            else -> Outcome.Err(NotificationTokenError.Kind.Unexpected)
        }
    }
}
