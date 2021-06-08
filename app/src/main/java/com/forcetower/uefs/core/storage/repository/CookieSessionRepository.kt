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

import android.webkit.CookieManager
import androidx.annotation.WorkerThread
import com.forcetower.sagres.SagresNavigator
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.storage.network.UService
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CookieSessionRepository @Inject constructor(
    private val service: UService
) {
    suspend fun findAndSaveCookies() {
        try {
            val cookies = CookieManager.getInstance().getCookie("http://academico2.uefs.br/")
            Timber.d("Cookies that will be sent: $cookies")
            val response = service.prepareSession(cookies)
            Timber.d("Will we eat cookie? TOGETHER? ${response.success}")
        } catch (error: Throwable) {
            Timber.e(error, "This user wont update. Omega lul")
        }
    }

    suspend fun getGoodCookies() {
        try {
            val cookies = service.getSession().data
            if (cookies == null) {
                Timber.e("The cookie that is good no one wants to give!")
            } else {
                SagresNavigator.instance.setCookiesOnClient(cookies)
            }
        } catch (error: Throwable) {
            Timber.e(error, "This user is actually service or internet ducked him")
        }
    }

    suspend fun invalidateCookies() {
        try {
            val response = service.invalidateSession()
            Timber.d("Did we forgot about you? ${response.success}")
        } catch (error: Throwable) {
            Timber.e(error, "Nothing actually happened. Right?")
        }
    }
}
