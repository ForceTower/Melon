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

import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.UService
import com.google.android.gms.tasks.Tasks
import com.google.firebase.iid.FirebaseInstanceId
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpgradeRepository @Inject constructor(
    private val service: UService,
    private val database: UDatabase,
    private val executors: AppExecutors
) {
    fun onUpgrade() {
        executors.networkIO().execute {
            database.accessTokenDao().getAccessTokenDirect() ?: return@execute
            val task = FirebaseInstanceId.getInstance().instanceId
            try {
                val result = Tasks.await(task)
                val value = result?.token ?: return@execute
                service.sendToken(mapOf("token" to value)).execute()
            } catch (e: Throwable) {
                Timber.e(e, "Well... Failed...")
            }
        }
    }
}
