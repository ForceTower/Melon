/*
 * Copyright (c) 2018.
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
                processResults(body)
            } else {
                result.postValue(GradesCallback(Status.RESPONSE_FAILED).code(response.code()).message(response.message()))
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun processResults(body: String) {
        val document = Utils.createDocument(body)
        val codes    = SagresGradesParser.extractSemesterCodes(document)
        val selected = SagresGradesParser.getSelectedSemester(document)

        if (selected == null) {

        } else {
            //successMeasures(document, selected, codes)
        }
    }
}
