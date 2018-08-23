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

package com.forcetower.unes.core.storage.network.adapter

import com.google.gson.Gson
import retrofit2.Response
import timber.log.Timber

class ApiResponse<T> {
    val code: Int
    val body: T?
    val errorMessage: String?
    val actionError: ActionError?

    val isSuccessful: Boolean
        get() = code in 200..299

    constructor(error: Throwable) {
        code = 500
        body = null
        errorMessage = error.message
        actionError = null
    }

    constructor(response: Response<T>) {
        code = response.code()

        if (response.isSuccessful) {
            body = response.body()
            errorMessage = null
            actionError = null
        } else {
            var message: String? = null
            var aError: ActionError? = null
            if (response.errorBody() != null) {
                try {
                    message = response.errorBody()!!.string()
                    if (message != null) aError = Gson().fromJson(message, ActionError::class.java)
                } catch (e: Exception) {
                    Timber.e(e, "error while parsing response")
                }

            }
            if (message == null || message.trim { it <= ' ' }.isEmpty()) {
                message = response.message()
            }
            actionError = aError
            errorMessage = message
            body = null
        }
    }
}