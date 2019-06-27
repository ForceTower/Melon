/*
 * Copyright (c) 2019.
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

package com.forcetower.uefs.feature.document

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Document
import com.forcetower.uefs.core.model.unes.SagresDocument
import com.forcetower.uefs.core.storage.repository.DocumentsRepository
import com.forcetower.uefs.core.storage.resource.Status
import com.forcetower.uefs.core.vm.Event
import java.io.File
import javax.inject.Inject

class DocumentsViewModel @Inject constructor(
    private val repository: DocumentsRepository,
    private val context: Context
) : ViewModel(), DocumentActions {
    val documents by lazy { repository.getDocuments() }

    private val _openDocumentAction = MutableLiveData<Event<File>>()
    val openDocumentAction: LiveData<Event<File>>
        get() = _openDocumentAction

    private val _snackMessages = MediatorLiveData<Event<String>>()
    val snackMessages: LiveData<Event<String>>
        get() = _snackMessages

    override fun onOpen(document: SagresDocument) {
        _openDocumentAction.value = Event(repository.getFileFrom(document))
    }

    override fun onDownload(document: SagresDocument) {
        val value = getDocumentValue(document)
        val source = repository.downloadDocument(value)
        _snackMessages.addSource(source) {
            _snackMessages.removeSource(source)
            if (it.status == Status.ERROR) {
                val resource = when (it.code) {
                    500 -> R.string.failed_to_load_page
                    600 -> R.string.document_not_found
                    700 -> R.string.need_sagres_access
                    800 -> R.string.unable_to_login
                    else -> R.string.unknown_error
                }
                _snackMessages.value = Event(context.getString(resource))
            }
        }
    }

    override fun onDelete(document: SagresDocument) {
        val value = getDocumentValue(document)
        repository.deleteDocument(value)
    }

    private fun getDocumentValue(document: SagresDocument): Document {
        return when (document.type) {
            Document.ENROLLMENT.value -> Document.ENROLLMENT
            Document.FLOWCHART.value -> Document.FLOWCHART
            else -> Document.HISTORY
        }
    }
}