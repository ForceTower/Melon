/*
 * Copyright (c) 2019.
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
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import com.forcetower.sagres.database.SagresDatabase
import com.forcetower.sagres.database.model.SDemandOffer
import com.forcetower.sagres.impl.SagresNavigatorImpl
import com.forcetower.sagres.operation.calendar.CalendarCallback
import com.forcetower.sagres.operation.demand.DemandCreatorCallback
import com.forcetower.sagres.operation.demand.DemandOffersCallback
import com.forcetower.sagres.operation.disciplinedetails.DisciplineDetailsCallback
import com.forcetower.sagres.operation.disciplines.FastDisciplinesCallback
import com.forcetower.sagres.operation.document.DocumentCallback
import com.forcetower.sagres.operation.grades.GradesCallback
import com.forcetower.sagres.operation.login.LoginCallback
import com.forcetower.sagres.operation.messages.MessagesCallback
import com.forcetower.sagres.operation.person.PersonCallback
import com.forcetower.sagres.operation.semester.SemesterCallback
import com.forcetower.sagres.operation.servicerequest.RequestedServicesCallback
import com.forcetower.sagres.operation.start_page.StartPageCallback
import org.jsoup.nodes.Document
import java.io.File

abstract class SagresNavigator {
    abstract val database: SagresDatabase

    @AnyThread
    abstract fun aLogin(username: String, password: String): LiveData<LoginCallback>

    @WorkerThread
    abstract fun login(username: String, password: String): LoginCallback

    @AnyThread
    abstract fun aMe(): LiveData<PersonCallback>

    @WorkerThread
    abstract fun me(): PersonCallback

    @AnyThread
    abstract fun aMessages(userId: Long, fetchAll: Boolean = false): LiveData<MessagesCallback>

    @WorkerThread
    abstract fun messages(userId: Long, fetchAll: Boolean = false): MessagesCallback

    @AnyThread
    abstract fun aMessagesHtml(needsAuth: Boolean = false): LiveData<MessagesCallback>

    @WorkerThread
    abstract fun messagesHtml(needsAuth: Boolean = false): MessagesCallback

    @AnyThread
    abstract fun aCalendar(): LiveData<CalendarCallback>

    @WorkerThread
    abstract fun calendar(): CalendarCallback

    @AnyThread
    abstract fun aSemesters(userId: Long): LiveData<SemesterCallback>

    @WorkerThread
    abstract fun semesters(userId: Long): SemesterCallback

    @AnyThread
    abstract fun aStartPage(): LiveData<StartPageCallback>

    @WorkerThread
    abstract fun startPage(): StartPageCallback

    @AnyThread
    abstract fun aGetCurrentGrades(): LiveData<GradesCallback>

    @WorkerThread
    abstract fun getCurrentGrades(): GradesCallback

    @WorkerThread
    abstract fun getGradesFromSemester(semesterSagresId: Long, document: Document): GradesCallback

    @WorkerThread
    abstract fun downloadEnrollment(file: File): DocumentCallback

    @WorkerThread
    abstract fun downloadFlowchart(file: File): DocumentCallback

    @WorkerThread
    abstract fun downloadHistory(file: File): DocumentCallback

    @WorkerThread
    abstract fun loadDisciplineDetails(semester: String?, code: String?, group: String?, partialLoad: Boolean = false): DisciplineDetailsCallback

    @AnyThread
    abstract fun aLoadDisciplineDetails(semester: String?, code: String?, group: String?, partialLoad: Boolean = false): LiveData<DisciplineDetailsCallback>

    @AnyThread
    abstract fun aLoadDemandOffers(): LiveData<DemandOffersCallback>

    @WorkerThread
    abstract fun loadDemandOffers(): DemandOffersCallback

    @WorkerThread
    abstract fun createDemandOffer(offers: List<SDemandOffer>): DemandCreatorCallback

    @AnyThread
    abstract fun aGetRequestedServices(login: Boolean = false): LiveData<RequestedServicesCallback>

    @WorkerThread
    abstract fun getRequestedServices(login: Boolean = false): RequestedServicesCallback

    @AnyThread
    abstract fun aDisciplinesExperimental(semester: String? = null, code: String? = null, group: String? = null, partialLoad: Boolean = false, discover: Boolean = true): LiveData<FastDisciplinesCallback>

    @WorkerThread
    abstract fun disciplinesExperimental(semester: String? = null, code: String? = null, group: String? = null, partialLoad: Boolean = false, discover: Boolean = true): FastDisciplinesCallback

    @AnyThread
    abstract fun stopTags(tag: String?)

    @AnyThread
    abstract fun aLogout()

    @WorkerThread
    abstract fun logout()

    @AnyThread
    abstract fun getSelectedInstitution(): String

    @AnyThread
    abstract fun setSelectedInstitution(institution: String)
    companion object {
        val instance: SagresNavigator
            get() = SagresNavigatorImpl.instance

        @AnyThread
        fun initialize(context: Context) {
            SagresNavigatorImpl.initialize(context)
        }

        @AnyThread
        fun getSupportedInstitutions() = Constants.SUPPORTED_INSTITUTIONS
    }
}
