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
    ForeignKey(entity = ClassStudent::class, parentColumns = ["uid"], childColumns = ["class_id"], onUpdate = CASCADE, onDelete = CASCADE)
], indices = [
    Index(value = ["class_id"]),
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