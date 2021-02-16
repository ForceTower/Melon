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

package com.forcetower.uefs.architecture.service.firebase

import com.forcetower.uefs.core.storage.repository.FirebaseMessageRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class FirebaseActionsService : FirebaseMessagingService() {
    @Inject
    lateinit var repository: FirebaseMessageRepository

    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(job + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        Timber.d("Message received: $message")
        coroutineScope.launch {
            repository.onMessageReceived(message)
        }
    }

    override fun onNewToken(token: String) {
        Timber.d("On Token received: $token")
        repository.onNewToken(token)
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("Service onDestroy called. Coroutines will be canceled")
        job.cancel("Service destroyed")
    }
}
