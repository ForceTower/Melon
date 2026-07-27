package dev.forcetower.unes.update

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import timber.log.Timber

// Play In-App Updates decision table (docs/in-app-update.md). Priority is set
// per release via the Play Developer API only — Console-cut releases default
// to 0, so staleness is the everyday driver: routine updates wait a week,
// then download silently (FLEXIBLE); a release explicitly marked critical, or
// a month of ignored updates, interrupts with Play's fullscreen updater
// (IMMEDIATE). Pure function, same plain-JUnit testability as `parseDeepLink`.
internal fun chooseUpdateType(
    availability: Int,
    priority: Int,
    stalenessDays: Int?,
    immediateAllowed: Boolean,
    flexibleAllowed: Boolean,
    flexibleSuppressed: Boolean,
): Int? {
    if (availability != UpdateAvailability.UPDATE_AVAILABLE) return null
    val staleness = stalenessDays ?: 0
    return when {
        priority >= 4 && immediateAllowed -> AppUpdateType.IMMEDIATE
        staleness >= 30 && immediateAllowed -> AppUpdateType.IMMEDIATE
        priority >= 2 && flexibleAllowed && !flexibleSuppressed -> AppUpdateType.FLEXIBLE
        staleness >= 7 && flexibleAllowed && !flexibleSuppressed -> AppUpdateType.FLEXIBLE
        else -> null
    }
}

private val Context.updateDataStore by preferencesDataStore(name = "in_app_update")

// Runs the update check once per process and carries the flexible-flow state
// between `MainActivity` (which owns the activity-result launcher) and
// `ConnectedScreen` (which shows the restart banner) — same activity-agnostic
// singleton shape as `DeepLinkHandler`. Every failure path is silent: on
// devices without Play (sideloads, emulators, the `.debug` app id) the info
// fetch throws and the feature becomes a no-op.
@Singleton
internal class InAppUpdater @Inject constructor(
    private val appUpdateManager: AppUpdateManager,
    @ApplicationContext context: Context,
) {
    private val dataStore = context.updateDataStore

    private val _updateDownloaded = MutableStateFlow(false)

    // A flexible download finished — the app can restart into the new version.
    val updateDownloaded: StateFlow<Boolean> = _updateDownloaded

    private var checked = false
    private var startedImmediate = false

    suspend fun checkOnLaunch(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        // Once per process, not per activity: config changes and activity
        // recreation don't re-prompt, while a process-death restore (non-null
        // savedInstanceState) still gets its check — that restore path is what
        // the DOWNLOADED short-circuit below exists for.
        if (checked) return
        checked = true
        val info = try {
            appUpdateManager.requestAppUpdateInfo()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.d(e, "in-app update unavailable")
            return
        }
        // Bits already on disk (download finished while the process was dead)
        // — surface the restart banner, never re-run the consent sheet.
        if (info.installStatus() == InstallStatus.DOWNLOADED) {
            _updateDownloaded.value = true
            return
        }
        val type = chooseUpdateType(
            availability = info.updateAvailability(),
            priority = info.updatePriority(),
            stalenessDays = info.clientVersionStalenessDays(),
            immediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE),
            flexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE),
            flexibleSuppressed = flexibleSuppressed(),
        ) ?: return
        if (type == AppUpdateType.IMMEDIATE) {
            startedImmediate = true
        } else {
            registerDownloadListener()
        }
        val started = appUpdateManager.startUpdateFlowForResult(
            info,
            launcher,
            AppUpdateOptions.newBuilder(type).build(),
        )
        if (!started) Timber.d("in-app update flow could not start, type=%d", type)
    }

    // Re-enters a fullscreen update the user backgrounded so it can't stay
    // wedged half-applied. Gated on `startedImmediate` because Play reports
    // DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS for a background flexible
    // download too, and resuming that as IMMEDIATE would hijack a silent
    // download into a fullscreen blocker.
    suspend fun resumeIfStalled(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        if (!startedImmediate) return
        val info = try {
            appUpdateManager.requestAppUpdateInfo()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return
        }
        if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
            appUpdateManager.startUpdateFlowForResult(
                info,
                launcher,
                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
            )
        }
    }

    suspend fun onUpdateFlowResult(resultCode: Int) {
        when (resultCode) {
            Activity.RESULT_OK -> Unit
            Activity.RESULT_CANCELED -> recordDecline()
            ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> Timber.d("in-app update flow failed")
            else -> Timber.d("in-app update flow result=%d", resultCode)
        }
    }

    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    // Session-only: the banner returns on next cold start through the
    // DOWNLOADED short-circuit, which is the desired gentle re-ask.
    fun dismissUpdateBanner() {
        _updateDownloaded.value = false
    }

    private fun registerDownloadListener() {
        val listener = object : InstallStateUpdatedListener {
            override fun onStateUpdate(state: InstallState) {
                when (state.installStatus()) {
                    InstallStatus.DOWNLOADED -> {
                        _updateDownloaded.value = true
                        appUpdateManager.unregisterListener(this)
                    }
                    // Terminal states never surface UI.
                    InstallStatus.INSTALLED, InstallStatus.FAILED, InstallStatus.CANCELED ->
                        appUpdateManager.unregisterListener(this)
                    else -> Unit
                }
            }
        }
        appUpdateManager.registerListener(listener)
    }

    // Declining a flexible prompt mutes the FLEXIBLE rows for a week so a
    // daily user isn't re-asked every cold start. IMMEDIATE ignores this — a
    // critical release must not be muted by an earlier routine decline.
    private suspend fun flexibleSuppressed(): Boolean {
        val declinedAt = dataStore.data.first()[declinedAtKey] ?: return false
        return System.currentTimeMillis() - declinedAt < SUPPRESSION_WINDOW_MS
    }

    private suspend fun recordDecline() {
        dataStore.edit { preferences -> preferences[declinedAtKey] = System.currentTimeMillis() }
    }

    private companion object {
        val declinedAtKey = longPreferencesKey("flexible_declined_at_ms")
        const val SUPPRESSION_WINDOW_MS = 7L * 24 * 60 * 60 * 1000
    }
}
