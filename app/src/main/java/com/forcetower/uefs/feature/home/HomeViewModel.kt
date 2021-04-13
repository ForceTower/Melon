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

package com.forcetower.uefs.feature.home

import android.content.Context
import android.net.Uri
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forcetower.core.lifecycle.Event
import com.forcetower.uefs.core.model.unes.Access
import com.forcetower.uefs.core.model.unes.Account
import com.forcetower.uefs.core.model.unes.Course
import com.forcetower.uefs.core.model.unes.Message
import com.forcetower.uefs.core.model.unes.Profile
import com.forcetower.uefs.core.model.unes.SagresFlags
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.storage.repository.AccountRepository
import com.forcetower.uefs.core.storage.repository.CookieSessionRepository
import com.forcetower.uefs.core.storage.repository.FirebaseMessageRepository
import com.forcetower.uefs.core.storage.repository.LoginSagresRepository
import com.forcetower.uefs.core.storage.repository.ProfileRepository
import com.forcetower.uefs.core.storage.repository.SagresDataRepository
import com.forcetower.uefs.core.storage.repository.UserSessionRepository
import com.forcetower.uefs.core.storage.repository.cloud.AffinityQuestionRepository
import com.forcetower.uefs.core.storage.repository.cloud.AuthRepository
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.storage.resource.Status
import com.forcetower.uefs.core.work.image.UploadImageToStorage
import com.forcetower.uefs.easter.darktheme.DarkThemeRepository
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val loginSagresRepository: LoginSagresRepository,
    private val dataRepository: SagresDataRepository,
    private val firebaseMessageRepository: FirebaseMessageRepository,
    private val darkThemeRepository: DarkThemeRepository,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val context: Context,
    private val sessionRepository: UserSessionRepository,
    private val accountRepository: AccountRepository,
    private val affinityRepository: AffinityQuestionRepository,
    private val userCookieSessionRepository: CookieSessionRepository,
    @Named("flagSnowpiercerEnabled")
    private val snowpiercerEnabled: Boolean
) : ViewModel() {
    private var selectImageUri: Uri? = null

    @AppUpdateType
    var updateType: Int? = null

    private val _snackbar = MutableLiveData<Event<String>>()
    val snackbarMessage: LiveData<Event<String>>
        get() = _snackbar

    private val _passwordChangeProcess = MediatorLiveData<Event<Resource<Boolean>>>()
    val passwordChangeProcess: LiveData<Event<Resource<Boolean>>>
        get() = _passwordChangeProcess

    private val _inAppUpdateStatus = MutableLiveData<Int>()
    val inAppUpdateStatus: LiveData<Int>
        get() = _inAppUpdateStatus

    private val _onMoveToSchedule = MutableLiveData<Event<Unit>>()
    val onMoveToSchedule: LiveData<Event<Unit>>
        get() = _onMoveToSchedule

    val access: LiveData<Access?> by lazy { loginSagresRepository.getAccess() }
    val profile: LiveData<Profile?> by lazy { loginSagresRepository.getProfileMe() }
    val messages: LiveData<List<Message>> by lazy { dataRepository.getMessages() }
    val semesters: LiveData<List<Semester>> by lazy { dataRepository.getSemesters() }
    val course: LiveData<String?> by lazy { dataRepository.getCourse() }
    val account: LiveData<Resource<Account>> = accountRepository.getAccount()
    val databaseAccount = accountRepository.getAccountOnDatabase()
    val flags: LiveData<SagresFlags?> by lazy { dataRepository.getFlags() }
    val scheduleHideCount: LiveData<Int> = dataRepository.getScheduleHideCount()

    fun uploadImageToStorage() {
        val uri = selectImageUri
        uri ?: return
        UploadImageToStorage.createWorker(context, uri)
    }

    fun setSelectedImage(uri: Uri) {
        selectImageUri = uri
    }

    fun logout() = dataRepository.logout()

    fun showSnack(message: String) {
        _snackbar.value = Event(message)
    }

    fun subscribeToTopics(vararg topics: String) {
        firebaseMessageRepository.subscribe(topics)
    }

    fun verifyDarkTheme() = darkThemeRepository.getPreconditions()

    fun lightWeightCalcScore() = dataRepository.lightweightCalcScore()

    fun changeAccessValidation(valid: Boolean) = dataRepository.changeAccessValidation(valid)

    fun attemptNewPasswordLogin(password: String) {
        if (!snowpiercerEnabled) {
            val source = dataRepository.attemptLoginWithNewPassword(password)
            _passwordChangeProcess.addSource(source) {
                if (it.status == Status.SUCCESS) {
                    _passwordChangeProcess.removeSource(source)
                }
                _passwordChangeProcess.value = Event(it)
            }
        } else {
            viewModelScope.launch {
                _passwordChangeProcess.value = Event(Resource.loading(null))
                val result = dataRepository.loginWithNewPasswordSuspend(password)
                _passwordChangeProcess.value = Event(Resource.success(result))
            }
        }
    }

    fun connectToServiceIfNeeded() {
        authRepository.performAccountSyncStateIfNeededAsync()
    }

    @MainThread
    fun sendToken(): LiveData<Boolean> {
        return firebaseMessageRepository.sendNewTokenOrNot()
    }

    fun setSelectedCourse(course: Course) {
        profileRepository.updateUserCourse(course)
    }

    fun onSessionStarted() {
        sessionRepository.onSessionStartedAsync()
    }

    fun onUserInteraction() {
        sessionRepository.onUserInteractionAsync()
    }

    fun onUserClickedAd() {
        sessionRepository.onUserClickedAdAsync()
    }

    fun onUserAdImpression() {
        sessionRepository.onUserAdImpressionAsync()
    }

    fun getAccountSync(): Account? {
        return accountRepository.getAccountSync()
    }

    fun onSyncSessions() {
        sessionRepository.onSyncSessionsAsync()
    }

    fun setCurrentUpdateState(@InstallStatus installStatus: Int) {
        _inAppUpdateStatus.value = installStatus
    }

    fun getMeProfile() {
        profileRepository.getMeProfileAsync()
    }

    fun onMoveToSchedule() {
        _onMoveToSchedule.value = Event(Unit)
    }

    fun getAffinityQuestions() {
        affinityRepository.getAffinityQuestionsAsync()
    }

    fun goodCookies() {
        userCookieSessionRepository.getGoodCookies()
    }
}
