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

import androidx.annotation.Nullable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.ForeignKey.SET_NULL
import androidx.room.Index
import androidx.room.PrimaryKey
import com.forcetower.sagres.database.model.SagresMaterialLink
import java.util.UUID

@Entity(
    foreignKeys = [
        ForeignKey(entity = ClassGroup::class, parentColumns = ["uid"], childColumns = ["group_id"], onDelete = CASCADE, onUpdate = CASCADE),
        ForeignKey(entity = ClassItem::class, parentColumns = ["uid"], childColumns = ["class_item_id"], onDelete = SET_NULL, onUpdate = CASCADE)
    ],
    indices = [
        Index(value = ["name"], unique = false),
        Index(value = ["link"], unique = false),
        Index(value = ["group_id"], unique = false),
        Index(value = ["class_item_id"], unique = false),
        Index(value = ["uuid"], unique = true),
        Index(value = ["is_new"], unique = false),
        Index(value = ["name", "link", "group_id"], unique = true)
    ]
)
data class ClassMaterial(
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    @ColumnInfo(name = "group_id")
    val groupId: Long,
    @Nullable
    @ColumnInfo(name = "class_item_id")
    val classItemId: Long?,
    val name: String,
    val link: String,
    @ColumnInfo(name = "is_new")
    val isNew: Boolean,
    val uuid: String = UUID.randomUUID().toString(),
    val notified: Boolean = false
) {
    companion object {
        fun createFromSagres(groupId: Long, classId: Long?, material: SagresMaterialLink, notified: Boolean = false): ClassMaterial {
            return ClassMaterial(
                groupId = groupId,
                classItemId = classId,
                name = material.name,
                link = material.link,
                isNew = true,
                notified = notified
            )
        }
    }
}
