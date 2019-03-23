/*
 * Copyright (c) 2019.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
import com.forcetower.sagres.database.model.SMaterialLink
import java.util.UUID

@Entity(foreignKeys = [
    ForeignKey(entity = ClassGroup::class, parentColumns = ["uid"], childColumns = ["group_id"], onDelete = CASCADE, onUpdate = CASCADE),
    ForeignKey(entity = ClassItem::class, parentColumns = ["uid"], childColumns = ["class_item_id"], onDelete = SET_NULL, onUpdate = CASCADE)
], indices = [
    Index(value = ["name"], unique = false),
    Index(value = ["link"], unique = false),
    Index(value = ["group_id"], unique = false),
    Index(value = ["class_item_id"], unique = false),
    Index(value = ["uuid"], unique = true),
    Index(value = ["is_new"], unique = false),
    Index(value = ["name", "link", "group_id"], unique = true)
])
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
        fun createFromSagres(groupId: Long, classId: Long?, material: SMaterialLink): ClassMaterial {
            return ClassMaterial(
                groupId = groupId,
                classItemId = classId,
                name = material.name,
                link = material.link,
                isNew = true
            )
        }
    }
}