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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.time.ZonedDateTime

@Entity
data class Event(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val name: String,
    val description: String,
    @SerializedName("image_url")
    val imageUrl: String,
    @SerializedName("creator_name")
    val creatorName: String,
    @SerializedName("creator_id")
    val creatorId: Long,
    @SerializedName("offered_by")
    val offeredBy: String,
    @SerializedName("start_date")
    val startDate: ZonedDateTime,
    @SerializedName("end_date")
    val endDate: ZonedDateTime,
    val location: String,
    val price: Double?,
    @SerializedName("certificate_hours")
    val certificateHours: Int?,
    @SerializedName("course_id")
    val courseId: Int?,
    val featured: Boolean,
    @SerializedName("created_at")
    val createdAt: ZonedDateTime,
    val approved: Boolean,
    @SerializedName("can_modify")
    val canModify: Boolean,
    val participating: Boolean,
    val fakeTemp: Boolean? = false,
    val sending: Boolean? = false,
    @SerializedName("register_page")
    val registerPage: String?,
    @SerializedName("can_approve")
    @ColumnInfo(defaultValue = "0")
    val canApprove: Boolean = false
) {

    override fun toString(): String {
        return name
    }

    companion object {
        const val COLLECTION = "events"
    }
}
