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

package com.forcetower.uefs.core.model.siecomp

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(
    foreignKeys = [
        ForeignKey(entity = Session::class, parentColumns = ["uid"], childColumns = ["session_id"], onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE),
        ForeignKey(entity = Speaker::class, parentColumns = ["uid"], childColumns = ["speaker_id"], onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE)
    ],
    indices = [
        Index(value = ["session_id", "speaker_id"], unique = true),
        Index(value = ["session_id"]),
        Index(value = ["speaker_id"]),
        Index(value = ["uuid"], unique = true)
    ]
)
data class SessionSpeaker(
    @SerializedName(value = "id")
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    @ColumnInfo(name = "session_id")
    val session: Long,
    @ColumnInfo(name = "speaker_id")
    val speaker: Long,
    val uuid: String = UUID.randomUUID().toString()
)
