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
