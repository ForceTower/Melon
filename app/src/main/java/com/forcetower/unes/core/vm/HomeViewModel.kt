/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.forcetower.unes.core.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import com.forcetower.unes.core.model.Access
import com.forcetower.unes.core.model.Message
import com.forcetower.unes.core.model.Profile
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
