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
import java.util.*

@Entity(foreignKeys = [
    ForeignKey(entity = ClassStudent::class, parentColumns = ["uid"], childColumns = ["class_id"], onUpdate = CASCADE, onDelete = CASCADE)
], indices = [
    Index(value = ["name", "class_id"], unique = true),
    Index(value = ["uuid"], unique = true)
])
data class Grade(
    @PrimaryKey(autoGenerate = true)
    var uid: Long = 0,
    @ColumnInfo(name = "class_id")
    val classId: Long,
    val name: String,
    var date: String,
    var grade: String,
    var notified: Int = 0,
    val uuid: String = UUID.randomUUID().toString()
) {
    fun hasGrade(): Boolean {
        return (!grade.trim { it <= ' ' }.isEmpty()
                && !grade.trim { it <= ' ' }.equals("Não Divulgada", ignoreCase = true)
                && !grade.trim { it <= ' ' }.equals("-", ignoreCase = true)
                && !grade.trim { it <= ' ' }.equals("--", ignoreCase = true)
                && !grade.trim { it <= ' ' }.equals("*", ignoreCase = true)
                && !grade.trim { it <= ' ' }.equals("**", ignoreCase = true)
                && !grade.trim { it <= ' ' }.equals("-1", ignoreCase = true))
    }

    override fun toString(): String = "${name}_${grade}_${date}_$notified"
}