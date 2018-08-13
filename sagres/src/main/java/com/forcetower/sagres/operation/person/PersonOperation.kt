package com.forcetower.sagres.operation.person

import com.forcetower.sagres.database.model.Person
import com.forcetower.sagres.operation.BaseCallback
import com.forcetower.sagres.operation.Operation
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.request.SagresCalls

import java.io.IOException
import java.util.concurrent.Executor
import okhttp3.Call
import okhttp3.Response

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
                val user = gson.fromJson(body, Person::class.java)
                successMeasures(user)
            } else {
                result.postValue(PersonCallback(Status.RESPONSE_FAILED).code(response.code()).message(response.message()))
            }
        } catch (e: IOException) {
            e.printStackTrace()
            result.postValue(PersonCallback(Status.NETWORK_ERROR).throwable(e))
        }

    }

    private fun successMeasures(user: Person) {
        result.postValue(PersonCallback(Status.SUCCESS).person(user))
    }
}
