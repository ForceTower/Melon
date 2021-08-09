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

package com.forcetower.uefs.feature.messages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import androidx.core.view.drawToBitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forcetower.core.lifecycle.Event
import com.forcetower.uefs.BuildConfig
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.service.UMessage
import com.forcetower.uefs.core.model.unes.Message
import com.forcetower.uefs.core.storage.repository.MessagesRepository
import com.forcetower.uefs.core.task.usecase.message.FetchAllMessagesSnowpiercerUseCase
import com.forcetower.uefs.feature.shared.extensions.toFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val repository: MessagesRepository,
    @Named("flagSnowpiercerEnabled") private val snowpiercerEnabled: Boolean,
    private val fetchAllMessagesSnowpiercerUseCase: FetchAllMessagesSnowpiercerUseCase
) : ViewModel(), MessagesActions {
    val messages by lazy { repository.getMessages() }
    val unesMessages by lazy { repository.getUnesMessages() }
    var pushedTimes = 0

    private val _refreshing = MediatorLiveData<Boolean>()
    val refreshing: LiveData<Boolean>
        get() = _refreshing

    private val _messageClick = MutableLiveData<Event<String>>()
    val messageClick: LiveData<Event<String>>
        get() = _messageClick

    private val _portalMessageClick = MutableLiveData<Event<Message>>()
    val portalMessageClick: LiveData<Event<Message>>
        get() = _portalMessageClick

    private val _snackMessage = MutableLiveData<Event<Int>>()
    val snackMessage: LiveData<Event<Int>>
        get() = _snackMessage

    fun onRefresh() {
        pushedTimes++

        if (pushedTimes == 3 && snowpiercerEnabled) {
            _snackMessage.value = Event(R.string.download_all_messages)
            viewModelScope.launch {
                _refreshing.value = true
                fetchAllMessagesSnowpiercerUseCase(Unit)
                _refreshing.value = false
                pushedTimes = 0
            }
        } else {
            val fetchMessages = repository.fetchMessages(pushedTimes == 3)
            _refreshing.value = true
            _refreshing.addSource(fetchMessages) {
                _refreshing.removeSource(fetchMessages)
                _refreshing.value = false
            }
        }
    }

    override fun onMessageClick(message: String?) {
        message ?: return
        _messageClick.value = Event(message)
    }

    override fun onPortalMessageClick(message: Message?) {
        message ?: return
        onMessageClick(message.content)
    }

    override fun onUNESMessageLongClick(view: View, message: UMessage?): Boolean {
        message ?: return false
        val context = view.context
        val content = "Mensagem UNES\n\n${message.message}"
        return shareMessage(context, content)
    }

    override fun onMessageLongClick(view: View, message: Message?): Boolean {
        message ?: return false
        val context = view.context
        val content = "${message.content}\n\nEnviada por ${message.senderName}"
        return shareMessage(context, content)
    }

    override fun onMessageShare(view: View, pos: Int) {
        val context = view.context
        val file = view.drawToBitmap().toFile(context)

        val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.type = "image/jpg"
        context.startActivity(intent)
    }

    private fun shareMessage(context: Context, content: String): Boolean {
        val clipboard: ClipboardManager? = context.getSystemService()
        clipboard ?: return false
        clipboard.setPrimaryClip(ClipData.newPlainText("unes-message", content))
        _snackMessage.value = Event(R.string.message_copied_to_clipboard)
        return true
    }
}
