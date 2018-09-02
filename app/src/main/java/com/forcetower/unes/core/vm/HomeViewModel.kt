/*
 * Copyright (c) 2018.
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

package com.forcetower.unes.core.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import com.forcetower.unes.core.model.unes.Access
import com.forcetower.unes.core.model.unes.Message
import com.forcetower.unes.core.model.unes.Profile
import com.forcetower.unes.core.storage.repository.SagresDataRepository
import com.forcetower.unes.core.storage.repository.LoginSagresRepository
import javax.inject.Inject

class HomeViewModel
@Inject constructor(
        private val loginSagresRepository: LoginSagresRepository,
        private val dataRepository: SagresDataRepository
): ViewModel() {
    val access: LiveData<Access?> by lazy { loginSagresRepository.getAccess() }
    val profile: LiveData<Profile> by lazy { loginSagresRepository.getProfileMe() }
    val messages: LiveData<PagedList<Message>> by lazy { dataRepository.getMessages() }
}
