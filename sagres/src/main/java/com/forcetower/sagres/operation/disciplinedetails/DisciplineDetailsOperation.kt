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

package com.forcetower.sagres.operation.disciplinedetails

import android.util.Base64
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.Utils.createDocument
import com.forcetower.sagres.database.model.SAccess
import com.forcetower.sagres.database.model.SDisciplineGroup
import com.forcetower.sagres.operation.BaseCallback
import com.forcetower.sagres.operation.Operation
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.operation.disciplinedetails.DisciplineDetailsCallback.Companion.DOWNLOADING
import com.forcetower.sagres.operation.disciplinedetails.DisciplineDetailsCallback.Companion.INITIAL
import com.forcetower.sagres.operation.disciplinedetails.DisciplineDetailsCallback.Companion.LOGIN
import com.forcetower.sagres.operation.disciplinedetails.DisciplineDetailsCallback.Companion.PROCESSING
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
    private val partialLoad: Boolean,
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
        publishProgress(DisciplineDetailsCallback(Status.LOADING).flags(LOGIN))
        login(access) ?: return
        publishProgress(DisciplineDetailsCallback(Status.LOADING).flags(INITIAL))
        val initial = initialPage() ?: return
        publishProgress(DisciplineDetailsCallback(Status.LOADING).flags(PROCESSING))
        val forms = SagresDisciplineDetailsFetcherParser.extractFormBodies(initial.document!!, semester, code, group)
        val groups = mutableListOf<SDisciplineGroup>()

        var failureCount = 0

        val total = forms.size
        for ((index, form) in forms.withIndex()) {
            publishProgress(DisciplineDetailsCallback(Status.LOADING).flags(DOWNLOADING).current(index).total(total))
            val document = initialFormConnect(form.first)
            if (document != null) {
                val params = SagresDisciplineDetailsFetcherParser.extractParamsForDiscipline(document)
                val discipline = if (partialLoad) document else disciplinePageParams(params)
                if (discipline != null) {
                    val group = processGroup(discipline)
                    if (group != null) {
                        if (!partialLoad) downloadMaterials(discipline, group)
                        groups.add(group)
                    } else {
                        failureCount++
                        Timber.d("Processed group was null")
                    }
                } else {
                    failureCount++
                    Timber.d("Discipline from params was null")
                }
            } else {
                failureCount++
                Timber.d("Document from initial was null")
            }
        }
        Timber.d("Completed ${forms.size} -- $semester $code $group")
        publishProgress(DisciplineDetailsCallback(Status.COMPLETED).groups(groups).failureCount(failureCount))
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
                val body = response.body!!.string()
                return createDocument(body)
            } else {
                publishProgress(DisciplineDetailsCallback(Status.RESPONSE_FAILED).message("Unsuccessful response").code(response.code))
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
                val body = response.body!!.string()
                return createDocument(body)
            } else {
                publishProgress(DisciplineDetailsCallback(Status.RESPONSE_FAILED).message("Unsuccessful response at params").code(response.code))
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
                val body = response.body!!.string()
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