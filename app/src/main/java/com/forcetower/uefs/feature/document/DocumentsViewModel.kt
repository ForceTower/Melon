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

package com.forcetower.uefs.feature.document

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.core.lifecycle.Event
import com.forcetower.sagres.Constants
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Document
import com.forcetower.uefs.core.model.unes.SagresDocument
import com.forcetower.uefs.core.storage.repository.DocumentsRepository
import com.forcetower.uefs.core.storage.resource.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DocumentsViewModel @Inject constructor(
    private val repository: DocumentsRepository,
    @ApplicationContext private val context: Context
) : ViewModel(), DocumentActions {
    val documents by lazy { repository.getDocuments() }

    private val _openDocumentAction = MutableLiveData<Event<File>>()
    val openDocumentAction: LiveData<Event<File>>
        get() = _openDocumentAction

    private val _onRequestPendingDownload = MutableLiveData<Event<SagresDocument>>()
    val onRequestDownload: LiveData<Event<SagresDocument>> = _onRequestPendingDownload

    private val _snackMessages = MediatorLiveData<Event<String>>()
    val snackMessages: LiveData<Event<String>>
        get() = _snackMessages

    override fun onOpen(document: SagresDocument) {
        _openDocumentAction.value = Event(repository.getFileFrom(document))
    }

    override fun onDownload(document: SagresDocument, gtoken: String?) {
        val value = getDocumentValue(document)

        if (Constants.getParameter("REQUIRES_CAPTCHA") == "true" && gtoken == null) {
            _onRequestPendingDownload.value = Event(document)
        } else {
            val source = repository.downloadDocument(value, gtoken)
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
