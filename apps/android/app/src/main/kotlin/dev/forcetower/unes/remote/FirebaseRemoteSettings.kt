package dev.forcetower.unes.remote

import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfigSettings
import dev.forcetower.unes.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import timber.log.Timber

// The base layer, and the one being migrated away from: Remote Config is
// becoming a paid feature, so lever sits in front of it (see
// `CompositeRemoteSettings`) and this stays as the answer for every key lever
// has not taken over yet.
//
// `getBoolean`/`getString` are total — an unpublished key reads as `false`/`""`
// — which is what makes this a valid bottom of the stack. Between launches
// Remote Config's own disk cache keeps the last activated values, so gates hold
// their state offline.
@Singleton
internal class FirebaseRemoteSettings @Inject constructor() : RemoteSettings {
    private val config = FirebaseRemoteConfig.getInstance()

    // `replay = 1` so the launch fetch cannot land before `FeatureFlags`
    // subscribes and be lost — the gate would then stay stale for the whole
    // session, since Remote Config has nothing more to say until it changes.
    private val changed =
        MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val changes: Flow<Unit> = changed

    override fun bool(key: RemoteBoolKey): Boolean = config.getBoolean(key.key)

    override fun string(key: RemoteStringKey): String = config.getString(key.key)

    override fun start() {
        if (BuildConfig.DEBUG) {
            // Same as iOS DEBUG: skip the 12h fetch cache so console changes
            // land on next launch while developing.
            config.setConfigSettingsAsync(
                remoteConfigSettings { minimumFetchIntervalInSeconds = 0 },
            )
        }
        config.fetchAndActivate().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Timber.w(task.exception, "remote config fetchAndActivate failed")
            }
            changed.tryEmit(Unit)
        }
        config.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                config.activate().addOnCompleteListener { changed.tryEmit(Unit) }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Timber.w(error, "remote config realtime update failed")
            }
        })
    }
}
