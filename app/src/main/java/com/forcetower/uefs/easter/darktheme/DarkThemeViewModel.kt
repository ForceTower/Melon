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

package com.forcetower.uefs.easter.darktheme

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.model.service.FirebaseProfile
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.storage.resource.Status
import com.forcetower.uefs.core.vm.Event
import javax.inject.Inject

class DarkThemeViewModel @Inject constructor(
    private val repository: DarkThemeRepository
) : ViewModel() {

    val preconditions: LiveData<List<Precondition>>
        get() = repository.getPreconditions()

    val profile: LiveData<FirebaseProfile?>
        get() = repository.getFirebaseProfile()

    private val _currentCall = MediatorLiveData<Event<Resource<Boolean>>>()
    val currentCall: LiveData<Event<Resource<Boolean>>>
        get() = _currentCall

    fun sendDarkThemeTo(username: String?) {
        val source = repository.sendDarkThemeTo(username)
        _currentCall.addSource(source) {
            if (it.status == Status.SUCCESS || it.status == Status.ERROR) {
                _currentCall.removeSource(source)
            }
            _currentCall.value = Event(it)
        }
    }
}
