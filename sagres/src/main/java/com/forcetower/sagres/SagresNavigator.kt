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

package com.forcetower.sagres

import android.content.Context

import com.forcetower.sagres.database.SagresDatabase
import com.forcetower.sagres.impl.SagresNavigatorImpl
import com.forcetower.sagres.operation.calendar.CalendarCallback
import com.forcetower.sagres.operation.login.LoginCallback
import com.forcetower.sagres.operation.messages.MessagesCallback
import com.forcetower.sagres.operation.person.PersonCallback
import com.forcetower.sagres.operation.start_page.StartPageCallback
import com.forcetower.sagres.operation.semester.SemesterCallback

import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import com.forcetower.sagres.operation.grades.GradesCallback

abstract class SagresNavigator {
    abstract val database: SagresDatabase

    @AnyThread
    abstract fun aLogin(username: String, password: String): LiveData<LoginCallback>

    @WorkerThread
    abstract fun login(username: String, password: String): SagresNavigator?

    @AnyThread
    abstract fun aMe(): LiveData<PersonCallback>

    @WorkerThread
    abstract fun me(): SagresNavigator?

    @AnyThread
    abstract fun aMessages(userId: Long): LiveData<MessagesCallback>

    @AnyThread
    abstract fun aCalendar(): LiveData<CalendarCallback>

    @AnyThread
    abstract fun aSemesters(userId: Long): LiveData<SemesterCallback>

    @AnyThread
    abstract fun startPage(): LiveData<StartPageCallback>

    @AnyThread
    abstract fun getCurrentGrades(): LiveData<GradesCallback>

    abstract fun stopTags(tag: String)

    companion object {
        val instance: SagresNavigator
            get() = SagresNavigatorImpl.instance

        fun initialize(context: Context) {
            SagresNavigatorImpl.initialize(context)
        }
    }
}
