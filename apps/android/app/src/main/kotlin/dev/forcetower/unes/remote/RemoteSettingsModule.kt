package dev.forcetower.unes.remote

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.forcetower.lever.LeverClient
import dev.forcetower.lever.LeverConfiguration
import dev.forcetower.lever.LeverContext
import dev.forcetower.lever.logging.LeverLogLevel
import dev.forcetower.lever.logging.LeverLogSink
import dev.forcetower.unes.BuildConfig
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import timber.log.Timber

@Module
@InstallIn(SingletonComponent::class)
internal object RemoteSettingsModule {

    @Provides
    @Singleton
    fun provideRemoteSettings(composite: CompositeRemoteSettings): RemoteSettings = composite

    // Built here rather than through `Lever.configure`/`Lever.shared`: Hilt
    // injects `MelonApp`'s fields inside `super.onCreate()`, before any of our
    // own launch code runs, so a process-global that had to be configured first
    // would be read before it existed. The client loads its cache
    // synchronously in this constructor, which is what makes the first gate
    // read after startup correct rather than eventually correct.
    @Provides
    @Singleton
    fun provideLeverClient(@ApplicationContext context: Context): LeverClient =
        LeverClient(
            context,
            LeverConfiguration(
                baseUrl = BuildConfig.LEVER_BASE_URL,
                // A `pk_` key is an identifier, not a credential: it authorizes
                // reading one environment's resolved values, which every user of
                // the app can see anyway. Shipping it in the APK is the intent.
                clientKey = BuildConfig.LEVER_CLIENT_KEY,
                // Feeds the server's targeting rules — `platform` is filled in by
                // the SDK, so a parameter can be split android-vs-ios or rolled
                // out by version without a client release.
                context = LeverContext(appVersion = BuildConfig.VERSION_NAME),
                // Same rule the Firebase layer follows: no fetch cache while
                // developing, so a publish lands on the next launch.
                minimumFetchInterval = if (BuildConfig.DEBUG) Duration.ZERO else 12.hours,
                // Pins the cache file's identity to a name we own, so rotating
                // the client key still lands on a warm cache.
                cacheNamespace = "prod",
                logSink = TimberLeverLogSink,
            ),
        )
}

private object TimberLeverLogSink : LeverLogSink {
    override fun log(level: LeverLogLevel, message: String) {
        val priority = when (level) {
            LeverLogLevel.DEBUG -> android.util.Log.DEBUG
            LeverLogLevel.INFO -> android.util.Log.INFO
            LeverLogLevel.WARN -> android.util.Log.WARN
            LeverLogLevel.ERROR -> android.util.Log.ERROR
        }
        Timber.tag("Lever").log(priority, message)
    }
}
