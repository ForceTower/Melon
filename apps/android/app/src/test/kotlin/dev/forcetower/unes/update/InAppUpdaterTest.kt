package dev.forcetower.unes.update

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

// Plain mocks instead of Play's FakeAppUpdateManager (needs Robolectric, see
// docs/in-app-update.md). The suppression DataStore is a process-wide
// singleton, so the decline is recorded last, in the same test.
internal class InAppUpdaterTest {
    private val context = mockk<Context>().also { context ->
        every { context.applicationContext } returns context
        every { context.filesDir } returns createTempDirectory("in-app-update").toFile()
    }
    private val launcher = mockk<ActivityResultLauncher<IntentSenderRequest>>()

    @Test
    fun aFlexibleFlowThatFailsToStartOrIsDeclinedReleasesItsListener() =
        runTest {
            val unstartable = appUpdateManager(flowStarts = false)
            InAppUpdater(unstartable, context).checkOnLaunch(launcher)
            unstartable.assertListenerReleased()

            val declined = appUpdateManager(flowStarts = true)
            val updater = InAppUpdater(declined, context)
            updater.checkOnLaunch(launcher)
            updater.onUpdateFlowResult(Activity.RESULT_CANCELED)
            declined.assertListenerReleased()
        }

    private fun AppUpdateManager.assertListenerReleased() {
        val listener = slot<InstallStateUpdatedListener>()
        verify(exactly = 1) { registerListener(capture(listener)) }
        verify(exactly = 1) { unregisterListener(listener.captured) }
    }

    private fun appUpdateManager(flowStarts: Boolean): AppUpdateManager =
        mockk(relaxUnitFun = true) {
            every { appUpdateInfo } returns completed(flexibleUpdate())
            every {
                startUpdateFlowForResult(any(), any<ActivityResultLauncher<IntentSenderRequest>>(), any())
            } returns flowStarts
        }

    private fun flexibleUpdate(): AppUpdateInfo =
        mockk {
            every { installStatus() } returns InstallStatus.UNKNOWN
            every { updateAvailability() } returns UpdateAvailability.UPDATE_AVAILABLE
            every { updatePriority() } returns 2
            every { clientVersionStalenessDays() } returns null
            every { isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) } returns false
            every { isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) } returns true
        }

    private fun <T> completed(value: T): Task<T> =
        mockk {
            every { isComplete } returns true
            every { isSuccessful } returns true
            every { result } returns value
        }
}
