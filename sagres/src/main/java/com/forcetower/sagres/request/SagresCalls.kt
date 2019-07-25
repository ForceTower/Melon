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

package com.forcetower.sagres.request

import androidx.annotation.RestrictTo
import com.forcetower.sagres.database.model.SDemandOffer
import com.forcetower.sagres.database.model.SLinker
import com.forcetower.sagres.impl.SagresNavigatorImpl
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody
import org.jsoup.nodes.Document

@RestrictTo(RestrictTo.Scope.LIBRARY)
object SagresCalls {

    @JvmStatic
    val me: Call
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        get() {
            val request = SagresRequests.me()
            return getCall(request)
        }

    @JvmStatic
    val startPage: Call
        get() {
            val request = SagresRequests.startPage()
            return getCall(request)
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private fun getCall(request: Request): Call {
        val client = SagresNavigatorImpl.instance.client
        return client.newCall(request)
    }

    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun login(username: String, password: String): Call {
        val body = SagresForms.loginBody(username, password)
        val request = SagresRequests.loginRequest(body)
        return getCall(request)
    }

    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun loginApproval(document: Document, response: Response): Call {
        val responsePath = response.request.url.toUrl()
        val url = responsePath.host + responsePath.path
        val body = SagresForms.loginApprovalBody(document)
        val request = SagresRequests.loginApprovalRequest(url, body)
        return getCall(request)
    }

    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getPerson(userId: Long?): Call {
        val request = SagresRequests.getPerson(userId!!)
        return getCall(request)
    }

    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getLink(linker: SLinker): Call {
        val request = SagresRequests.link(linker)
        return getCall(request)
    }

    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getLink(href: String): Call {
        val request = SagresRequests.link(href)
        return getCall(request)
    }

    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getMessages(userId: Long): Call {
        val request = SagresRequests.messages(userId)
        return getCall(request)
    }

    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getSemesters(userId: Long): Call {
        val request = SagresRequests.getSemesters(userId)
        return getCall(request)
    }

    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getGrades(semester: Long?, document: Document?, variant: Long? = null): Call {
        val request = if (semester == null) {
            SagresRequests.getCurrentGrades()
        } else {
            SagresRequests.getGradesForSemester(semester, document!!, variant)
        }

        return getCall(request)
    }

    fun getPageCall(url: String): Call {
        return getCall(SagresRequests.getPageRequest(url))
    }

    fun getDisciplinePageFromInitial(form: FormBody.Builder): Call {
        return getCall(SagresRequests.postAtStudentPage(form))
    }

    fun getDisciplinePageWithParams(params: FormBody.Builder): Call {
        return getCall(SagresRequests.getDisciplinePageWithParams(params))
    }

    fun getDisciplineMaterials(encoded: String, document: Document): Call {
        val body = SagresForms.makeFormBodyForDisciplineMaterials(document, encoded)
        val request = SagresRequests.getDisciplinePageWithParams(body)
        return getCall(request)
    }

    fun getDemandPage(): Call {
        return getCall(SagresRequests.getDemandPage())
    }

    fun createDemand(list: List<SDemandOffer>, document: Document): Call {
        val body = SagresForms.makeFormBodyForDemand(list, document)
        val request = SagresRequests.createDemandWithParams(body)
        return getCall(request)
    }

    fun getRequestedServices(): Call {
        return getCall(SagresRequests.getRequestedServices())
    }

    fun getMessagesPage(): Call {
        return getCall(SagresRequests.getMessagesPage())
    }

    fun getAllDisciplinesPage(): Call {
        return getCall(SagresRequests.getAllDisciplinesPage())
    }

    fun postAllDisciplinesParams(document: Document): Call {
        val body = SagresForms.makeFormBodyForAllDisciplines(document)
        val request = SagresRequests.postAllDisciplinesParams(body)
        return getCall(request)
    }

    fun goToDisciplineAlternate(body: RequestBody): Call {
        val request = SagresRequests.postAllDisciplinesParams(body)
        return getCall(request)
    }
}
