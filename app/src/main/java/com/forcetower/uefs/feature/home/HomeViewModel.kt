/*
 * Copyright (c) 2019.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.forcetower.uefs.feature.home

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.model.unes.Access
import com.forcetower.uefs.core.model.unes.Account
import com.forcetower.uefs.core.model.unes.Message
import com.forcetower.uefs.core.model.unes.Profile
import com.forcetower.uefs.core.model.unes.SagresFlags
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.storage.repository.AccountRepository
import com.forcetower.uefs.core.storage.repository.FirebaseMessageRepository
import com.forcetower.uefs.core.storage.repository.LoginSagresRepository
import com.forcetower.uefs.core.storage.repository.SagresDataRepository
import com.forcetower.uefs.core.storage.repository.SettingsRepository
import com.forcetower.uefs.core.storage.repository.cloud.AuthRepository
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.storage.resource.Status
import com.forcetower.uefs.core.vm.Event
import com.forcetower.uefs.easter.darktheme.DarkThemeRepository
import javax.inject.Inject

class HomeViewModel @Inject constructor(
    private val loginSagresRepository: LoginSagresRepository,
    private val dataRepository: SagresDataRepository,
    private val firebaseMessageRepository: FirebaseMessageRepository,
    private val settingsRepository: SettingsRepository,
    private val darkThemeRepository: DarkThemeRepository,
    private val authRepository: AuthRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {
    private val _snackbar = MutableLiveData<Event<String>>()
    val snackbarMessage: LiveData<Event<String>>
        get() = _snackbar

    val isDarkModeEnabled: LiveData<Boolean>
        get() = settingsRepository.hasDarkModeEnabled()

    private val _openProfileCase = MediatorLiveData<Event<String>>()
    val openProfileCase: LiveData<Event<String>>
        get() = _openProfileCase

    private val _passwordChangeProcess = MediatorLiveData<Event<Resource<Boolean>>>()
    val passwordChangeProcess: LiveData<Event<Resource<Boolean>>>
        get() = _passwordChangeProcess

    val access: LiveData<Access?> by lazy { loginSagresRepository.getAccess() }
    val profile: LiveData<Profile?> by lazy { loginSagresRepository.getProfileMe() }
    val messages: LiveData<List<Message>> by lazy { dataRepository.getMessages() }
    val semesters: LiveData<List<Semester>> by lazy { dataRepository.getSemesters() }
    val course: LiveData<String?> by lazy { dataRepository.getCourse() }
    val account: LiveData<Resource<Account>> = accountRepository.getAccount()

    val flags: LiveData<SagresFlags?> by lazy { dataRepository.getFlags() }

    fun logout() = dataRepository.logout()

    fun showSnack(message: String) {
        _snackbar.value = Event(message)
    }

    fun onMeProfileClicked() {
        _openProfileCase.addSource(profile) {
            _openProfileCase.removeSource(profile)
            if (it != null) {
                _openProfileCase.value = Event(it.uuid)
            }
        }
    }

    fun subscribeToTopics(vararg topics: String) {
        firebaseMessageRepository.subscribe(topics)
    }

    fun verifyDarkTheme() = darkThemeRepository.getPreconditions()
    fun lightWeightCalcScore() = dataRepository.lightweightCalcScore()
    fun changeAccessValidation(valid: Boolean) = dataRepository.changeAccessValidation(valid)
    fun attemptNewPasswordLogin(password: String) {
        val source = dataRepository.attemptLoginWithNewPassword(password)
        _passwordChangeProcess.addSource(source) {
            if (it.status == Status.SUCCESS) {
                _passwordChangeProcess.removeSource(source)
            }
            _passwordChangeProcess.value = Event(it)
        }
    }

    fun connectToServiceIfNeeded() {
        authRepository.performAccountSyncStateIfNeededAsync()
    }

    @MainThread
    fun sendToken(): LiveData<Boolean> {
        return firebaseMessageRepository.sendNewTokenOrNot()
    }
}
