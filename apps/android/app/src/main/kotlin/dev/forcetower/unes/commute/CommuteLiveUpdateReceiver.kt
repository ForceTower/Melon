package dev.forcetower.unes.commute

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.forcetower.unes.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// The followed-bus notification's own broadcasts: "Parar" (or swiping it
// away, where the system allows it) and the stale alarm.
internal class CommuteLiveUpdateReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun liveUpdates(): CommuteLiveUpdates

        @ApplicationScope
        fun applicationScope(): CoroutineScope
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val activityId = intent.getStringExtra(EXTRA_ACTIVITY_ID) ?: return
        val dependencies = EntryPointAccessors.fromApplication(context, Dependencies::class.java)
        val pending = goAsync()
        dependencies.applicationScope().launch {
            try {
                when (intent.action) {
                    ACTION_STOP -> dependencies.liveUpdates().stop(activityId)
                    ACTION_STALE -> dependencies.liveUpdates().onStale(activityId)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_STOP = "dev.forcetower.unes.commute.ACTION_STOP"
        const val ACTION_STALE = "dev.forcetower.unes.commute.ACTION_STALE"
        const val EXTRA_ACTIVITY_ID = "activityId"
    }
}
