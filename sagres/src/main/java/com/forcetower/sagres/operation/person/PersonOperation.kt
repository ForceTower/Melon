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

package com.forcetower.sagres.operation.person

import com.forcetower.sagres.database.model.SPerson
import com.forcetower.sagres.operation.Operation
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.request.SagresCalls

import java.io.IOException
import java.util.concurrent.Executor

class PersonOperation(private val userId: Long?, executor: Executor?) : Operation<PersonCallback>(executor) {
    init {
        this.perform()
    }

    override fun execute() {
        result.postValue(PersonCallback(Status.STARTED))
        val call = if (userId == null) SagresCalls.me else SagresCalls.getPerson(userId)
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                val body = response.body()!!.string()
                val user = gson.fromJson(body, SPerson::class.java)
                successMeasures(user)
            } else {
                result.postValue(PersonCallback(Status.RESPONSE_FAILED).code(response.code()).message(response.message()))
            }
        } catch (e: IOException) {
            e.printStackTrace()
            result.postValue(PersonCallback(Status.NETWORK_ERROR).throwable(e))
        }

    }

    private fun successMeasures(user: SPerson) {
        result.postValue(PersonCallback(Status.SUCCESS).person(user))
    }
}
