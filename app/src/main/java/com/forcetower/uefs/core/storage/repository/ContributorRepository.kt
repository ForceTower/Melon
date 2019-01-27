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

package com.forcetower.uefs.core.storage.repository

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.unes.Contributor
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.github.GithubContributor
import com.forcetower.uefs.core.storage.network.github.GithubService
import com.forcetower.uefs.core.storage.resource.NetworkBoundResource
import com.forcetower.uefs.core.storage.resource.Resource
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

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
            override fun createCall() = service.getContributors()

            override fun saveCallResult(value: List<GithubContributor>) {
                // This is kinda messy, but since this running on a worker thread...
                findDetailsAndSave(value)
            }
        }.asLiveData()
    }

    @WorkerThread
    private fun findDetailsAndSave(value: List<GithubContributor>) {
        value.filter { it.author != null }
            .map {
                val name = it.author?.login
                if (name != null) {
                    try {
                        val response = service.getUserDirect(name).execute()
                        if (response.isSuccessful) {
                            val user = response.body()
                            val contributor = it.toContributor()!!
                            contributor.name = user?.name ?: contributor.login
                            contributor.bio = user?.bio
                            contributor
                        } else {
                            Timber.d("User fetch failed with code: ${response.code()}")
                            it.author
                        }
                    } catch (t: Throwable) {
                        Timber.d("Failed to fetch user details... Message: ${t.message}")
                        it.author
                    }
                } else {
                    it.toContributor()
                }
            }.filter { it != null }
            .forEach {
                if (it != null) {
                    database.contributorDao().insert(it)
                }
            }
    }
}