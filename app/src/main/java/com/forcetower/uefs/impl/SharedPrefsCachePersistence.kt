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

package com.forcetower.uefs.impl

import android.content.SharedPreferences
import com.forcetower.sagres.database.model.SagresClass
import com.forcetower.sagres.database.model.SagresCredential
import com.forcetower.sagres.database.model.SagresDisciplineResumed
import com.forcetower.sagres.database.model.SagresMessageScope
import com.forcetower.sagres.database.model.SagresPerson
import com.forcetower.sagres.persist.CachedPersistence
import com.forcetower.sagres.persist.Storage
import com.forcetower.uefs.core.util.toJson
import com.google.common.reflect.TypeToken
import com.google.gson.Gson

class SharedPrefsCachePersistence(
    private val preferences: SharedPreferences
) : CachedPersistence {
    private val access = object : Storage<SagresCredential>() {
        private val key = "credential"
        private val map = retrieveMap<SagresCredential>(key)

        override fun save(id: String, value: SagresCredential): Boolean {
            map[id] = value
            updateOnPrefs(key, map)
            return true
        }
        override fun retrieve(id: String) = map[id]
        override fun retrieveFromLink(link: String): SagresCredential? = null
    }

    private val clazz = object : Storage<SagresClass>() {
        private val key = "class"
        private val map = retrieveMap<SagresClass>(key)
        override fun save(id: String, value: SagresClass): Boolean {
            map[id] = value
            updateOnPrefs(key, map)
            return true
        }

        override fun retrieve(id: String) = map[id]

        override fun retrieveFromLink(link: String): SagresClass? {
            return map.values.firstOrNull { it.link == link }
        }
    }

    private val person = object : Storage<SagresPerson>() {
        private val key = "message_scope"
        private val map = retrieveMap<SagresPerson>(key)

        override fun save(id: String, value: SagresPerson): Boolean {
            map[id] = value
            updateOnPrefs(key, map)
            return true
        }

        override fun retrieve(id: String) = map[id]

        override fun retrieveFromLink(link: String): SagresPerson? {
            return map.values.firstOrNull { it.link == link }
        }
    }

    private val messageScope = object : Storage<SagresMessageScope>() {
        private val key = "message_scope"
        private val map = retrieveMap<SagresMessageScope>(key)

        override fun save(id: String, value: SagresMessageScope): Boolean {
            map[id] = value
            updateOnPrefs(key, map)
            return true
        }

        override fun retrieve(id: String) = map[id]

        override fun retrieveFromLink(link: String): SagresMessageScope? {
            return map.values.firstOrNull { it.uid == link }
        }
    }

    private val disciplineResumed = object : Storage<SagresDisciplineResumed>() {
        private val key = "discipline_resumed"
        private val map = retrieveMap<SagresDisciplineResumed>(key)

        override fun save(id: String, value: SagresDisciplineResumed): Boolean {
            map[id] = value
            updateOnPrefs(key, map)
            return true
        }

        override fun retrieve(id: String) = map[id]

        override fun retrieveFromLink(link: String): SagresDisciplineResumed? {
            return map.values.firstOrNull { it.link == link }
        }
    }

    private fun <T> retrieveMap(name: String): MutableMap<String, T> {
        val gson = Gson()
        val typeToken = object : TypeToken<MutableMap<String, T>>() {}.type
        val mapping = preferences.getString("cache_temp_mapping_$name", null) ?: "{}"
        return gson.fromJson(mapping, typeToken)
    }

    private fun updateOnPrefs(name: String, map: Any) {
        preferences.edit().putString("cache_temp_mapping_$name", map.toJson()).apply()
    }

    override fun access() = access
    override fun clazz() = clazz
    override fun person() = person
    override fun messageScope() = messageScope
    override fun disciplineResumed() = disciplineResumed
}