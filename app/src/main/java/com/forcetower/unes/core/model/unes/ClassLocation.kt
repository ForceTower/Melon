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
import java.util.*

@Entity(foreignKeys = [
    ForeignKey(entity = ClassGroup::class, parentColumns = ["uid"], childColumns = ["group_id"], onUpdate = CASCADE, onDelete = CASCADE),
    ForeignKey(entity = Profile::class, parentColumns = ["uid"], childColumns = ["profile_id"], onUpdate = CASCADE, onDelete = CASCADE)
], indices = [
    Index(value = ["group_id", "day", "starts_at", "ends_at", "profile_id"], unique = true),
    Index(value = ["profile_id"]),
    Index(value = ["uuid"], unique = true)
])
data class ClassLocation (
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    @ColumnInfo(name = "group_id")
    val groupId: Long,
    @ColumnInfo(name = "profile_id")
    val profileId: Long,
    @ColumnInfo(name = "starts_at")
    val startsAt: String,
    @ColumnInfo(name = "ends_at")
    val endsAt: String,
    val day: String,
    val room: String?,
    val modulo: String?,
    val campus: String?,
    val uuid: String = UUID.randomUUID().toString()
): Comparable<ClassLocation> {

    override fun toString(): String {
        return "${groupId}_$profileId: $day >> $startsAt .. $endsAt"
    }

    override fun compareTo(other: ClassLocation): Int {
        return startsAt.compareTo(other.startsAt)
    }
}