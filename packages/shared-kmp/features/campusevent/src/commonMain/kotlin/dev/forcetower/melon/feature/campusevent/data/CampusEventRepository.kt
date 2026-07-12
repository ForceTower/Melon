package dev.forcetower.melon.feature.campusevent.data

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.storage.KeyValueStorage
import dev.forcetower.melon.feature.campusevent.data.network.CampusEventDTO
import dev.forcetower.melon.feature.campusevent.data.network.CampusEventService
import dev.forcetower.melon.feature.campusevent.data.network.toDomainOrNull
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEvent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

// Single-snapshot mirror of the featured campus event, the Android analogue
// of iOS `MirrorStore+CampusEvent` (which uses a one-row GRDB table): the
// last payload persists as JSON in `KeyValueStorage` so the Home card renders
// offline, refreshes land through `observe`, and identical `(id, revision)`
// publishes write nothing so observers only wake for real changes.
@SingleIn(AppScope::class)
@Inject
class CampusEventRepository internal constructor(
    private val service: CampusEventService,
    private val storage: KeyValueStorage,
    private val json: Json,
) {
    private val state = MutableStateFlow<CampusEvent?>(null)
    private val hydration = Mutex()
    private var hydrated = false

    fun observe(): Flow<CampusEvent?> = flow {
        hydrate()
        emitAll(state)
    }

    // Failures never surface — the stale offline payload is kept, exactly
    // like the iOS repository's silent refresh.
    suspend fun refresh() {
        hydrate()
        when (val outcome = service.current()) {
            is Outcome.Ok -> apply(outcome.value)
            is Outcome.Err -> Unit
        }
    }

    // Logout wipe — the featured event is user/course-scoped, so it must not
    // leak into the next account on the device. iOS gets this through the
    // GRDB mirror wipe; here the key-value snapshot is cleared explicitly.
    suspend fun clear() {
        hydration.withLock { hydrated = true }
        state.value = null
        storage.remove(STORAGE_KEY)
    }

    private suspend fun hydrate() {
        hydration.withLock {
            if (hydrated) return
            hydrated = true
            val cached = storage.get(STORAGE_KEY) ?: return
            state.value = runCatching { json.decodeFromString<CampusEventDTO>(cached) }
                .getOrNull()
                ?.toDomainOrNull()
        }
    }

    private suspend fun apply(dto: CampusEventDTO?) {
        if (dto == null) {
            // The server un-featured the event.
            state.value = null
            storage.remove(STORAGE_KEY)
            return
        }
        val event = dto.toDomainOrNull() ?: return
        val current = state.value
        if (current != null && current.id == event.id && current.revision == event.revision) return
        state.value = event
        storage.put(STORAGE_KEY, json.encodeToString(dto))
    }

    private companion object {
        const val STORAGE_KEY = "campus_event.current"
    }
}
