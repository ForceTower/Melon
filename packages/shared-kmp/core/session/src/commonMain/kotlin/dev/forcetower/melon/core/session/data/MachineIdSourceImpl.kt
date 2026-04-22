package dev.forcetower.melon.core.session.data

import dev.forcetower.melon.core.network.MachineIdSource
import dev.forcetower.melon.core.storage.KeyValueStorage
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal const val MACHINE_ID_KEY = "melon.machine_id"

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
internal class MachineIdSourceImpl(
    private val storage: KeyValueStorage,
) : MachineIdSource {

    private val mutex = Mutex()

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun getMachineId(): String = mutex.withLock {
        storage.get(MACHINE_ID_KEY) ?: Uuid.random().toString().replace("-", "").also { storage.put(MACHINE_ID_KEY, it) }
    }
}
