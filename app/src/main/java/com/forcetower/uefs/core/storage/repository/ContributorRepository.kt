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

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.unes.Contributor
import com.forcetower.uefs.core.model.unes.GithubContributor
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.adapter.asLiveData
import com.forcetower.uefs.core.storage.network.github.GithubService
import com.forcetower.uefs.core.storage.resource.NetworkBoundResource
import com.forcetower.uefs.core.storage.resource.Resource
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class ContributorRepository @Inject constructor(
    private val database: UDatabase,
    private val executors: AppExecutors,
    private val service: GithubService
) {

    @MainThread
    fun loadContributors(): LiveData<Resource<List<Contributor>>> {
        return object : NetworkBoundResource<List<Contributor>, List<GithubContributor>>(executors) {
            override fun loadFromDb() = database.contributorDao().getContributors()
            override fun shouldFetch(it: List<Contributor>?) = true
            override fun createCall() = service.getContributors().asLiveData()

            override fun saveCallResult(value: List<GithubContributor>) {
                // This is kinda messy, but since this running on a worker thread...
                findDetailsAndSave(value)
            }
        }.asLiveData()
    }

    @WorkerThread
    private fun findDetailsAndSave(value: List<GithubContributor>) {
        value.map {
            var contributor = Contributor(
                it.id,
                it.login,
                it.total,
                it.login,
                it.avatarUrl,
                it.htmlUrl,
                it.url,
                null
            )
            try {
                val response = service.getUserDirect(it.login).execute()
                if (response.isSuccessful) {
                    val user = response.body()
                    contributor = contributor.copy(name = user?.name ?: it.login, bio = user?.bio)
                } else {
                    Timber.d("User fetch failed with code: ${response.code()}")
                }
            } catch (t: Throwable) {
                Timber.d("Failed to fetch user details... Message: ${t.message}")
            }
            contributor
        }.forEach {
            database.contributorDao().insert(it)
        }
    }
}
