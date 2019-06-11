package com.forcetower.uefs.core.storage.repository

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.unes.Account
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.core.storage.network.adapter.ApiResponse
import com.forcetower.uefs.core.storage.network.adapter.asLiveData
import com.forcetower.uefs.core.storage.resource.NetworkBoundResource
import com.forcetower.uefs.core.storage.resource.Resource
import javax.inject.Inject

class AccountRepository @Inject constructor(
    private val database: UDatabase,
    private val executor: AppExecutors,
    private val service: UService,
    private val preferences: SharedPreferences
) {
    fun getAccount(): LiveData<Resource<Account>> {
        return object : NetworkBoundResource<Account, Account>(executor) {
            override fun loadFromDb(): LiveData<Account> {
                return database.accountDao().getAccount()
            }
            override fun shouldFetch(it: Account?) = true
            override fun createCall(): LiveData<ApiResponse<Account>> {
                return service.getAccount().asLiveData()
            }
            override fun saveCallResult(value: Account) {
                preferences.edit().putBoolean("ach_night_mode_enabled", value.darkThemeEnabled).apply()
                database.accountDao().insert(value)
            }
        }.asLiveData()
    }
}
