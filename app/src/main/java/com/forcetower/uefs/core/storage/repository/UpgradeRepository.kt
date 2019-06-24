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