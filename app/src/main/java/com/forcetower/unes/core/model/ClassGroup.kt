/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.forcetower.unes.core.model

import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import com.forcetower.sagres.database.model.SDisciplineGroup
import java.util.*

@Entity(foreignKeys = [
    ForeignKey(entity = Class::class, parentColumns = ["uid"], childColumns = ["class_id"], onDelete = CASCADE, onUpdate = CASCADE)
], indices = [
    Index(value = ["class_id", "group"], unique = true),
    Index(value = ["uuid"], unique = true)
])
data class ClassGroup(
    @PrimaryKey(autoGenerate = true)
    var uid: Long = 0,
    @ColumnInfo(name = "class_id")
    val classId: Long,
    var group: String,
    var teacher: String? = null,
    var credits: Int = 0,
    val uuid: String = UUID.randomUUID().toString(),
    var draft: Boolean = true,
    var ignored: Boolean = false
) {

    fun selectiveCopy(grp: SDisciplineGroup) {
        if (!grp.group.isNullOrBlank()) group = grp.group
        if (!grp.teacher.isNullOrBlank()) teacher = grp.teacher
        if (grp.credits > 0) credits = grp.credits
    }

    override fun toString(): String {
        return "${classId}_$group draft: $draft"
    }
}