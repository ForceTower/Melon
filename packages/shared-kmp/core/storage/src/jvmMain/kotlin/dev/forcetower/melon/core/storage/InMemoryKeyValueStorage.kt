package dev.forcetower.melon.core.storage

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// JVM target exists only for unit tests today. When Android ships, add an EncryptedSharedPreferences-backed
// impl in androidMain and move this in-memory variant under jvmTest fixtures.
internal class InMemoryKeyValueStorage : KeyValueStorage {

    private val lock = Mutex()
    private val values = mutableMapOf<String, String>()

    override suspend fun get(key: String): String? = lock.withLock { values[key] }

    override suspend fun put(key: String, value: String) {
        lock.withLock { values[key] = value }
    }

    override suspend fun remove(key: String) {
        lock.withLock { values.remove(key) }
    }
}

@ContributesTo(AppScope::class)
interface JvmStorageGraph {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun keyValueStorage(): KeyValueStorage = InMemoryKeyValueStorage()
    }
}
