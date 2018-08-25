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

package com.forcetower.sagres.database.model

class SDisciplineClassLocation(
    var startTime: String,
    var endTime: String,
    var day: String,
    var room: String?,
    var campus: String?,
    var modulo: String?,
    var className: String,
    var classCode: String,
    var classGroup: String)
: Comparable<SDisciplineClassLocation> {

    override fun compareTo(other: SDisciplineClassLocation): Int {
        return startTime.compareTo(other.startTime)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val that = other as SDisciplineClassLocation?
        if (startTime != that!!.startTime)
            return false
        if (endTime != that.endTime) return false
        if (day != that.day) return false
        if (room != that.room) return false
        if (campus != that.campus) return false
        return modulo == that.modulo
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + startTime.hashCode()
        result = 31 * result + endTime.hashCode()
        result = 31 * result + day.hashCode()
        result = 31 * result + if (room != null) room!!.hashCode() else 0
        result = 31 * result + if (campus != null) campus!!.hashCode() else 0
        result = 31 * result + if (modulo != null) modulo!!.hashCode() else 0
        return result
    }
}
