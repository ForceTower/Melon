package dev.forcetower.unes.remote

import dev.forcetower.lever.LeverClient
import dev.forcetower.lever.LeverKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// The top layer: our own remote config (https://github.com/ForceTower/lever).
//
// Deliberately not a `RemoteSettings` — this source is allowed to have no
// opinion. `lookup` returns null when lever has not published a key (or
// published something the key cannot read), which is what lets the composite
// fall through to Firebase instead of committing to a default. A key lever
// *has* published wins outright, `false` included.
//
// The defaults handed to `LeverKey` are never served: `lookup` reports absence
// rather than substituting them. They exist because the SDK's key type carries
// one.
@Singleton
internal class LeverRemoteSettings @Inject constructor(private val client: LeverClient) {
    private val boolKeys = RemoteBoolKey.entries.associateWith { LeverKey.boolean(it.key, false) }
    private val stringKeys = RemoteStringKey.entries.associateWith { LeverKey.string(it.key, "") }

    // Emits on every value-changing activation — the launch fetch, the polling
    // floor, and the SSE nudge that lands a console publish while the app is
    // open, all through one channel.
    val changes: Flow<Unit> = client.updates.map { }

    fun lookup(key: RemoteBoolKey): Boolean? = client.lookup(boolKeys.getValue(key))

    fun lookup(key: RemoteStringKey): String? = client.lookup(stringKeys.getValue(key))
}
