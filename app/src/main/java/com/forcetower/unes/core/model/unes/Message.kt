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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.forcetower.sagres.database.model.SMessage
import java.util.*

@Entity(indices = [
    Index(value = ["sagres_id"], unique = true),
    Index(value = ["uuid"], unique = true)
])
data class Message(
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    val content: String,
    @ColumnInfo(name = "sagres_id")
    val sagresId: Long,
    val timestamp: Long,
    @ColumnInfo(name = "sender_profile")
    val senderProfile: Int,
    @ColumnInfo(name = "sender_name")
    val senderName: String,
    val notified: Boolean = false,
    val uuid: String = UUID.randomUUID().toString()
) {

    companion object {
        fun fromMessage(me: SMessage) = Message(content = me.message, sagresId = me.sagresId, senderName = me.senderName, senderProfile = me.senderProfile, timestamp = me.timeStampInMillis)
    }
}