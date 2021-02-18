/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.uefs.core.storage.repository

import android.content.Context
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.operation.Status
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.unes.Document
import com.forcetower.uefs.core.model.unes.SagresDocument
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.resource.Resource
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentsRepository @Inject constructor(
    private val database: UDatabase,
    private val executor: AppExecutors,
    context: Context
) {
    private val folder: File = File(context.getExternalFilesDir(null), "documents")

    init {
        Timber.d("Folder Path: ${folder.absolutePath}")
        executor.diskIO().execute {
            folder.mkdirs()
            if (database.documentDao().getDocumentsDirect().isEmpty()) {
                database.documentDao().insert(SagresDocument.enrollment())
                database.documentDao().insert(SagresDocument.flowchart())
                database.documentDao().insert(SagresDocument.history())
            }

            val enroll = File(folder, Document.ENROLLMENT.value).exists()
            database.documentDao().updateDownloaded(enroll, Document.ENROLLMENT.value)
            database.documentDao().updateDownloading(false, Document.ENROLLMENT.value)
            val flow = File(folder, Document.FLOWCHART.value).exists()
            database.documentDao().updateDownloaded(flow, Document.FLOWCHART.value)
            database.documentDao().updateDownloading(false, Document.FLOWCHART.value)
            val hist = File(folder, Document.HISTORY.value).exists()
            database.documentDao().updateDownloaded(hist, Document.HISTORY.value)
            database.documentDao().updateDownloading(false, Document.HISTORY.value)
        }
    }

    fun getDocuments() = database.documentDao().getDocuments()

    @AnyThread
    fun downloadDocument(document: Document, gtoken: String?): LiveData<Resource<SagresDocument>> {
        val data = MutableLiveData<Resource<SagresDocument>>()
        executor.networkIO().execute { download(data, document, gtoken) }
        return data
    }

    @WorkerThread
    private fun download(data: MutableLiveData<Resource<SagresDocument>>, document: Document, gtoken: String?) {
        val access = database.accessDao().getAccessDirect()
        if (access == null) {
            data.postValue(Resource.error("Access is null", 700, Exception("No Access")))
        } else {
            database.documentDao().updateDownloading(true, document.value)
            val login = SagresNavigator.instance.login(access.username, access.password, gtoken)

            if (login.status == Status.INVALID_LOGIN) {
                Timber.d("Login failed. Login status is: ${login.status}")
                database.documentDao().updateDownloading(false, document.value)
                data.postValue(Resource.error("Login Failed", 800, Exception("Login Failed")))
            } else {
                val response = when (document) {
                    Document.ENROLLMENT -> SagresNavigator.instance.downloadEnrollment(File(folder, document.value))
                    Document.FLOWCHART -> SagresNavigator.instance.downloadFlowchart(File(folder, document.value))
                    Document.HISTORY -> SagresNavigator.instance.downloadHistory(File(folder, document.value))
                }
                Timber.d("Response message ${response.message}")
                Timber.d(response.throwable, "Hum...")

                database.documentDao().run {
                    updateDownloading(false, document.value)
                    updateDownloaded(File(folder, document.value).exists(), document.value)
                    if (response.status == Status.SUCCESS) {
                        updateDate(System.currentTimeMillis(), document.value)
                    }
                }

                Timber.d("Response Document Result: ${response.status}")
                val value = database.documentDao().getDocumentDirect(document.value)
                if (response.status == Status.SUCCESS) {
                    data.postValue(Resource.success(value))
                } else {
                    database.documentDao().updateDownloading(false, document.value)
                    data.postValue(Resource.error(response.message ?: "Generic error", response.code, null))
                }
            }
        }
    }

    fun deleteDocument(document: Document) {
        executor.diskIO().execute {
            val delete = File(folder, document.value).delete()
            if (delete) {
                database.documentDao().updateDownloaded(false, document.value)
                Timber.d("File deleted")
            } else {
                Timber.d("File not deleted...")
            }
        }
    }

    fun getFileFrom(document: SagresDocument) = File(folder, document.type)
}
