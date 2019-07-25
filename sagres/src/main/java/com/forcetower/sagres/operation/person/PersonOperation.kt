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

package com.forcetower.sagres.operation.person

import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.database.model.SPerson
import com.forcetower.sagres.operation.Operation
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.request.SagresCalls
import java.util.concurrent.Executor

class PersonOperation(
    private val userId: Long?,
    executor: Executor?,
    private val cached: Boolean = true
) : Operation<PersonCallback>(executor) {
    init {
        this.perform()
    }

    override fun execute() {
        publishProgress(PersonCallback(Status.STARTED))
        if (cached) {
            val person = SagresNavigator.instance.database.personDao().getPersonDirect(userId.toString())
            if (person != null) {
                successMeasures(person)
                return
            }
        }

        val call = if (userId == null) SagresCalls.me else SagresCalls.getPerson(userId)
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                val body = response.body!!.string()
                val user = gson.fromJson(body, SPerson::class.java)
                successMeasures(user)
            } else {
                publishProgress(PersonCallback(Status.RESPONSE_FAILED).code(response.code).message(response.message))
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            publishProgress(PersonCallback(Status.NETWORK_ERROR).throwable(t))
        }
    }

    private fun successMeasures(user: SPerson) {
        SagresNavigator.instance.database.personDao().insert(user)
        publishProgress(PersonCallback(Status.SUCCESS).person(user))
    }
}
