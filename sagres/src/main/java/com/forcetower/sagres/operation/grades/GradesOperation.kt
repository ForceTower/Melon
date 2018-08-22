package com.forcetower.sagres.operation.grades

import com.forcetower.sagres.Utils
import com.forcetower.sagres.operation.Operation
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.parsers.SagresGradesParser
import com.forcetower.sagres.request.SagresCalls
import org.jsoup.nodes.Document
import java.io.IOException
import java.util.concurrent.Executor

class GradesOperation(private val semester: Long?, private val document: Document?, executor: Executor): Operation<GradesCallback>(executor) {
    init {
        this.perform()
    }

    override fun execute() {
        val call = SagresCalls.getGrades(semester, document)
        try {
            result.postValue(GradesCallback(Status.LOADING))
            val response = call.execute()
            if (response.isSuccessful) {
                val body = response.body()!!.string()
                successMeasures(body)
            } else {
                result.postValue(GradesCallback(Status.RESPONSE_FAILED).code(response.code()).message(response.message()))
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun successMeasures(body: String) {
        val document = Utils.createDocument(body)
        val selected = SagresGradesParser.getSelectedSemester(document)
    }
}
