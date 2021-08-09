/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.forcetower.uefs.feature.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.core.lifecycle.Event
import com.forcetower.uefs.core.model.unes.ProfileStatement
import com.forcetower.uefs.core.model.unes.SStudent
import com.forcetower.uefs.core.storage.repository.ProfileRepository
import com.forcetower.uefs.core.storage.resource.Status
import com.forcetower.uefs.feature.shared.extensions.setValueIfNew
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel(), ProfileInteractor {
    val commonProfile = repository.getCommonProfile()
    private var userId: Long? = null
    private val profileId = MutableLiveData<Long?>()

    private val _profile = MediatorLiveData<SStudent?>()
    val profile: LiveData<SStudent?>
        get() = _profile

    val account = repository.getAccountDatabase()

    private val _statements = MediatorLiveData<List<ProfileStatement>>()
    val statements: LiveData<List<ProfileStatement>>
        get() = _statements

    private val _sendingStatement = MediatorLiveData<Boolean>()
    val sendingStatement: LiveData<Boolean>
        get() = _sendingStatement

    private val _messages = MutableLiveData<Event<String>>()
    val messages: LiveData<Event<String>>
        get() = _messages

    private val _statementSentSignal = MutableLiveData<Event<Boolean>>()
    val statementSentSignal: LiveData<Event<Boolean>>
        get() = _statementSentSignal

    init {
        _profile.addSource(profileId) {
            refreshProfile(it)
            refreshStatements(it, userId)
        }
    }

    private fun refreshStatements(profileId: Long?, userId: Long?) {
        profileId ?: return
        userId ?: return
        val source = repository.loadStatements(profileId, userId)
        _statements.addSource(source) {
            Timber.d("Resource status ${it.status}")
            val data = it.data ?: emptyList()
            _statements.value = data
        }
    }

    private fun refreshProfile(profileId: Long?) {
        if (profileId != null) {
            val source = repository.loadProfile(profileId)
            Timber.d("Fetching profile...")
            _profile.addSource(source) {
                Timber.d("Profile load update ${it.status}")
                val data = it.data
                if (data != null) {
                    _profile.value = data
                }
            }
        } else {
            Timber.d("No profile information available")
        }
    }

    fun getMeProfile() = repository.getMeProfile()

    fun setProfileId(newProfileId: Long?) {
        Timber.d("Setting new profile id: $newProfileId")
        profileId.setValueIfNew(newProfileId)
    }

    /**
     * This method should be replaced by a reactive thing together with setProfileId
     */
    fun setUserId(userId: Long) {
        Timber.d("Setting new userId: $userId")
        this.userId = userId
    }

    fun onSendStatement(statement: String, profileId: Long, hidden: Boolean) {
        if (_sendingStatement.value == true) {
            Timber.d("Already sending data")
            return
        }

        _sendingStatement.value = true
        val source = repository.sendStatement(statement, profileId, hidden)
        _sendingStatement.addSource(source) {
            _sendingStatement.removeSource(source)
            if (it.message != null) {
                _messages.value = Event(it.message)
            }

            if (it.status == Status.SUCCESS) {
                _statementSentSignal.value = Event(true)
                val uid = userId
                if (uid != null) {
                    repository.loadStatements(profileId, uid)
                }
            }

            _sendingStatement.value = false
        }
    }

    override fun onPictureClick() = Unit

    override fun onAcceptStatement(statement: ProfileStatement) {
        repository.acceptStatementAsync(statement)
    }

    override fun onRefuseStatement(statement: ProfileStatement) {
        repository.refuseStatementAsync(statement)
    }

    override fun onDeleteStatement(statement: ProfileStatement) {
        repository.deleteStatementAsync(statement)
    }
}
