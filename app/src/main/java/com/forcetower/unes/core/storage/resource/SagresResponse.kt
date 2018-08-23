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