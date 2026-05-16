package dev.forcetower.melon.core.storage

interface KeyValueStorage {
    suspend fun get(key: String): String?
    suspend fun put(key: String, value: String)
    suspend fun remove(key: String)
}
