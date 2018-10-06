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

import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.preference.PreferenceManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Message
import com.forcetower.uefs.core.storage.database.accessors.GradeWithClassStudent
import com.forcetower.uefs.core.util.VersionUtils

object NotificationCreator {

    fun showSagresMessageNotification(message: Message, context: Context) {
        if (!shouldShowNotification("show_message_notification", context)) {
            return
        }

        //val pendingIntent = getPendingIntent(context, LoggedActivity::class.java, MESSAGES_FRAGMENT_SAGRES)
        val builder = notificationBuilder(context, NotificationHelper.CHANNEL_MESSAGES_SAGRES_ID)
                .setContentTitle(if (message.senderProfile == 3) "UEFS" else message.senderName)
                .setContentText(message.content)
                .setStyle(createBigText(message.content))
                //.setContentIntent(pendingIntent)
                .setColor(ContextCompat.getColor(context, R.color.blue_accent))

        addOptions(context, builder)
        showNotification(context, message.uid, builder)
    }

    fun showSagresCreateGradesNotification(grade: GradeWithClassStudent, context: Context) {
        if (!shouldShowNotification("show_create_grades_notification", context)) {
            return
        }

        val discipline = grade.clazz().group().singleClass().singleDiscipline().name
        val message = context.getString(R.string.notification_grade_created, grade.grade.name, discipline)
        val builder = notificationBuilder(context, NotificationHelper.CHANNEL_GRADES_CREATED_ID)
            .setContentTitle(context.getString(R.string.notification_grade_created_title))
            .setContentText(message)
            .setColor(ContextCompat.getColor(context, R.color.yellow_pr))
            .setStyle(createBigText(message))

        addOptions(context, builder)
        showNotification(context, grade.grade.uid, builder)
    }

    fun showSagresChangeGradesNotification(grade: GradeWithClassStudent, context: Context) {
        if (!shouldShowNotification("show_change_grades_notification", context)) {
            return
        }

        val discipline = grade.clazz().group().singleClass().singleDiscipline().name
        val message = context.getString(R.string.notification_grade_changed, grade.grade.name, discipline)
        val builder = notificationBuilder(context, NotificationHelper.CHANNEL_GRADES_CREATED_ID)
            .setContentTitle(context.getString(R.string.notification_grade_changed_title))
            .setContentText(message)
            .setColor(ContextCompat.getColor(context, R.color.yellow_pr))
            .setStyle(createBigText(message))

        addOptions(context, builder)
        showNotification(context, grade.grade.uid, builder)
    }

    fun showSagresDateGradesNotification(grade: GradeWithClassStudent, context: Context) {
        if (!shouldShowNotification("show_date_grades_notification", context)) {
            return
        }

        val discipline = grade.clazz().group().singleClass().singleDiscipline().name
        val message = context.getString(R.string.notification_grade_date_change, grade.grade.name, discipline)
        val builder = notificationBuilder(context, NotificationHelper.CHANNEL_GRADES_CREATED_ID)
            .setContentTitle(context.getString(R.string.notification_grade_date_change_title))
            .setContentText(message)
            .setColor(ContextCompat.getColor(context, R.color.yellow_pr))
            .setStyle(createBigText(message))

        addOptions(context, builder)
        showNotification(context, grade.grade.uid, builder)
    }

    fun showSagresPostedGradesNotification(grade: GradeWithClassStudent, context: Context) {
        if (!shouldShowNotification("show_posted_grades_notification", context)) {
            return
        }

        val spoiler = getPreferences(context).getInt("notification_grade_spoiler_level", 0)
        val discipline = grade.clazz().group().singleClass().singleDiscipline().name
        val message = when (spoiler) {
            1 -> {
                val value = grade.grade.grade.trim().toDoubleOrNull()
                if (value == null) context.getString(R.string.notification_grade_posted_message_lv_0, grade.grade.name, discipline)
                else when (value) {
                    in 0.0..6.9 -> context.getString(R.string.notification_grade_posted_message_lv_1_bad, grade.grade.name, discipline)
                    in 7.0..7.9 -> context.getString(R.string.notification_grade_posted_message_lv_1_pass, grade.grade.name, discipline)
                    in 8.0..9.9 -> context.getString(R.string.notification_grade_posted_message_lv_1_good, grade.grade.name, discipline)
                    10.0 -> context.getString(R.string.notification_grade_posted_message_lv_1_perfect, grade.grade.name, discipline)
                    else -> context.getString(R.string.notification_grade_posted_message_lv_0, grade.grade.name, discipline)
                }
            }
            2 -> context.getString(R.string.notification_grade_posted_message_lv_2, grade.grade.grade, discipline)
            else -> context.getString(R.string.notification_grade_posted_message_lv_0, grade.grade.name, discipline)
        }

        val builder = notificationBuilder(context, NotificationHelper.CHANNEL_GRADES_POSTED_ID)
            .setContentTitle(context.getString(R.string.notification_grade_posted_title))
            .setContentText(message)
            .setColor(ContextCompat.getColor(context, R.color.yellow_pr))
            .setStyle(createBigText(message))

        addOptions(context, builder)
        showNotification(context, grade.grade.uid, builder)
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

    private fun shouldShowNotification(value: String, context: Context): Boolean {
        val preference = getPreferences(context)
        val notify = preference.getBoolean(value, true)
        return notify || VersionUtils.isOreo()
    }

    private fun getPreferences(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)
}