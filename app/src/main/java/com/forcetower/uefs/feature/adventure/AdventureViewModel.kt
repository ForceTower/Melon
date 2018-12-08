package com.forcetower.uefs.feature.adventure

import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.storage.repository.AdventureRepository
import javax.inject.Inject

class AdventureViewModel @Inject constructor(
    private val repository: AdventureRepository
) : ViewModel(), AdventureInteractor {

    override fun beginAdventure() {

    }

    override fun leave() {

    }

    override fun turnOnLocations() {

    }

    override fun checkAchievements() {

    }

    override fun isConnected(): Boolean {
        return true
    }
}