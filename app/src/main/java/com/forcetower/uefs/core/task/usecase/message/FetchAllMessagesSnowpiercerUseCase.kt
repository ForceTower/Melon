/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2021. João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.uefs.core.task.usecase.message

import com.forcetower.uefs.core.model.unes.Message
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.task.UseCase
import dev.forcetower.breaker.Orchestra
import dev.forcetower.breaker.model.Authorization
import dev.forcetower.breaker.result.Outcome
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class FetchAllMessagesSnowpiercerUseCase @Inject constructor(
    private val database: UDatabase,
    private val client: OkHttpClient,
    @Named("webViewUA") private val agent: String
) : UseCase<Unit, Unit>(Dispatchers.IO) {

    override suspend fun execute(parameters: Unit) {
        val access = database.accessDao().getAccessDirect()
        val profile = database.profileDao().selectMeDirect()
        access ?: return
        profile ?: return

        val orchestra = Orchestra.Builder().client(client).userAgent(agent).build()
        orchestra.setAuthorization(Authorization(access.username, access.password))

        var until: String? = ""

        do {
            val page = orchestra.messages(profile.sagresId, until ?: "")
            until = if (page is Outcome.Success) {
                val value = page.value
                database.messageDao()
                    .insertIgnoring(
                        value.messages.map {
                            Message.fromMessage(it, true)
                        }
                    )
                Timber.d("Value Next: ${value.next}")
                value.next
            } else {
                null
            }
        } while (until != null)
    }
}
