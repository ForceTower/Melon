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

package com.forcetower.uefs.core.notification

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import androidx.core.content.ContextCompat
import com.forcetower.sagres.utils.WordUtils
import com.forcetower.uefs.R
import com.forcetower.uefs.feature.home.HomeActivity
import com.forcetower.uefs.feature.profile.ProfileActivity
import com.forcetower.uefs.service.NotificationCreator
import com.forcetower.uefs.service.NotificationHelper
import timber.log.Timber

object StatementNotificationProcessor {
    @JvmStatic
    fun openProfileIntent(ctx: Context, userId: Long, profileId: Long): PendingIntent {
        val intent = ProfileActivity.startIntent(ctx, profileId, userId)

        return TaskStackBuilder.create(ctx)
            .addParentStack(HomeActivity::class.java)
            .addNextIntent(intent)
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    @JvmStatic
    fun onStatementReceived(context: Context, data: Map<String, String>) {
        Timber.d("Hum....")
        if (!NotificationCreator.shouldShowNotification("stg_ntf_statement_received", context, true)) return
        val type = data["service_typed"]
        val statement = data["statement"]
        val statementId = data["statement_id"]
        val userId = data["receiver_user_id"]?.toLongOrNull()
        val profileId = data["receiver_profile_id"]?.toLongOrNull()

        Timber.d("Statement Received: $statement, $statementId")
        Timber.d("$profileId, $userId")

        if (statement == null || statementId == null) return

        val senderName = data["sender_name"]

        val title = if (type == "statement_received_hidden") {
            context.getString(R.string.notification_statement_received_anonymous_title)
        } else {
            context.getString(R.string.notification_statement_received_common_title)
        }

        val name = if (type == "statement_received_hidden") {
            senderName
        } else {
            WordUtils.capitalize(senderName)
        }

        val intent = if (userId != null && profileId != null) {
            openProfileIntent(context, userId, profileId)
        } else {
            null
        }

        val body = context.getString(R.string.notification_statement_received_common_body, name)
        val builder = NotificationCreator
            .showDefaultImageNotification(
                context,
                NotificationHelper.CHANNEL_SOCIAL_STATEMENT_RECEIVED_ID,
                title,
                body,
                null
            ).setColor(ContextCompat.getColor(context, R.color.blue_accent))
            .setContentIntent(intent)

        NotificationCreator.showNotification(context, statementId.toLongOrNull() ?: 67312L, builder)
    }
}
