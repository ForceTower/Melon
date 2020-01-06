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

package com.forcetower.uefs.dashboard.feature

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.model.unes.Account
import com.forcetower.uefs.core.storage.repository.SagresDataRepository
import com.forcetower.uefs.dashboard.core.storage.repository.DashboardRepository
import javax.inject.Inject

class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val dataRepository: SagresDataRepository
) : ViewModel() {
    val course: LiveData<String?> by lazy { dataRepository.getCourse() }
    val account: LiveData<Account> = repository.getAccount()
    val currentClass = repository.getCurrentClass()
}