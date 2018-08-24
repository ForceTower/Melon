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

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.forcetower.sagres.database.model.SCalendar
import java.util.*

@Entity(indices = [
    Index(value = ["uuid"], unique = true)
])
data class CalendarItem(
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    val message: String,
    val date: String,
    val uuid: String = UUID.randomUUID().toString()
) {
    companion object {
        fun fromSagres(item: SCalendar) = CalendarItem(message = item.message, date = item.day)
    }
}