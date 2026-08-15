package dev.forcetower.unes.remote

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge

// lever answers where it has an opinion; Firebase answers everywhere else.
//
// That ordering is the migration: a key moves off Remote Config the moment it
// is published in lever, one key at a time, with no client release — and
// deleting it from lever hands it straight back. Nothing needs both sides to
// agree, and nothing breaks while only one of them is populated.
@Singleton
internal class CompositeRemoteSettings @Inject constructor(
    private val lever: LeverRemoteSettings,
    private val firebase: FirebaseRemoteSettings,
) : RemoteSettings {
    override fun bool(key: RemoteBoolKey): Boolean = lever.lookup(key) ?: firebase.bool(key)

    override fun string(key: RemoteStringKey): String = lever.lookup(key) ?: firebase.string(key)

    // Either layer changing can change what a read resolves to, so both are
    // reasons to recompute.
    override val changes: Flow<Unit> = merge(lever.changes, firebase.changes)

    // lever fetches from the moment its client is constructed; only Firebase
    // has a launch step.
    override fun start() = firebase.start()
}
