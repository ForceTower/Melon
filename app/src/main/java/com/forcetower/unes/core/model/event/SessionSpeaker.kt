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
import com.google.gson.annotations.SerializedName
import java.util.*

@Entity(foreignKeys = [
    ForeignKey(entity = Session::class, parentColumns = ["uid"], childColumns = ["session_id"], onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE),
    ForeignKey(entity = Speaker::class, parentColumns = ["uid"], childColumns = ["speaker_id"], onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE)
], indices = [
        Index(value = ["session_id", "speaker_id"], unique = true),
        Index(value = ["session_id"]),
        Index(value = ["speaker_id"]),
        Index(value = ["uuid"], unique = true)
])
data class SessionSpeaker(
    @SerializedName(value = "id")
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    @ColumnInfo(name = "session_id")
    val session: Long,
    @ColumnInfo(name = "speaker_id")
    val speaker: Long,
    val uuid: String = UUID.randomUUID().toString()
)