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
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import com.forcetower.sagres.database.model.SagresDiscipline
import java.util.UUID

@Entity(
    foreignKeys = [
        ForeignKey(entity = Discipline::class, parentColumns = ["uid"], childColumns = ["discipline_id"], onDelete = CASCADE, onUpdate = CASCADE),
        ForeignKey(entity = Semester::class, parentColumns = ["uid"], childColumns = ["semester_id"], onDelete = CASCADE, onUpdate = CASCADE)
    ],
    indices = [
        Index(value = ["semester_id"]),
        Index(value = ["discipline_id", "semester_id"], unique = true),
        Index(value = ["uuid"], unique = true)
    ]
)
data class Class(
    @PrimaryKey(autoGenerate = true)
    var uid: Long = 0,
    @ColumnInfo(name = "discipline_id")
    val disciplineId: Long,
    @ColumnInfo(name = "semester_id")
    val semesterId: Long,
    var status: String? = null,
    @ColumnInfo(name = "final_score")
    var finalScore: Double? = null,
    @ColumnInfo(name = "partial_score")
    var partialScore: Double? = null,
    val uuid: String = UUID.randomUUID().toString(),
    var missedClasses: Int = 0,
    var lastClass: String = "",
    var nextClass: String = "",
    @ColumnInfo(name = "schedule_only")
    var scheduleOnly: Boolean = false
) {

    fun selectiveCopy(dis: SagresDiscipline, validated: Boolean) {
        if (!dis.nextClass.isBlank()) nextClass = dis.nextClass
        if (!dis.lastClass.isBlank()) lastClass = dis.lastClass
        if (dis.missedClasses >= 0) missedClasses = dis.missedClasses
        if (!dis.situation.isNullOrBlank()) status = dis.situation
        scheduleOnly = !validated
    }

    fun isInFinal(): Boolean {
        val operation = partialScore
        return if (operation == null) {
            false
        } else {
            operation in 3.0..6.97
        }
    }
}
