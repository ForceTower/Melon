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

package com.forcetower.uefs.core.storage.repository

import androidx.lifecycle.LiveData
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.unes.Account
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.core.storage.network.adapter.ApiResponse
import com.forcetower.uefs.core.storage.network.adapter.asLiveData
import com.forcetower.uefs.core.storage.resource.NetworkBoundResource
import com.forcetower.uefs.core.storage.resource.Resource
import timber.log.Timber
import javax.inject.Inject

class AccountRepository @Inject constructor(
    private val database: UDatabase,
    private val executor: AppExecutors,
    private val service: UService
) {
    fun getAccount(): LiveData<Resource<Account>> {
        return object : NetworkBoundResource<Account, Account>(executor) {
            override fun loadFromDb(): LiveData<Account> {
                return database.accountDao().getAccount()
            }
            override fun shouldFetch(it: Account?): Boolean {
                Timber.d("A account was found? ${it != null}")
                return true
            }
            override fun createCall(): LiveData<ApiResponse<Account>> {
                return service.getAccount().asLiveData()
            }
            override fun saveCallResult(value: Account) {
                database.accountDao().insert(value)
            }
        }.asLiveData()
    }

    fun getAccountOnDatabase(): LiveData<Account?> {
        return database.accountDao().getAccountNullable()
    }

    fun getAccountSync(): Account? {
        return database.accountDao().getAccountDirect()
    }
}
