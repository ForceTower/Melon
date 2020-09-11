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

package com.forcetower.uefs.core.storage.cookies

import android.content.Context
import android.content.SharedPreferences
import com.forcetower.sagres.cookies.CookiePersistor
import com.forcetower.sagres.cookies.SerializableCookie
import okhttp3.Cookie
import java.util.ArrayList

class PrefsCookiePersistor(context: Context) : CookiePersistor {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("PrefsCookiePersistence", Context.MODE_PRIVATE)

    override fun loadAll(): List<Cookie> {
        val cookies = ArrayList<Cookie>(sharedPreferences.all.size)

        for ((_, value) in sharedPreferences.all) {
            val serializedCookie = value as String
            val cookie = SerializableCookie().decode(serializedCookie)
            if (cookie != null) {
                cookies.add(cookie)
            }
        }
        return cookies
    }

    override fun saveAll(cookies: Collection<Cookie>) {
        val editor = sharedPreferences.edit()
        for (cookie in cookies) {
            editor.putString(createCookieKey(cookie), SerializableCookie().encode(cookie))
        }
        editor.apply()
    }

    override fun removeAll(cookies: Collection<Cookie>) {
        val editor = sharedPreferences.edit()
        for (cookie in cookies) {
            editor.remove(createCookieKey(cookie))
        }
        editor.apply()
    }

    private fun createCookieKey(cookie: Cookie): String {
        return (if (cookie.secure) "https" else "http") + "://" + cookie.domain + cookie.path + "|" + cookie.name
    }

    override fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}
