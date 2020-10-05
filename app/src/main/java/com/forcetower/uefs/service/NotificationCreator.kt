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

package com.forcetower.uefs.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.forcetower.uefs.GlideApp
import com.forcetower.uefs.R
import com.forcetower.uefs.core.constants.Constants
import com.forcetower.uefs.core.model.bigtray.BigTrayData
import com.forcetower.uefs.core.model.unes.ClassAbsence
import com.forcetower.uefs.core.model.unes.ClassGroup
import com.forcetower.uefs.core.model.unes.Message
import com.forcetower.uefs.core.model.unes.ServiceRequest
import com.forcetower.uefs.core.storage.database.aggregation.ClassAbsenceWithClass
import com.forcetower.uefs.core.storage.database.aggregation.ClassMaterialWithClass
import com.forcetower.uefs.core.storage.database.aggregation.GradeWithClassStudent
import com.forcetower.uefs.core.util.VersionUtils
import com.forcetower.uefs.feature.disciplines.disciplinedetail.DisciplineDetailsActivity
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

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val institution = preferences.getString(Constants.SELECTED_INSTITUTION_KEY, "UEFS") ?: "UEFS"

        val builder = notificationBuilder(context, channel)
            .setContentTitle(if (message.senderProfile == 3) institution else message.discipline?.toTitleCase() ?: message.senderName.toTitleCase())
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

        val discipline = grade.clazz.discipline.name
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

        val discipline = grade.clazz.discipline.name
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

    fun showAbsenceNotification(absence: ClassAbsence, context: Context, created: Boolean) {
        if (!shouldShowNotification("stg_ntf_absence", context)) {
            return
        }

        val channel = if (created)
            NotificationHelper.CHANNEL_ABSENCE_CREATE_ID
        else
            NotificationHelper.CHANNEL_ABSENCE_REMOVE_ID

        val message = if (created)
            context.getString(R.string.notification_absence_posted, absence.description.toTitleCase())
        else
            context.getString(R.string.notification_absence_deleted, absence.description.toTitleCase())

        val titleRes = if (created)
            R.string.notification_absence_posted_title
        else
            R.string.notification_absence_removed_title

        val builder = notificationBuilder(context, channel)
            .setContentTitle(context.getString(titleRes))
            .setContentText(message)
            .setContentIntent(createOpenIntent(context))
            .setColor(ContextCompat.getColor(context, R.color.dis_07))
            .setStyle(createBigText(message))

        addOptions(context, builder)
        showNotification(context, 1000 + absence.uid, builder)
    }

    fun showSagresDateGradesNotification(grade: GradeWithClassStudent, context: Context) {
        if (!shouldShowNotification("stg_ntf_grade_date", context, false)) {
            return
        }

        val discipline = grade.clazz.discipline.name
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
        val discipline = grade.clazz.discipline.name
        Timber.d("Spoiler level: $spoiler")

        val message = when (spoiler) {
            1 -> {
                val value = grade.grade.gradeDouble()
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
        builder.setContentIntent(createUNESMessagesIntent(context, 1))
        showNotification(context, id, builder)
    }

    fun showAERIMessageNotification(context: Context, id: Long, title: String, description: String, image: String?) {
        val builder = showDefaultImageNotification(context, NotificationHelper.CHANNEL_GENERAL_REMOTE_ID, title, description, image)
        builder.setContentIntent(createUNESMessagesIntent(context, 2))
        showNotification(context, id, builder)
    }

    fun showInvalidAccessNotification(context: Context) {
        val title = context.getString(R.string.access_invalidated_notification_title)
        val message = context.getString(R.string.access_invalidated_notification_message)

        val builder = showDefaultImageNotification(context, NotificationHelper.CHANNEL_GENERAL_WARNINGS_ID, title, message, null)
            .setColor(ContextCompat.getColor(context, R.color.red))
        showNotification(context, message.hashCode().toLong(), builder)
    }

    fun showDefaultImageNotification(context: Context, channel: String, title: String, content: String, image: String?): NotificationCompat.Builder {
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

    fun disciplineDetailsLoadNotification(context: Context): NotificationCompat.Builder {
        return notificationBuilder(context, NotificationHelper.CHANNEL_GENERAL_COMMON_LOW_ID, false)
            .setOngoing(true)
            .setContentTitle(context.getString(R.string.downloading_discipline_details))
            .setPriority(NotificationManagerCompat.IMPORTANCE_LOW)
            .setColor(ContextCompat.getColor(context, R.color.blue_accent))
    }

    fun showDemandOpenNotification(context: Context) {
        val builder = notificationBuilder(context, NotificationHelper.CHANNEL_GENERAL_WARNINGS_ID, true)
            .setContentTitle(context.getString(R.string.demand_open_title))
            .setContentText(context.getString(R.string.demand_open_text))
            .setColor(ContextCompat.getColor(context, R.color.teal))
            .setContentIntent(createDemandIntent(context))

        addOptions(context, builder)
        showNotification(context, 7690, builder)
    }

    fun createCompletedDisciplineLoadNotification(context: Context) {
        val builder = notificationBuilder(context, NotificationHelper.CHANNEL_GENERAL_WARNINGS_ID, true)
            .setContentTitle(context.getString(R.string.discipline_load_all_completed))
            .setContentText(context.getString(R.string.discipline_load_all_completed_info))
            .setColor(ContextCompat.getColor(context, R.color.blue_accent))
        // .setContentIntent(createHourglassIntent(context))

        addOptions(context, builder)
        showNotification(context, 7569, builder)
    }

    fun createFailedWarningNotification(context: Context, title: String, message: String) {
        val builder = notificationBuilder(context, NotificationHelper.CHANNEL_GENERAL_WARNINGS_ID, true)
            .setContentTitle(title)
            .setContentText(message)
            .setColor(ContextCompat.getColor(context, R.color.blue_accent))

        addOptions(context, builder)
        showNotification(context, 7570, builder)
    }

    fun createServiceRequestNotification(context: Context, service: ServiceRequest, update: Boolean) {
        val preference = if (update) "stg_ntf_svc_req_update" else "stg_ntf_svc_req_create"
        if (!shouldShowNotification(preference, context, default = update)) return

        val channel = if (update) NotificationHelper.CHANNEL_SVC_REQ_UPDATE_ID else NotificationHelper.CHANNEL_SVC_REQ_CREATE_ID
        val title = context.getString(R.string.service_requests_ntf_title)
        val message = when (update) {
            true -> {
                context.getString(R.string.service_requests_ntf_update_format, service.service, service.situation)
            }
            false -> {
                context.getString(R.string.service_requests_ntf_create_format, service.service)
            }
        }

        val builder = notificationBuilder(context, channel)
            .setContentTitle(title)
            .setContentText(message)
            .setColor(ContextCompat.getColor(context, R.color.teal))
            .setContentIntent(createDirectionsIntent(context, HomeActivity.EXTRA_REQUEST_SERVICE_DIRECTION))

        addOptions(context, builder)
        showNotification(context, service.uid + message.hashCode(), builder)
    }

    fun showMaterialPostedNotification(context: Context, it: ClassMaterialWithClass) {
        if (!shouldShowNotification("stg_ntf_material_posted", context)) return
        val discipline = it.group.classData.discipline.name
        val title = it.material.name
        val content = context.getString(R.string.material_posted_ntf_content, title, discipline)
        val builder = notificationBuilder(context, NotificationHelper.CHANNEL_DISCIPLINE_MATERIAL_POSTED)
            .setContentTitle(context.getString(R.string.material_posted_ntf_title))
            .setContentText(content)
            .setStyle(createBigText(content))
            .setColor(ContextCompat.getColor(context, R.color.yellow_pr_dark))
            .setContentIntent(createDisciplineDetailsIntent(context, it.group.group))

        addOptions(context, builder)
        showNotification(context, it.material.uid, builder)
    }

    fun showAbsenceNotification(context: Context, data: ClassAbsenceWithClass, created: Boolean) {
        val value = if (created) "stg_ntf_absence_created" else "stg_ntf_absence_removed"
        val contentId = if (created) R.string.absence_created_ntf_content else R.string.absence_removed_ntf_content
        if (!shouldShowNotification(value, context)) return
        val discipline = data.clazz.discipline.name
        val content = context.getString(contentId, discipline)
        val builder = notificationBuilder(context, NotificationHelper.CHANNEL_DISCIPLINE_MATERIAL_POSTED)
            .setContentTitle(context.getString(R.string.material_posted_ntf_title))
            .setContentText(content)
            .setStyle(createBigText(content))
            .setColor(ContextCompat.getColor(context, R.color.yellow_pr_dark))
            .setContentIntent(createOpenIntent(context))

        addOptions(context, builder)
        showNotification(context, data.absence.uid, builder)
    }

    fun createCookieSyncServiceNotification(context: Context, close: PendingIntent): Notification {
        return notificationBuilder(context, NotificationHelper.CHANNEL_GENERAL_SYNC_SERVICE_FOREGROUND, false)
            .setContentTitle(context.getString(R.string.label_service_sync_foreground))
            .setContentText(context.getString(R.string.label_service_sync_foreground_desc))
            // .addAction(R.drawable.ic_close_black_24dp, context.getString(R.string.ru_close_notification), close)
            .setOnlyAlertOnce(true)
            .setContentIntent(createOpenIntent(context))
            .setPriority(NotificationManagerCompat.IMPORTANCE_LOW)
            .setColor(ContextCompat.getColor(context, R.color.blue_accent))
            .build()
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

    fun showNotification(context: Context, id: Long, builder: NotificationCompat.Builder): Boolean {
        val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
        notificationManager?.notify(id.toInt(), builder.build())
        Timber.d("Notification manager ${notificationManager != null}")
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

    private fun createUNESMessagesIntent(ctx: Context, fragmentIndex: Int): PendingIntent {
        val intent = Intent(ctx, HomeActivity::class.java).apply {
            putExtra(HomeActivity.EXTRA_FRAGMENT_DIRECTIONS, HomeActivity.EXTRA_MESSAGES_SAGRES_DIRECTION)
            putExtra(MessagesFragment.EXTRA_MESSAGES_FLAG, fragmentIndex)
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

    private fun createDemandIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx, HomeActivity::class.java).apply {
            putExtra(HomeActivity.EXTRA_FRAGMENT_DIRECTIONS, HomeActivity.EXTRA_DEMAND_DIRECTION)
        }

        return TaskStackBuilder.create(ctx)
            .addParentStack(HomeActivity::class.java)
            .addNextIntent(intent)
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun createDisciplineDetailsIntent(context: Context, classGroup: ClassGroup?): PendingIntent? {
        classGroup ?: return createOpenIntent(context)

        val intent = Intent(context, DisciplineDetailsActivity::class.java).apply {
            putExtra(DisciplineDetailsActivity.CLASS_GROUP_ID, classGroup.uid)
            putExtra(DisciplineDetailsActivity.CLASS_ID, classGroup.classId)
        }

        return TaskStackBuilder.create(context)
            .addParentStack(DisciplineDetailsActivity::class.java)
            .addNextIntent(intent)
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
    }

//    private fun createHourglassIntent(ctx: Context): PendingIntent {
//        val intent = Intent(ctx, HourglassActivity::class.java)
//
//        return TaskStackBuilder.create(ctx)
//                .addParentStack(HourglassActivity::class.java)
//                .addNextIntent(intent)
//                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
//    }

    private fun createOpenIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx, HomeActivity::class.java)

        return TaskStackBuilder.create(ctx)
            .addParentStack(HomeActivity::class.java)
            .addNextIntent(intent)
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun createDirectionsIntent(ctx: Context, direction: String): PendingIntent {
        val intent = Intent(ctx, HomeActivity::class.java).apply {
            putExtra(HomeActivity.EXTRA_FRAGMENT_DIRECTIONS, direction)
        }

        return TaskStackBuilder.create(ctx)
            .addParentStack(HomeActivity::class.java)
            .addNextIntent(intent)
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun shouldShowNotification(value: String, context: Context, default: Boolean = true): Boolean {
        val preference = getPreferences(context)
        val notify = preference.getBoolean(value, default)
        return notify || VersionUtils.isOreo()
    }

    private fun getPreferences(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)
}
