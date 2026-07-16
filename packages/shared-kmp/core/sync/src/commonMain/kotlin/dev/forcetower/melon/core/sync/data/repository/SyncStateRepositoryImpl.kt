package dev.forcetower.melon.core.sync.data.repository

import dev.forcetower.melon.core.database.dao.SyncStateDao
import dev.forcetower.melon.core.database.entity.SyncStateEntity
import dev.forcetower.melon.core.sync.domain.repository.SyncStateRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

internal const val KEY_ONBOARDING_COMPLETE = "sync.onboarding_complete"
internal const val KEY_BACKFILL_MIRROR_COMPLETE = "sync.backfill_mirror_complete"

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
internal class SyncStateRepositoryImpl(private val dao: SyncStateDao) : SyncStateRepository {

    override suspend fun getOnboardingComplete(): Boolean =
        dao.get(KEY_ONBOARDING_COMPLETE) == "true"

    override suspend fun setOnboardingComplete(value: Boolean) {
        dao.put(SyncStateEntity(key = KEY_ONBOARDING_COMPLETE, value = value.toString()))
    }

    override suspend fun getBackfillMirrorComplete(): Boolean =
        dao.get(KEY_BACKFILL_MIRROR_COMPLETE) == "true"

    override suspend fun setBackfillMirrorComplete(value: Boolean) {
        dao.put(SyncStateEntity(key = KEY_BACKFILL_MIRROR_COMPLETE, value = value.toString()))
    }

    override suspend fun reset() {
        dao.clear()
    }
}
