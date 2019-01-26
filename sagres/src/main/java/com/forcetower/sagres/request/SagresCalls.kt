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

package com.forcetower.sagres.request

import androidx.annotation.RestrictTo
import com.forcetower.sagres.database.model.SDemandOffer
import com.forcetower.sagres.database.model.SLinker
import com.forcetower.sagres.impl.SagresNavigatorImpl
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
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
        val responsePath = response.request().url().url()
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
    @RestrictTo
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
}
