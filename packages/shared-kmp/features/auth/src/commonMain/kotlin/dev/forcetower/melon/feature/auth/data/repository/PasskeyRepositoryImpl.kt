package dev.forcetower.melon.feature.auth.data.repository

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.network.ApiEnvelope
import dev.forcetower.melon.feature.auth.data.dto.PasskeyCredentialsResponse
import dev.forcetower.melon.feature.auth.data.dto.PasskeyRenameRequest
import dev.forcetower.melon.feature.auth.data.mapper.toDomain
import dev.forcetower.melon.feature.auth.data.mapper.toVerifyRequest
import dev.forcetower.melon.feature.auth.data.network.PasskeyService
import dev.forcetower.melon.feature.auth.domain.model.PasskeyAttestation
import dev.forcetower.melon.feature.auth.domain.model.PasskeyCredential
import dev.forcetower.melon.feature.auth.domain.model.PasskeyError
import dev.forcetower.melon.feature.auth.domain.model.PasskeyRegistrationOptions
import dev.forcetower.melon.feature.auth.domain.repository.PasskeyRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
internal class PasskeyRepositoryImpl(
    private val api: PasskeyService,
    logger: Logger,
) : PasskeyRepository {

    private val log = logger.withTag("PasskeyRepositoryImpl")

    override suspend fun registrationOptions(): Outcome<PasskeyRegistrationOptions, PasskeyError> = call("registrationOptions") {
        val response = api.registerOptions()
        when (val status = response.status.value) {
            in 200..299 -> {
                // Pass the server's creation-options object through verbatim;
                // CredentialManager consumes exactly this JSON shape.
                val envelope = response.body<ApiEnvelope<JsonObject>>()
                val data = envelope.data ?: run {
                    log.w { "registrationOptions 2xx with null data" }
                    return@call Outcome.Err(PasskeyError.Unexpected)
                }
                Outcome.Ok(PasskeyRegistrationOptions(requestJson = data.toString()))
            }
            else -> statusError("registrationOptions", status, response)
        }
    }

    override suspend fun register(
        attestation: PasskeyAttestation,
        deviceName: String?,
    ): Outcome<Unit, PasskeyError> = call("register") {
        val response = api.registerVerify(attestation.toVerifyRequest(deviceName))
        when (val status = response.status.value) {
            in 200..299 -> Outcome.Ok(Unit)
            else -> statusError("register", status, response)
        }
    }

    override suspend fun list(): Outcome<List<PasskeyCredential>, PasskeyError> = call("list") {
        val response = api.credentials()
        when (val status = response.status.value) {
            in 200..299 -> {
                val envelope = response.body<ApiEnvelope<PasskeyCredentialsResponse>>()
                Outcome.Ok(envelope.data?.credentials.orEmpty().map { it.toDomain() })
            }
            else -> statusError("list", status, response)
        }
    }

    override suspend fun rename(id: String, deviceName: String): Outcome<Unit, PasskeyError> = call("rename") {
        val response = api.rename(id, PasskeyRenameRequest(deviceName))
        when (val status = response.status.value) {
            in 200..299 -> Outcome.Ok(Unit)
            404 -> Outcome.Err(PasskeyError.NotFound)
            else -> statusError("rename", status, response)
        }
    }

    override suspend fun delete(id: String): Outcome<Unit, PasskeyError> = call("delete") {
        val response = api.delete(id)
        when (val status = response.status.value) {
            in 200..299 -> Outcome.Ok(Unit)
            404 -> Outcome.Err(PasskeyError.NotFound)
            else -> statusError("delete", status, response)
        }
    }

    // Shared transport-failure envelope. Cancellation propagates; a bad body is
    // Unexpected; anything else on the wire is NoConnection.
    private inline fun <T> call(
        op: String,
        block: () -> Outcome<T, PasskeyError>,
    ): Outcome<T, PasskeyError> = try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (ex: SerializationException) {
        log.e(throwable = ex) { "$op failed: deserialization" }
        Outcome.Err(PasskeyError.Unexpected)
    } catch (ex: Throwable) {
        log.w(throwable = ex) { "$op failed: transport" }
        Outcome.Err(PasskeyError.NoConnection)
    }

    private suspend fun statusError(
        op: String,
        status: Int,
        response: HttpResponse,
    ): Outcome<Nothing, PasskeyError> = when (status) {
        in 500..599 -> {
            val envelope = runCatching { response.body<ApiEnvelope<JsonObject>>() }.getOrNull()
            log.w { "$op server $status message=${envelope?.message ?: "<none>"}" }
            Outcome.Err(PasskeyError.Server(envelope?.message))
        }
        else -> {
            log.w { "$op unexpected status $status" }
            Outcome.Err(PasskeyError.Unexpected)
        }
    }
}
