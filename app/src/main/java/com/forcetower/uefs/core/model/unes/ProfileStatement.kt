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

package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.time.ZonedDateTime

@Entity
data class ProfileStatement(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    // This matches to user_id not the student_id
    @SerializedName("receiver_id")
    val receiverId: Long,
    // The user id that sent this
    @SerializedName("sender_id")
    val senderId: Long,
    @SerializedName("sender_name")
    val senderName: String?,
    @SerializedName("sender_picture")
    val senderPicture: String?,
    val hidden: Boolean,
    val text: String,
    val likes: Int,
    val approved: Boolean,
    @SerializedName("created_at")
    val createdAt: ZonedDateTime,
    @SerializedName("updated_at")
    val updatedAt: ZonedDateTime
)

data class CreateStatementParams(
    val statement: String,
    val hidden: Boolean,
    @SerializedName("profile_id")
    val profileId: Long
)
