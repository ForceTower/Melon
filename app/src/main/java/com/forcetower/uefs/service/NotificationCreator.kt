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

package com.forcetower.uefs.service

import com.forcetower.uefs.core.util.VersionUtils
import android.preference.PreferenceManager
import timber.log.Timber
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Message


object NotificationCreator {

    fun showSagresMessageNotification(message: Message, context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val notify = preferences.getBoolean("show_message_notification", true)
        if (!notify && !VersionUtils.isOreo()) {
            Timber.d("Skipped messages due to preferences")
        }

        //TODO Show "UEFS" or teacher name
        //val pendingIntent = getPendingIntent(context, LoggedActivity::class.java, MESSAGES_FRAGMENT_SAGRES)
        val builder = notificationBuilder(context, NotificationHelper.CHANNEL_MESSAGES_SAGRES_ID)
                .setContentTitle(message.senderName)
                .setContentText(message.content)
                .setStyle(createBigText(message.content))
                //.setContentIntent(pendingIntent)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))

        addOptions(context, builder)
        showNotification(context, message.uid, builder)
    }




    private fun notificationBuilder(context: Context, groupId: String): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(context, groupId)
        builder.setAutoCancel(true)
        builder.setSmallIcon(R.drawable.ic_unes_colored)
        return builder
    }

    private fun createBigText(message: String): NotificationCompat.BigTextStyle {
        return NotificationCompat.BigTextStyle().bigText(message)
    }

    private fun showNotification(context: Context, id: Long, builder: NotificationCompat.Builder): Boolean {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(id.toInt(), builder.build())
        return true
    }


    private fun addOptions(context: Context, builder: NotificationCompat.Builder) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        if (!VersionUtils.isOreo()) {
            if (preferences.getBoolean("notifications_new_message_vibrate", true)) {
                builder.setVibrate(longArrayOf(150, 300, 150, 300))
            }

            val ringtone = Uri.parse(preferences.getString("notifications_new_message_ringtone", "content://settings/system/notification_sound"))
            builder.setSound(ringtone)
        }
    }
}