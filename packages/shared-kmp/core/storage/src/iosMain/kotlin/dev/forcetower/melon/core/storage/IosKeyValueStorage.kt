package dev.forcetower.melon.core.storage

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import platform.Foundation.NSUserDefaults

// TODO(security): swap NSUserDefaults for Keychain before shipping. Tokens are secrets and must not
// live in plist-backed preferences. The interface is designed so the swap is drop-in.
internal class IosKeyValueStorage(suiteName: String) : KeyValueStorage {

    private val defaults = NSUserDefaults(suiteName = suiteName)

    override suspend fun get(key: String): String? = defaults.stringForKey(key)

    override suspend fun put(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    override suspend fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }
}

@ContributesTo(AppScope::class)
interface IosStorageGraph {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun keyValueStorage(): KeyValueStorage =
            IosKeyValueStorage(suiteName = "dev.forcetower.melon")
    }
}
