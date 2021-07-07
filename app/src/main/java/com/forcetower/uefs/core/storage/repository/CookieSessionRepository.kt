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

import android.content.Context
import com.forcetower.sagres.SagresNavigator
import com.forcetower.uefs.core.model.service.SavedCookie
import com.forcetower.uefs.core.storage.cookies.CachedCookiePersistor
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.service.NotificationCreator
import okhttp3.Cookie
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CookieSessionRepository @Inject constructor(
    private val cookiesPersistence: CachedCookiePersistor,
    private val service: UService,
    private val context: Context
) {
    suspend fun findAndSaveCookies() {
        try {
            val cached = cookiesPersistence.loadAll()
            val filtered = cached.filter { it.name == "ASP.NET_SessionId" || it.name == ".PORTALAUTH" }

            Timber.d("What cached has returned: $filtered")
            // there MUST be 2 parts
            if (filtered.size < 2) {
                NotificationCreator.showSimpleNotification(context, "Então...", "Essa sessão é completamente inutil")
                Timber.d("Session contains a invalid amount of required elements")
            } else {
                val (one, two) = filtered
                val prepared = bakeCookie(one, two)
                Timber.d("Cookies that will be sent: $prepared")
                val response = service.prepareSession(prepared)
                Timber.d("Will we eat cookies? TOGETHER? ${response.success}")
            }
        } catch (error: Throwable) {
            // TODO This must prompt a warning
            Timber.i(error, "This user wont update. Omega lul")
        }
    }

    private fun bakeCookie(one: Cookie, two: Cookie): SavedCookie {
        val sessionId = if (one.name == "ASP.NET_SessionId") {
            one.value
        } else {
            two.value
        }

        val auth = if (one.name == ".PORTALAUTH") {
            one.value
        } else {
            two.value
        }

        return SavedCookie(auth, sessionId)
    }

    suspend fun getGoodCookies() {
        try {
            val cookies = service.getSession().data
            if (cookies == null) {
                NotificationCreator.showSimpleNotification(context, "Vish...", "O seu login foi pro espaço, entra de novo")
                // TODO Must actually disconnect user OMEGALUL
                Timber.i("The cookie that is good no one wants to give!")
            } else {
                val elemental = ".PORTALAUTH=${cookies.auth};ASP.NET_SessionId=${cookies.sessionId}"
                SagresNavigator.instance.setCookiesOnClient(elemental)
            }
        } catch (error: Throwable) {
            Timber.i(error, "This user is actually service or internet ducked him")
        }
    }

    suspend fun invalidateCookies() {
        try {
            val response = service.invalidateSession()
            Timber.d("Has service actually done something? ${response.success}")
        } catch (error: Throwable) {
            Timber.i(error, "Nothing actually happened. Right?")
        }
    }
}
