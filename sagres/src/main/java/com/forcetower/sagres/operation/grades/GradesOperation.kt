package com.forcetower.sagres.operation.grades

import com.forcetower.sagres.operation.Operation
import com.forcetower.sagres.request.SagresCalls
import org.jsoup.nodes.Document
import java.util.concurrent.Executor

class GradesOperation(private val semester: Long?, private val document: Document?, executor: Executor): Operation<GradesCallback>(executor) {
    init {
        this.perform()
    }

    override fun execute() {
        val call = SagresCalls.getGrades(semester, document)
    }
}
