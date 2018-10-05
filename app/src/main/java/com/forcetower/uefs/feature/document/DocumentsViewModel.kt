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

package com.forcetower.uefs.feature.document

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.model.unes.Document
import com.forcetower.uefs.core.model.unes.SagresDocument
import com.forcetower.uefs.core.storage.repository.DocumentsRepository
import com.forcetower.uefs.core.vm.Event
import javax.inject.Inject

class DocumentsViewModel @Inject constructor(
    private val repository: DocumentsRepository
): ViewModel(), DocumentActions {
    val documents by lazy { repository.getDocuments() }

    private val _openDocumentAction = MutableLiveData<Event<SagresDocument>>()
    val openDocumentAction: LiveData<Event<SagresDocument>>
        get() = _openDocumentAction

    override fun onOpen(document: SagresDocument) {
        _openDocumentAction.value = Event(document)
    }

    override fun onDownload(document: SagresDocument) {
        val value = getDocumentValue(document)
        repository.downloadDocument(value)
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