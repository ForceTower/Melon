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
import androidx.room.Index
import androidx.room.PrimaryKey
import com.forcetower.sagres.database.model.SagresDisciplineClassItem
import java.util.UUID

@Entity(
    foreignKeys = [
        ForeignKey(entity = ClassGroup::class, parentColumns = ["uid"], childColumns = ["group_id"], onUpdate = ForeignKey.CASCADE, onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index(value = ["group_id", "number"], unique = true),
        Index(value = ["number_of_materials"], unique = false),
        Index(value = ["situation"], unique = false),
        Index(value = ["date"], unique = false),
        Index(value = ["is_new"], unique = false),
        Index(value = ["uuid"], unique = true)
    ]
)
data class ClassItem(
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    @ColumnInfo(name = "group_id")
    val groupId: Long,
    val number: Int,
    var situation: String?,
    var subject: String?,
    var date: String?,
    @ColumnInfo(name = "number_of_materials")
    var numberOfMaterials: Int,
    @ColumnInfo(name = "material_links")
    var materialLinks: String,
    @ColumnInfo(name = "is_new")
    val isNew: Boolean,
    val uuid: String = UUID.randomUUID().toString()
) {
    fun selectiveCopy(other: ClassItem) {
        situation = other.situation
        subject = other.subject
        date = other.date
        numberOfMaterials = other.numberOfMaterials
        materialLinks = other.materialLinks
    }

    companion object {
        fun createFromSagres(group: Long, value: SagresDisciplineClassItem): ClassItem {
            return ClassItem(
                groupId = group,
                number = value.number,
                situation = value.situation,
                subject = value.subject,
                date = value.date,
                numberOfMaterials = value.numberOfMaterials,
                materialLinks = value.materialLink,
                isNew = true
            )
        }
    }
}
