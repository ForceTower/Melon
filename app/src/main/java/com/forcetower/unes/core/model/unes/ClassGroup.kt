/*
 * Copyright (c) 2018.
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

package com.forcetower.unes.core.model.unes

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