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

package com.forcetower.uefs.core.model.unes

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(foreignKeys = [
    ForeignKey(entity = ClassGroup::class, parentColumns = ["uid"], childColumns = ["group_id"], onDelete = CASCADE, onUpdate = CASCADE),
    ForeignKey(entity = Profile::class, parentColumns = ["uid"], childColumns = ["profile_id"], onDelete = CASCADE, onUpdate = CASCADE)
], indices = [
    Index(value = ["group_id"]),
    Index(value = ["profile_id", "group_id"], unique = true),
    Index(value = ["uuid"], unique = true)
])
data class ClassStudent(
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    @ColumnInfo(name = "profile_id")
    val profileId: Long,
    @ColumnInfo(name = "group_id")
    val groupId: Long,
    @ColumnInfo(name = "final_score")
    var finalScore: Double? = null,
    val uuid: String = UUID.randomUUID().toString()
)