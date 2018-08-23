/*
 * Copyright (c) 2018.
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
import java.util.*

@Entity(indices = [
    Index(value = ["code"], unique = true),
    Index(value = ["uuid"], unique = true),
    Index(value = ["name"], unique = true)
])
data class Discipline(
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    val name: String,
    val code: String,
    val credits: Int,
    val department: String? = null,
    val uuid: String = UUID.randomUUID().toString()
) {

    companion object {
        fun fromSagres(discipline: com.forcetower.sagres.database.model.Discipline)
                = Discipline(name = discipline.name, code = discipline.code, credits = discipline.credits)
    }
}