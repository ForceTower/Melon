package dev.forcetower.unes.update

import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdateStrategyTest {

    @Test
    fun `nothing happens without an available update`() {
        assertNull(choose(availability = UpdateAvailability.UPDATE_NOT_AVAILABLE, priority = 5))
        assertNull(choose(availability = UpdateAvailability.UNKNOWN, stalenessDays = 90))
        assertNull(
            choose(
                availability = UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
                priority = 5,
            ),
        )
    }

    @Test
    fun `critical priority goes immediate`() {
        assertEquals(AppUpdateType.IMMEDIATE, choose(priority = 4))
        assertEquals(AppUpdateType.IMMEDIATE, choose(priority = 5))
    }

    @Test
    fun `a month of staleness goes immediate`() {
        assertEquals(AppUpdateType.IMMEDIATE, choose(stalenessDays = 30))
        assertEquals(AppUpdateType.IMMEDIATE, choose(stalenessDays = 45))
    }

    @Test
    fun `elevated priority goes flexible`() {
        assertEquals(AppUpdateType.FLEXIBLE, choose(priority = 2))
        assertEquals(AppUpdateType.FLEXIBLE, choose(priority = 3))
    }

    @Test
    fun `a week of staleness goes flexible`() {
        assertEquals(AppUpdateType.FLEXIBLE, choose(stalenessDays = 7))
        assertEquals(AppUpdateType.FLEXIBLE, choose(stalenessDays = 29))
    }

    @Test
    fun `fresh or unknown staleness at routine priority does nothing`() {
        assertNull(choose(priority = 0, stalenessDays = null))
        assertNull(choose(priority = 1, stalenessDays = 6))
    }

    @Test
    fun `critical priority falls back to flexible when immediate is not allowed`() {
        assertEquals(AppUpdateType.FLEXIBLE, choose(priority = 5, immediateAllowed = false))
    }

    @Test
    fun `nothing starts when play allows neither flow`() {
        assertNull(choose(priority = 5, immediateAllowed = false, flexibleAllowed = false))
        assertNull(choose(stalenessDays = 10, flexibleAllowed = false))
    }

    @Test
    fun `suppression mutes flexible but never immediate`() {
        assertNull(choose(priority = 2, flexibleSuppressed = true))
        assertNull(choose(stalenessDays = 10, flexibleSuppressed = true))
        assertEquals(AppUpdateType.IMMEDIATE, choose(priority = 4, flexibleSuppressed = true))
        assertEquals(AppUpdateType.IMMEDIATE, choose(stalenessDays = 30, flexibleSuppressed = true))
    }

    private fun choose(
        availability: Int = UpdateAvailability.UPDATE_AVAILABLE,
        priority: Int = 0,
        stalenessDays: Int? = null,
        immediateAllowed: Boolean = true,
        flexibleAllowed: Boolean = true,
        flexibleSuppressed: Boolean = false,
    ): Int? = chooseUpdateType(
        availability = availability,
        priority = priority,
        stalenessDays = stalenessDays,
        immediateAllowed = immediateAllowed,
        flexibleAllowed = flexibleAllowed,
        flexibleSuppressed = flexibleSuppressed,
    )
}
