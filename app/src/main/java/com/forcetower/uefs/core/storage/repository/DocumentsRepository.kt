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
            val flow = File(folder, Document.FLOWCHART.value).exists()
            database.documentDao().updateDownloaded(flow, Document.FLOWCHART.value)
            val hist = File(folder, Document.HISTORY.value).exists()
            database.documentDao().updateDownloaded(hist, Document.HISTORY.value)
        }
    }

    fun getDocuments() = database.documentDao().getDocuments()

    @AnyThread
    fun downloadDocument(document: Document): LiveData<Resource<SagresDocument>> {
        val data = MutableLiveData<Resource<SagresDocument>>()
        executor.networkIO().execute { download(data, document) }
        return data
    }

    @WorkerThread
    private fun download(data: MutableLiveData<Resource<SagresDocument>>?, document: Document) {
        val access = database.accessDao().getAccessDirect()
        if (access == null) {
            data?.postValue(Resource.error("Access is null", 700, Exception("No Access")))
        } else {
            database.documentDao().updateDownloading(true, document.value)
            val login = SagresNavigator.instance.login(access.username, access.password)
            if (login.status != Status.SUCCESS) {
                Timber.d("Login failed. Login status is: ${login.status}")
                data?.postValue(Resource.error("Login Failed", 800, Exception("Login Failed")))
            } else {
                val response = when (document) {
                    Document.ENROLLMENT -> SagresNavigator.instance.downloadEnrollment(File(folder, document.value))
                    Document.FLOWCHART -> SagresNavigator.instance.downloadFlowchart(File(folder, document.value))
                    Document.HISTORY -> SagresNavigator.instance.downloadHistory(File(folder, document.value))
                }
                database.documentDao().updateDownloading(false, document.value)
                database.documentDao().updateDownloaded(File(folder, document.value).exists(), document.value)

                Timber.d("Response Document Result: ${response.status}")
                val value = database.documentDao().getDocumentDirect(document.value)
                if (response.status == Status.SUCCESS) {
                    data?.postValue(Resource.success(value))
                } else {
                    data?.postValue(Resource.error(response.message ?: "Generic error", null))
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