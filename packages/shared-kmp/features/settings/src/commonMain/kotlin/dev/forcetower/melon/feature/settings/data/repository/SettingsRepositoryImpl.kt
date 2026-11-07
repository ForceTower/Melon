package dev.forcetower.melon.feature.settings.data.repository

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.database.dao.UserSettingsDao
import dev.forcetower.melon.core.database.entity.UserSettingsEntity
import dev.forcetower.melon.core.network.ApiEnvelope
import dev.forcetower.melon.feature.settings.data.dto.UpdateUserSettingsRequest
import dev.forcetower.melon.feature.settings.data.dto.UpdateUserSettingsResponse
import dev.forcetower.melon.feature.settings.data.dto.UserSettingsDto
import dev.forcetower.melon.feature.settings.data.network.SettingsApi
import dev.forcetower.melon.feature.settings.domain.model.UserSettings
import dev.forcetower.melon.feature.settings.domain.model.UserSettingsPatch
import dev.forcetower.melon.feature.settings.domain.repository.SettingsError
import dev.forcetower.melon.feature.settings.domain.repository.SettingsRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
internal class SettingsRepositoryImpl(
    private val dao: UserSettingsDao,
    private val api: SettingsApi,
    logger: Logger,
) : SettingsRepository {

    private val log = logger.withTag("SettingsRepositoryImpl")

    override fun observe(): Flow<UserSettings?> = dao.observeCurrent().map { it?.toDomain() }

    override suspend fun update(patch: UserSettingsPatch): Outcome<UserSettings, SettingsError> {
        val current = dao.getCurrent()
            ?: run {
                log.w { "update with no local UserSettings row" }
                return Outcome.Err(SettingsError.NoLocalUser)
            }

        // Optimistic local apply so the UI flips immediately. Rolled back
        // unless the server confirms it — the next profile sync would
        // otherwise silently restore the server value.
        dao.applyPatch(current.userId, patch)

        val outcome = try {
            val response = api.update(patch.toRequest())
            classifyUpdate(response, current.userId)
        } catch (cancellation: CancellationException) {
            withContext(NonCancellable) { rollBack(patch, current) }
            throw cancellation
        } catch (ex: SerializationException) {
            log.e(throwable = ex) { "update serialization failure" }
            Outcome.Err(SettingsError.Unexpected)
        } catch (ex: Throwable) {
            log.w(throwable = ex) { "update transport failure" }
            Outcome.Err(SettingsError.NoConnection)
        }
        if (outcome is Outcome.Err) rollBack(patch, current)
        return outcome
    }

    // Puts back only the fields this patch wrote that still hold its value,
    // so a later update that landed meanwhile is kept.
    private suspend fun rollBack(
        patch: UserSettingsPatch,
        previous: UserSettingsEntity,
    ) {
        val row = dao.getCurrent() ?: return
        dao.applyPatch(
            previous.userId,
            UserSettingsPatch(
                gradeSpoiler = restore(previous.gradeSpoiler, row.gradeSpoiler, patch.gradeSpoiler),
                notifMsgBroadcast = restore(previous.notifMsgBroadcast, row.notifMsgBroadcast, patch.notifMsgBroadcast),
                notifMsgClass = restore(previous.notifMsgClass, row.notifMsgClass, patch.notifMsgClass),
                notifMsgDirect = restore(previous.notifMsgDirect, row.notifMsgDirect, patch.notifMsgDirect),
                notifGradePosted = restore(previous.notifGradePosted, row.notifGradePosted, patch.notifGradePosted),
                notifGradeChanged = restore(previous.notifGradeChanged, row.notifGradeChanged, patch.notifGradeChanged),
                notifGradeDateChanged = restore(
                    previous.notifGradeDateChanged,
                    row.notifGradeDateChanged,
                    patch.notifGradeDateChanged,
                ),
                notifClassLocation = restore(
                    previous.notifClassLocation,
                    row.notifClassLocation,
                    patch.notifClassLocation,
                ),
                notifClassMaterial = restore(
                    previous.notifClassMaterial,
                    row.notifClassMaterial,
                    patch.notifClassMaterial,
                ),
                notifClassSubject = restore(previous.notifClassSubject, row.notifClassSubject, patch.notifClassSubject),
            ),
        )
    }

    // On 2xx the server returns the canonical merged row — overwrite the
    // local row from that so server-side coercions (e.g. spoiler clamp)
    // immediately reflect on-device.
    private suspend fun classifyUpdate(
        response: HttpResponse,
        userId: String,
    ): Outcome<UserSettings, SettingsError> {
        val statusCode = response.status.value
        return when (statusCode) {
            in 200..299 -> {
                val envelope = response.body<ApiEnvelope<UpdateUserSettingsResponse>>()
                val payload = envelope.data
                if (payload != null) {
                    val entity = payload.settings.toEntity(userId)
                    dao.upsert(entity)
                    log.i { "update ok userId=$userId" }
                    Outcome.Ok(entity.toDomain())
                } else {
                    log.w { "update 2xx envelope had null data (message=${envelope.message})" }
                    Outcome.Err(SettingsError.Unexpected)
                }
            }
            401 -> {
                log.w { "update unauthorized" }
                Outcome.Err(SettingsError.Unauthorized)
            }
            in 500..599 -> {
                val envelope = runCatching {
                    response.body<ApiEnvelope<UpdateUserSettingsResponse>>()
                }.getOrNull()
                log.w { "update server $statusCode message=${envelope?.message ?: "<none>"}" }
                Outcome.Err(SettingsError.Server(envelope?.message))
            }
            else -> {
                log.w { "update unexpected status $statusCode" }
                Outcome.Err(SettingsError.Unexpected)
            }
        }
    }
}

private fun <T> restore(
    before: T,
    now: T,
    written: T?,
): T? = before.takeIf { now == written }

private suspend fun UserSettingsDao.applyPatch(
    userId: String,
    changes: UserSettingsPatch,
) = patch(
    userId = userId,
    gradeSpoiler = changes.gradeSpoiler,
    notifMsgBroadcast = changes.notifMsgBroadcast,
    notifMsgClass = changes.notifMsgClass,
    notifMsgDirect = changes.notifMsgDirect,
    notifGradePosted = changes.notifGradePosted,
    notifGradeChanged = changes.notifGradeChanged,
    notifGradeDateChanged = changes.notifGradeDateChanged,
    notifClassLocation = changes.notifClassLocation,
    notifClassMaterial = changes.notifClassMaterial,
    notifClassSubject = changes.notifClassSubject,
)

private fun UserSettingsPatch.toRequest(): UpdateUserSettingsRequest =
    UpdateUserSettingsRequest(
        gradeSpoiler = gradeSpoiler,
        notifMsgBroadcast = notifMsgBroadcast,
        notifMsgClass = notifMsgClass,
        notifMsgDirect = notifMsgDirect,
        notifGradePosted = notifGradePosted,
        notifGradeChanged = notifGradeChanged,
        notifGradeDateChanged = notifGradeDateChanged,
        notifClassLocation = notifClassLocation,
        notifClassMaterial = notifClassMaterial,
        notifClassSubject = notifClassSubject,
    )

internal fun UserSettingsDto.toEntity(userId: String): UserSettingsEntity =
    UserSettingsEntity(
        userId = userId,
        gradeSpoiler = gradeSpoiler,
        notifMsgBroadcast = notifMsgBroadcast,
        notifMsgClass = notifMsgClass,
        notifMsgDirect = notifMsgDirect,
        notifGradePosted = notifGradePosted,
        notifGradeChanged = notifGradeChanged,
        notifGradeDateChanged = notifGradeDateChanged,
        notifClassLocation = notifClassLocation,
        notifClassMaterial = notifClassMaterial,
        notifClassSubject = notifClassSubject,
    )

internal fun UserSettingsEntity.toDomain(): UserSettings =
    UserSettings(
        gradeSpoiler = gradeSpoiler,
        notifMsgBroadcast = notifMsgBroadcast,
        notifMsgClass = notifMsgClass,
        notifMsgDirect = notifMsgDirect,
        notifGradePosted = notifGradePosted,
        notifGradeChanged = notifGradeChanged,
        notifGradeDateChanged = notifGradeDateChanged,
        notifClassLocation = notifClassLocation,
        notifClassMaterial = notifClassMaterial,
        notifClassSubject = notifClassSubject,
    )
