package com.forcetower.uefs.feature.adventure

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.storage.repository.AdventureRepository
import com.forcetower.uefs.core.vm.Event
import javax.inject.Inject

class AdventureViewModel @Inject constructor(
    private val repository: AdventureRepository
) : ViewModel(), AdventureInteractor {
    private val _locations = MutableLiveData<Boolean?>()
    val locations: LiveData<Boolean?>
        get() = _locations

    private val _achievements = MutableLiveData<Event<Any?>>()
    val achievements: LiveData<Event<Any?>>
        get() = _achievements

    private val _leave = MutableLiveData<Event<Boolean>>()
    val leave: LiveData<Event<Boolean>>
        get() = _leave

    private val _start = MutableLiveData<Event<Any?>>()
    val start: LiveData<Event<Any?>>
        get() = _start

    override fun beginAdventure() {
        _start.value = Event(Any())
    }

    override fun leave() {
        _leave.value = Event(true)
    }

    override fun turnOnLocations() {
        val requesting = _locations.value ?: false
        _locations.value = !requesting
    }

    override fun openAchievements() {
        _achievements.value = Event(Any())
    }

    override fun isConnected(): Boolean {
        return true
    }

    fun checkAchievements(email: String? = null) {
        repository.checkAchievements(email)
    }
}