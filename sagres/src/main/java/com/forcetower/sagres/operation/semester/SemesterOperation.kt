package com.forcetower.sagres.operation.semester

import com.forcetower.sagres.database.model.Semester
import com.forcetower.sagres.operation.Dumb
import com.forcetower.sagres.operation.Operation
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.request.SagresCalls
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.util.*
import java.util.concurrent.Executor

class SemesterOperation(private val userId: Long, executor: Executor): Operation<SemesterCallback>(executor) {
    init {
        this.perform()
    }

    override fun execute() {
        val call = SagresCalls.getSemesters(userId)
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                val body = response.body()!!.string()
                successMeasures(body)
            } else {
                result.postValue(SemesterCallback(Status.NETWORK_ERROR).code(response.code()).message(response.message()))
            }
        } catch (e: IOException) {
            e.printStackTrace()
            result.postValue(SemesterCallback(Status.NETWORK_ERROR).throwable(e))
        }
    }

    private fun successMeasures(body: String) {
        val type = object: TypeToken<Dumb<MutableList<Semester>>>(){}.type
        val dSemesters = gson.fromJson<Dumb<MutableList<Semester>>>(body, type)
        val semesters = dSemesters.items

        val callback = SemesterCallback(Status.SUCCESS).semesters(semesters)
        this.finished = callback
        this.success = true

        result.postValue(callback)
    }
}
