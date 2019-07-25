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

package com.forcetower.sagres.operation.disciplinedetails

import com.forcetower.sagres.database.model.SDisciplineGroup
import com.forcetower.sagres.operation.BaseCallback
import com.forcetower.sagres.operation.Status

class DisciplineDetailsCallback(status: Status) : BaseCallback<DisciplineDetailsCallback>(status) {
    private var groups: List<SDisciplineGroup>? = null
    private var flags: Int = 0
    private var current: Int = 0
    private var total: Int = 0
    private var failureCount: Int = 0

    fun getFlags() = flags
    fun getGroups() = groups
    fun getCurrent() = current
    fun getTotal() = total
    fun getFailureCount() = failureCount

    fun groups(groups: List<SDisciplineGroup>?): DisciplineDetailsCallback {
        this.groups = groups
        return this
    }

    fun flags(flags: Int = 0): DisciplineDetailsCallback {
        this.flags = flags
        return this
    }

    fun current(current: Int): DisciplineDetailsCallback {
        this.current = current
        return this
    }

    fun total(total: Int): DisciplineDetailsCallback {
        this.total = total
        return this
    }

    fun failureCount(failureCount: Int): DisciplineDetailsCallback {
        this.failureCount = failureCount
        return this
    }

    companion object {
        fun copyFrom(callback: BaseCallback<*>): DisciplineDetailsCallback {
            return DisciplineDetailsCallback(callback.status).message(callback.message).code(callback.code).throwable(
                callback.throwable).document(callback.document)
        }

        const val LOGIN = 1
        const val INITIAL = 2
        const val PROCESSING = 4
        const val DOWNLOADING = 8
        const val SAVING = 16
        const val GRADES = 32
    }
}