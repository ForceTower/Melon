/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(indices = [
    Index(value = ["uuid"], unique = true),
    Index(value = ["start"], unique = true)
])
data class SyncRegistry(
    @PrimaryKey(autoGenerate = true)
    var uid: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val start: Long = System.currentTimeMillis(),
    var end: Long? = null,
    var completed: Boolean = false,
    var success: Boolean = false,
    var error: Int = 0,
    val executor: String,
    var message: String = "Nothing",
    val networkType: Int,
    val network: String,
    var skipped: Int = 0
)

enum class NetworkType {
    WIFI,
    CELLULAR,
    OTHER
}