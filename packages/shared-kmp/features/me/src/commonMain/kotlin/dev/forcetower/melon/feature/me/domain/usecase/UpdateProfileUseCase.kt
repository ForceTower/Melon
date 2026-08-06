package dev.forcetower.melon.feature.me.domain.usecase

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.me.data.network.ProfileService
import dev.forcetower.melon.feature.me.domain.model.ProfileUpdateError
import dev.zacsweers.metro.Inject
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

// Pushes the profile customizations (display name / picture) to the API. No
// optimistic local write: the caller runs `SyncProfileUseCase` after a
// successful save, and the refreshed User row flows back through
// `ObserveMeProfileUseCase` into every screen that renders the name.
@Inject
class UpdateProfileUseCase internal constructor(
    private val service: ProfileService,
    logger: Logger,
) {
    private val log = logger.withTag("UpdateProfileUseCase")

    // Null or blank clears the alternate name — the portal name takes over.
    suspend fun updateName(name: String?): Outcome<Unit, ProfileUpdateError> =
        call("updateName") { service.updateName(name?.trim()?.takeIf { it.isNotEmpty() }) }

    // `mimeType` must be the actual encoding of `bytes` — the API accepts
    // jpeg/png/webp up to 5 MB and verifies magic numbers against the type.
    suspend fun updatePicture(bytes: ByteArray, mimeType: String): Outcome<Unit, ProfileUpdateError> =
        call("updatePicture") { service.uploadPicture(bytes, mimeType) }

    suspend fun removePicture(): Outcome<Unit, ProfileUpdateError> =
        call("removePicture") { service.deletePicture() }

    private suspend fun call(
        label: String,
        request: suspend () -> HttpResponse,
    ): Outcome<Unit, ProfileUpdateError> {
        val response = try {
            request()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (ex: Throwable) {
            log.w(throwable = ex) { "$label transport failure" }
            return Outcome.Err(ProfileUpdateError.Connection)
        }
        return when {
            response.status.isSuccess() -> {
                log.i { "$label ok" }
                Outcome.Ok(Unit)
            }
            response.status == HttpStatusCode.BadRequest -> {
                log.w { "$label rejected by the API" }
                Outcome.Err(ProfileUpdateError.Rejected)
            }
            else -> {
                log.w { "$label unexpected status ${response.status.value}" }
                Outcome.Err(ProfileUpdateError.Unexpected)
            }
        }
    }
}
