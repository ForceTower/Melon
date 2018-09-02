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

package com.forcetower.unes.core.model.event

import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import com.google.gson.annotations.SerializedName
import java.util.*

@Entity(foreignKeys = [
    ForeignKey(entity = Session::class, parentColumns = ["uid"], childColumns = ["session_id"], onDelete = CASCADE, onUpdate = CASCADE),
    ForeignKey(entity = Tag::class, parentColumns = ["uid"], childColumns = ["tag_id"], onUpdate = CASCADE, onDelete = CASCADE)
], indices = [
    Index(value = ["session_id", "tag_id"], unique = true),
    Index(value = ["session_id"]),
    Index(value = ["tag_id"]),
    Index(value = ["uuid"], unique = true)
])
data class SessionTag(
    @SerializedName(value = "id")
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    @ColumnInfo(name = "session_id")
    val session: Long,
    @ColumnInfo(name = "tag_id")
    val tag: Long,
    val uuid: String = UUID.randomUUID().toString()
)