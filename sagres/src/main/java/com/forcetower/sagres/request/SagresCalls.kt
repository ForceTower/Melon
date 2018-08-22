package com.forcetower.sagres.request

import com.forcetower.sagres.database.model.Linker
import com.forcetower.sagres.impl.SagresNavigatorImpl

import org.jsoup.nodes.Document

import java.net.URL
import androidx.annotation.RestrictTo
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response

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
    fun getLink(linker: Linker): Call {
        val request = SagresRequests.link(linker)
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
    fun getGrades(semester: Long?, document: Document?): Call {
        val request = if (semester == null) {
            SagresRequests.getCurrentGrades()
        } else {
            SagresRequests.getGradesForSemester(semester, document!!)
        }

        return getCall(request)
    }
}
