/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.forcetower.sagres.database.model

data class SDisciplineClassLocation(
    var startTime: String,
    var endTime: String,
    var day: String,
    var room: String?,
    var campus: String?,
    var modulo: String?,
    var className: String,
    var classCode: String,
    var classGroup: String,
    var fromDisciplineParser: Boolean
) : Comparable<SDisciplineClassLocation> {

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
