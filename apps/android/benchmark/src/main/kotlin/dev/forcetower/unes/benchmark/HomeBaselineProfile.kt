package dev.forcetower.unes.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class HomeBaselineProfile {
    @get:Rule
    val profile = BaselineProfileRule()

    @Test
    fun startup() {
        profile.collect(packageName = TARGET_PACKAGE, includeInStartupProfile = true) {
            pressHome()
            startActivityAndWait()
            device.signInIfNeeded()
            device.waitForHome()
        }
    }

    @Test
    fun home() {
        profile.collect(packageName = TARGET_PACKAGE) {
            startActivityAndWait()
            device.signInIfNeeded()
            device.waitForHome()
            device.scrollHome()
        }
    }
}
