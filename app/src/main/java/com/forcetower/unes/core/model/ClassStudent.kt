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
    ForeignKey(entity = ClassGroup::class, parentColumns = ["uid"], childColumns = ["group_id"], onDelete = CASCADE, onUpdate = CASCADE),
    ForeignKey(entity = Profile::class, parentColumns = ["uid"], childColumns = ["profile_id"], onDelete = CASCADE, onUpdate = CASCADE)
], indices = [
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
    val uuid: String = UUID.randomUUID().toString()
)