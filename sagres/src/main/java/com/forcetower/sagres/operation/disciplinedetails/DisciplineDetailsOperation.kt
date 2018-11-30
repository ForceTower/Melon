/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.forcetower.sagres.operation.disciplinedetails

import android.util.Base64
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.Utils.createDocument
import com.forcetower.sagres.database.model.SAccess
import com.forcetower.sagres.database.model.SDisciplineGroup
import com.forcetower.sagres.operation.BaseCallback
import com.forcetower.sagres.operation.Operation
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.parsers.SagresDisciplineDetailsFetcherParser
import com.forcetower.sagres.parsers.SagresDisciplineDetailsParser
import com.forcetower.sagres.parsers.SagresMaterialsParser
import com.forcetower.sagres.request.SagresCalls
import okhttp3.FormBody
import org.json.JSONObject
import org.jsoup.nodes.Document
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.Executor

class DisciplineDetailsOperation(
    private val semester: String?,
    private val code: String?,
    private val group: String?,
    executor: Executor?
) : Operation<DisciplineDetailsCallback>(executor) {

    init {
        this.perform()
    }

    override fun execute() {
        val access = SagresNavigator.instance.database.accessDao().accessDirect
        if (access == null) {
            publishProgress(DisciplineDetailsCallback(Status.INVALID_LOGIN).message("Invalid Access"))
            return
        }

        executeSteps(access)
    }

    private fun executeSteps(access: SAccess) {
        login(access) ?: return
        val initial = initialPage() ?: return
        val forms = SagresDisciplineDetailsFetcherParser.extractFormBodies(initial.document!!, semester, code, group)
        val groups = mutableListOf<SDisciplineGroup>()
        for (form in forms) {
            val document = initialFormConnect(form.first)
            if (document != null) {
                val params = SagresDisciplineDetailsFetcherParser.extractParamsForDiscipline(document)
                val discipline = disciplinePageParams(params)
                if (discipline != null) {
                    val group = processGroup(discipline)
                    if (group != null) {
                        downloadMaterials(discipline, group)
                        groups.add(group)
                    } else {
                        Timber.d("Processed group was null")
                    }
                } else {
                    Timber.d("Discipline from params was null")
                }
            } else {
                Timber.d("Document from initial was null")
            }
        }
        Timber.d("Completed ${forms.size} -- $semester $code $group")
        publishProgress(DisciplineDetailsCallback(Status.COMPLETED).groups(groups))
    }

    private fun login(access: SAccess): BaseCallback<*>? {
        Timber.d("Login")
        val login = SagresNavigator.instance.login(access.username, access.password)
        if (login.status != Status.SUCCESS) {
            publishProgress(DisciplineDetailsCallback.copyFrom(login))
            return null
        }
        return login
    }

    private fun initialPage(): BaseCallback<*>? {
        Timber.d("Initial")
        val initial = SagresNavigator.instance.startPage()
        if (initial.status != Status.SUCCESS) {
            publishProgress(DisciplineDetailsCallback.copyFrom(initial))
            return null
        }
        return initial
    }

    private fun initialFormConnect(form: FormBody.Builder): Document? {
        Timber.d("Going to Discipline Page")
        val call = SagresCalls.getDisciplinePageFromInitial(form)
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                val body = response.body()!!.string()
                return createDocument(body)
            } else {
                publishProgress(DisciplineDetailsCallback(Status.RESPONSE_FAILED).message("Unsuccessful response").code(response.code()))
            }
        } catch (e: IOException) {
            publishProgress(DisciplineDetailsCallback(Status.NETWORK_ERROR).throwable(e).message("Failed at initial form connect"))
        }
        return null
    }

    private fun disciplinePageParams(params: FormBody.Builder): Document? {
        Timber.d("Discipline with Params")
        val call = SagresCalls.getDisciplinePageWithParams(params)
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                val body = response.body()!!.string()
                return createDocument(body)
            } else {
                publishProgress(DisciplineDetailsCallback(Status.RESPONSE_FAILED).message("Unsuccessful response at params").code(response.code()))
            }
        } catch (e: IOException) {
            publishProgress(DisciplineDetailsCallback(Status.NETWORK_ERROR).throwable(e).message("Failed at params setup"))
        }
        return null
    }

    private fun processGroup(document: Document): SDisciplineGroup? {
        Timber.d("Processing group")
        return SagresDisciplineDetailsParser.extractDisciplineGroup(document)
    }

    private fun downloadMaterials(document: Document, group: SDisciplineGroup) {
        Timber.d("Initializing materials download")
        for (item in group.classItems) {
            if (item.numberOfMaterials <= 0) continue
            val json = JSONObject()
            json.put("_realType", true)
            json.put("showForm", true)
            json.put("popupLinkColumn", "cpt_material_apoio")
            json.put("retrieveArguments", item.materialLink)
            val encoded = Base64.encodeToString(json.toString().toByteArray(), Base64.DEFAULT)
            val materials = executeMaterialCall(document, encoded)
            if (materials != null) {
                item.materials = SagresMaterialsParser.extractMaterials(materials)
            }
        }
    }

    private fun executeMaterialCall(document: Document, encoded: String): Document? {
        Timber.d("Executing materials call")
        val call = SagresCalls.getDisciplineMaterials(encoded, document)
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                val body = response.body()!!.string()
                return createDocument(body)
            } else {
                publishProgress(DisciplineDetailsCallback(Status.RESPONSE_FAILED).message("Unsuccessful response at material download"))
            }
        } catch (e: IOException) {
            publishProgress(DisciplineDetailsCallback(Status.NETWORK_ERROR).throwable(e).message("Failed to fetch material"))
        }
        return null
    }
}