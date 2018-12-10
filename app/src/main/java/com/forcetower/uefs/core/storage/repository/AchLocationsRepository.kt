package com.forcetower.uefs.core.storage.repository

import android.location.Location
import com.forcetower.uefs.R
import javax.inject.Inject

class AchLocationsRepository @Inject constructor() {

    fun onReceiveLocation(location: Location): Int? {
        return matchesBigTray(location) ?:
                matchesLibrary(location) ?:
                matchesZoologyMuseum(location) ?:
                matchesHogwarts(location)

    }

    private fun matchesBigTray(location: Location): Int? {
        val place = Location("").apply {
            latitude = -12.201868
            longitude = -38.96974
        }
        val distance = location.distanceTo(place)
        if (distance - location.accuracy <= 30)
            return R.string.achievement_bandejo
        return null
    }

    private fun matchesLibrary(location: Location): Int? {
        val place = Location("").apply {
            latitude = -12.202193
            longitude = -38.972065
        }
        val distance = location.distanceTo(place)
        if (distance - location.accuracy <= 30)
            return R.string.achievement_dora_a_estudiosa
        return null
    }

    private fun matchesZoologyMuseum(location: Location): Int? {
        val place = Location("").apply {
            latitude = -12.198888
            longitude = -38.967986
        }
        val distance = location.distanceTo(place)
        if (distance - location.accuracy <= 30)
            return R.string.achievement_dora_a_aventureira
        return null
    }

    private fun matchesHogwarts(location: Location): Int? {
        val place = Location("").apply {
            latitude = -12.198144
            longitude = -38.971951
        }
        val distance = location.distanceTo(place)
        if (distance - location.accuracy <= 30)
            return R.string.achievement_dora_a_misteriosa
        return null
    }
}
