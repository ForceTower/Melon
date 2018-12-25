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

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.preference.PreferenceManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.forcetower.uefs.GlideApp
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.bigtray.BigTrayData
import com.forcetower.uefs.core.model.unes.Message
import com.forcetower.uefs.core.storage.database.accessors.GradeWithClassStudent
import com.forcetower.uefs.core.util.VersionUtils
import com.forcetower.uefs.feature.home.HomeActivity
import com.forcetower.uefs.feature.messages.MessagesFragment
import com.forcetower.uefs.feature.shared.extensions.toTitleCase
import timber.log.Timber

object NotificationCreator {

    fun showSagresMessageNotification(message: Message, context: Context, uid: Long = message.uid) {
        val settingsKey: String
        val channel: String
        if (message.senderProfile == 3) {
            settingsKey = "stg_ntf_message_uefs"
            channel = NotificationHelper.CHANNEL_MESSAGES_UEFS_ID
        } else {
            settingsKey = "stg_ntf_message_teacher"
            channel = NotificationHelper.CHANNEL_MESSAGES_TEACHER_ID
        }

        if (!shouldShowNotification(settingsKey, context)) {
            return
        }

        val builder = notificationBuilder(context, channel)
            .setContentTitle(if (message.senderProfile == 3) "UEFS" else message.discipline?.toTitleCase() ?: message.senderName.toTitleCase())
            .setContentText(message.content)
            .setStyle(createBigText(message.content))
            .setContentIntent(createMessagesIntent(context))
            .setColor(ContextCompat.getColor(context, R.color.blue_accent))

        addOptions(context, builder)
        showNotification(context, uid, builder)
    }

    fun showSagresCreateGradesNotification(grade: GradeWithClassStudent, context: Context) {
        if (!shouldShowNotification("stg_ntf_grade_create", context, false)) {
            return
        }

        val discipline = grade.clazz().clazz.singleDiscipline().name
        val message = context.getString(R.string.notification_grade_created, grade.grade.name, discipline)
        val builder = notificationBuilder(context, NotificationHelper.CHANNEL_GRADES_CREATED_ID)
            .setContentTitle(context.getString(R.string.notification_grade_created_title))
            .setContentText(message)
            .setContentIntent(createGradesIntent(context))
            .setColor(ContextCompat.getColor(context, R.color.yellow_pr))
            .setStyle(createBigText(message))

        addOptions(context, builder)
        showNotification(context, grade.grade.uid, builder)
    }

    fun showSagresChangeGradesNotification(grade: GradeWithClassStudent, context: Context) {
        if (!shouldShowNotification("stg_ntf_grade_change", context)) {
            return
        }

        val discipline = grade.clazz().clazz.singleDiscipline().name
        val message = context.getString(R.string.notification_grade_changed, grade.grade.name, discipline)
        val builder = notificationBuilder(context, NotificationHelper.CHANNEL_GRADES_VALUE_CHANGED_ID)
            .setContentTitle(context.getString(R.string.notification_grade_changed_title))
            .setContentText(message)
            .setContentIntent(createGradesIntent(context))
            .setColor(ContextCompat.getColor(context, R.color.yellow_pr))
            .setStyle(createBigText(message))

        addOptions(context, builder)
        showNotification(context, grade.grade.uid, builder)
    }

    fun showSagresDateGradesNotification(grade: GradeWithClassStudent, context: Context) {
        if (!shouldShowNotification("stg_ntf_grade_date", context, false)) {
            return
        }

        val discipline = grade.clazz().clazz.singleDiscipline().name
        val message = context.getString(R.string.notification_grade_date_change, grade.grade.name, discipline)
        val builder = notificationBuilder(context, NotificationHelper.CHANNEL_GRADES_CREATED_ID)
            .setContentTitle(context.getString(R.string.notification_grade_date_change_title))
            .setContentText(message)
            .setContentIntent(createGradesIntent(context))
            .setColor(ContextCompat.getColor(context, R.color.yellow_pr))
            .setStyle(createBigText(message))

        addOptions(context, builder)
        showNotification(context, grade.grade.uid, builder)
    }

    fun showSagresPostedGradesNotification(grade: GradeWithClassStudent, context: Context) {
        if (!shouldShowNotification("stg_ntf_grade_posted", context)) {
            return
        }

        val spoiler = getPreferences(context).getString("stg_ntf_grade_spoiler", "1")?.toIntOrNull() ?: 1
        val discipline = grade.clazz().clazz.singleDiscipline().name
        Timber.d("Spoiler level: $spoiler")

        val message = when (spoiler) {
            1 -> {
                val value = grade.grade.grade.trim()
                    .replace(",", ".")
                    .replace("-", "")
                    .replace("*", "")
                    .toDoubleOrNull()
                Timber.d("Level 1 spoiler value: $value")
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
            .setContentIntent(createGradesIntent(context))
            .setColor(ContextCompat.getColor(context, R.color.yellow_pr))
            .setStyle(createBigText(message))

        addOptions(context, builder)
        showNotification(context, grade.grade.uid, builder)
    }

    fun showUpgradeNotification(title: String, content: String, context: Context) {
        val builder = notificationBuilder(context, NotificationHelper.CHANNEL_GENERAL_WARNINGS_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(createOpenIntent(context))
            .setColor(ContextCompat.getColor(context, R.color.colorAccent))
            .setStyle(createBigText(content))

        addOptions(context, builder)
        showNotification(context, content.hashCode().toLong(), builder)
    }

    fun showEventNotification(context: Context, id: String, title: String, description: String, image: String?) {
        if (!shouldShowNotification("stg_ntf_event_created", context)) return
        val builder = showDefaultImageNotification(context, NotificationHelper.CHANNEL_EVENTS_GENERAL_ID, title, description, image)
        showNotification(context, id.hashCode().toLong(), builder)
    }

    fun showServiceMessageNotification(context: Context, id: Long, title: String, description: String, image: String?) {
        val builder = showDefaultImageNotification(context, NotificationHelper.CHANNEL_GENERAL_REMOTE_ID, title, description, image)
        builder.setContentIntent(createUNESMessagesIntent(context))
        showNotification(context, id, builder)
    }

    private fun showDefaultImageNotification(context: Context, channel: String, title: String, content: String, image: String?): NotificationCompat.Builder {
        var style = createBigText(content)
        if (image != null && image != "null") {
            val other = createBigImage(context, image)
            if (other != null) style = other
        }

        val builder = notificationBuilder(context, channel)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(createOpenIntent(context))
            .setColor(ContextCompat.getColor(context, R.color.blue_accent))
            .setStyle(style)

        addOptions(context, builder)
        return builder
    }

    fun showSimpleNotification(context: Context, title: String, content: String) {
        if (!shouldShowNotification("show_notification_remote", context)) return
        val builder = showDefaultImageNotification(context, NotificationHelper.CHANNEL_GENERAL_REMOTE_ID, title, content, null)
        showNotification(context, content.hashCode().toLong(), builder)
    }

    fun showBigTrayNotification(context: Context, data: BigTrayData?, close: PendingIntent): Notification {
        val builder = notificationBuilder(context, NotificationHelper.CHANNEL_GENERAL_BIGTRAY_ID, false)
            .setOngoing(true)
            .setContentTitle(context.getString(R.string.label_big_tray))
            .setPriority(NotificationManagerCompat.IMPORTANCE_LOW)
            .setColor(ContextCompat.getColor(context, R.color.blue_accent))
            .setContentIntent(createBigTrayIntent(context))
            .addAction(R.drawable.ic_close_black_24dp, context.getString(R.string.ru_close_notification), close)

        val content = when {
            data == null -> context.getString(R.string.ru_loading_data)
            data.open -> {
                val number = data.quota.toIntOrNull()
                if (number == null || number == 0) {
                    context.getString(R.string.ru_quota_exceeded)
                } else {
                    context.getString(R.string.ru_quota_remaining, data.quota)
                }
            }
            !data.open && !data.error -> context.getString(R.string.ru_is_closed_message)
            else -> context.getString(R.string.ru_failed_load_data)
        }

        builder.setContentText(content)
        return builder.build()
    }

    private fun notificationBuilder(context: Context, groupId: String, autoCancel: Boolean = true): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(context, groupId)
        builder.setAutoCancel(autoCancel)
        builder.setSmallIcon(R.drawable.ic_unes_colored)
        return builder
    }

    private fun createBigText(message: String): NotificationCompat.Style {
        return NotificationCompat.BigTextStyle().bigText(message)
    }

    private fun createBigImage(context: Context, image: String): NotificationCompat.Style? {
        return try {
            val bitmap = GlideApp.with(context).asBitmap().load(image).submit().get()
            NotificationCompat.BigPictureStyle().bigPicture(bitmap)
        } catch (t: Throwable) {
            Timber.d("Error happened at image load: ${t.message}")
            null
        }
    }

    private fun showNotification(context: Context, id: Long, builder: NotificationCompat.Builder): Boolean {
        val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
        notificationManager?.notify(id.toInt(), builder.build())
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

    private fun createGradesIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx, HomeActivity::class.java).apply {
            putExtra(HomeActivity.EXTRA_FRAGMENT_DIRECTIONS, HomeActivity.EXTRA_GRADES_DIRECTION)
        }

        return TaskStackBuilder.create(ctx)
            .addParentStack(HomeActivity::class.java)
            .addNextIntent(intent)
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun createMessagesIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx, HomeActivity::class.java).apply {
            putExtra(HomeActivity.EXTRA_FRAGMENT_DIRECTIONS, HomeActivity.EXTRA_MESSAGES_SAGRES_DIRECTION)
        }

        return TaskStackBuilder.create(ctx)
            .addParentStack(HomeActivity::class.java)
            .addNextIntent(intent)
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun createUNESMessagesIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx, HomeActivity::class.java).apply {
            putExtra(HomeActivity.EXTRA_FRAGMENT_DIRECTIONS, HomeActivity.EXTRA_MESSAGES_SAGRES_DIRECTION)
            putExtra(MessagesFragment.EXTRA_MESSAGES_FLAG, true)
        }

        return TaskStackBuilder.create(ctx)
            .addParentStack(HomeActivity::class.java)
            .addNextIntent(intent)
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun createBigTrayIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx, HomeActivity::class.java).apply {
            putExtra(HomeActivity.EXTRA_FRAGMENT_DIRECTIONS, HomeActivity.EXTRA_BIGTRAY_DIRECTION)
        }

        return TaskStackBuilder.create(ctx)
                .addParentStack(HomeActivity::class.java)
                .addNextIntent(intent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun createOpenIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx, HomeActivity::class.java)

        return TaskStackBuilder.create(ctx)
            .addParentStack(HomeActivity::class.java)
            .addNextIntent(intent)
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun shouldShowNotification(value: String, context: Context, default: Boolean = true): Boolean {
        val preference = getPreferences(context)
        val notify = preference.getBoolean(value, default)
        return notify || VersionUtils.isOreo()
    }

    private fun getPreferences(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)
}