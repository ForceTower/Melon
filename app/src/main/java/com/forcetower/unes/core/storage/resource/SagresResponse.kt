package com.forcetower.unes.core.storage.resource

import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class SagresResponse {
    private val response: Response?
    val code: Int
    val document: Document?
    val message: String?
    val throwable: Throwable?

    constructor(response: Response) {
        this.response = response
        this.code = response.code()
        if (response.isSuccessful) {
            this.document = Jsoup.parse(response.body()?.string())
            this.message = null
        } else {
            this.document = null
            this.message = response.body()?.string()
        }
        this.throwable = null
    }

    constructor(throwable: Throwable) {
        this.throwable = throwable
        this.response = null
        this.code = 500
        this.document = null
        this.message = throwable.message
    }

    fun isSuccessful(): Boolean = code in 200..299
}