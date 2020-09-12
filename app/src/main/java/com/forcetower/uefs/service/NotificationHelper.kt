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

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.core.content.ContextCompat
import com.forcetower.uefs.R
import com.forcetower.uefs.core.util.VersionUtils

class NotificationHelper(val context: Context) : ContextWrapper(context) {

    fun createChannels() {
        if (!VersionUtils.isOreo()) return

        val cGrades = NotificationChannelGroup(CHANNEL_GROUP_GRADES_ID, getString(R.string.channel_group_grades))
        val cAbsences = NotificationChannelGroup(CHANNEL_GROUP_ABSENCE_ID, getString(R.string.channel_group_absences))
        val cMessages = NotificationChannelGroup(CHANNEL_GROUP_MESSAGES_ID, getString(R.string.channel_group_messages))
        val cGeneral = NotificationChannelGroup(CHANNEL_GROUP_GENERAL_ID, getString(R.string.channel_group_general))
        val cEvents = NotificationChannelGroup(CHANNEL_GROUP_EVENTS_ID, getString(R.string.channel_group_events))
        val cServices = NotificationChannelGroup(CHANNEL_GROUP_SERVICE_REQUEST_ID, getString(R.string.channel_group_service_request))
        val cDisciplines = NotificationChannelGroup(CHANNEL_GROUP_DISCIPLINE_ID, context.getString(R.string.channel_group_disciplines))
        val cSocial = NotificationChannelGroup(CHANNEL_GROUP_SOCIAL_ID, context.getString(R.string.channel_group_social_network))

        val manager = getManager()
        manager.createNotificationChannelGroups(listOf(cGrades, cMessages, cGeneral, cEvents, cServices, cDisciplines, cSocial, cAbsences))

        val messages = createChannel(CHANNEL_MESSAGES_TEACHER_ID, getString(R.string.channel_messages_teachers), NotificationManager.IMPORTANCE_DEFAULT)
        val uefsMsg = createChannel(CHANNEL_MESSAGES_UEFS_ID, getString(R.string.channel_messages_uefs), NotificationManager.IMPORTANCE_DEFAULT)
        val posted = createChannel(CHANNEL_GRADES_POSTED_ID, getString(R.string.channel_grades_posted), NotificationManager.IMPORTANCE_DEFAULT)
        val dateChanged = createChannel(CHANNEL_GRADES_DATE_CHANGED_ID, getString(R.string.channel_grades_date_changed), NotificationManager.IMPORTANCE_DEFAULT)
        val valueChanged = createChannel(CHANNEL_GRADES_VALUE_CHANGED_ID, getString(R.string.channel_grades_value_changed), NotificationManager.IMPORTANCE_DEFAULT)
        val created = createChannel(CHANNEL_GRADES_CREATED_ID, getString(R.string.channel_grades_created), NotificationManager.IMPORTANCE_DEFAULT)
        val absenceCreate = createChannel(CHANNEL_ABSENCE_CREATE_ID, getString(R.string.channel_absence_created), NotificationManager.IMPORTANCE_DEFAULT)
        val absenceRemove = createChannel(CHANNEL_ABSENCE_REMOVE_ID, getString(R.string.channel_absence_removed), NotificationManager.IMPORTANCE_DEFAULT)
        val warnings = createChannel(CHANNEL_GENERAL_WARNINGS_ID, getString(R.string.warnings), NotificationManager.IMPORTANCE_DEFAULT)
        val remote = createChannel(CHANNEL_GENERAL_REMOTE_ID, getString(R.string.remote), NotificationManager.IMPORTANCE_DEFAULT)
        val eventGen = createChannel(CHANNEL_EVENTS_GENERAL_ID, getString(R.string.channel_events_general), NotificationManager.IMPORTANCE_DEFAULT)
        val bigTray = createChannel(CHANNEL_GENERAL_BIGTRAY_ID, getString(R.string.channel_big_tray_quota), NotificationManager.IMPORTANCE_LOW)
        val serviceFrg = createChannel(CHANNEL_GENERAL_SYNC_SERVICE_FOREGROUND, getString(R.string.channel_service_sync_foreground), NotificationManager.IMPORTANCE_LOW)
        val commonLow = createChannel(CHANNEL_GENERAL_COMMON_LOW_ID, getString(R.string.channel_common_low), NotificationManager.IMPORTANCE_LOW)
        val commonDef = createChannel(CHANNEL_GENERAL_COMMON_HIG_ID, getString(R.string.channel_common_hig), NotificationManager.IMPORTANCE_HIGH)
        val svcCreate = createChannel(CHANNEL_SVC_REQ_CREATE_ID, getString(R.string.channel_svc_req_create), NotificationManager.IMPORTANCE_LOW)
        val svcUpdate = createChannel(CHANNEL_SVC_REQ_UPDATE_ID, getString(R.string.channel_svc_req_update), NotificationManager.IMPORTANCE_DEFAULT)
        val materialPost = createChannel(CHANNEL_DISCIPLINE_MATERIAL_POSTED, context.getString(R.string.channel_discipline_material_posted), NotificationManager.IMPORTANCE_DEFAULT)
        val socialStatements = createChannel(CHANNEL_SOCIAL_STATEMENT_RECEIVED_ID, context.getString(R.string.channel_social_statement_received), NotificationManager.IMPORTANCE_DEFAULT)

        messages.group = CHANNEL_GROUP_MESSAGES_ID
        uefsMsg.group = CHANNEL_GROUP_MESSAGES_ID
        posted.group = CHANNEL_GROUP_GRADES_ID
        dateChanged.group = CHANNEL_GROUP_GRADES_ID
        valueChanged.group = CHANNEL_GROUP_GRADES_ID
        created.group = CHANNEL_GROUP_GRADES_ID
        warnings.group = CHANNEL_GROUP_GENERAL_ID
        remote.group = CHANNEL_GROUP_GENERAL_ID
        eventGen.group = CHANNEL_GROUP_EVENTS_ID
        bigTray.group = CHANNEL_GROUP_GENERAL_ID
        commonLow.group = CHANNEL_GROUP_GENERAL_ID
        commonDef.group = CHANNEL_GROUP_GENERAL_ID
        serviceFrg.group = CHANNEL_GROUP_GENERAL_ID
        svcCreate.group = CHANNEL_GROUP_SERVICE_REQUEST_ID
        svcUpdate.group = CHANNEL_GROUP_SERVICE_REQUEST_ID
        materialPost.group = CHANNEL_GROUP_DISCIPLINE_ID
        socialStatements.group = CHANNEL_GROUP_SOCIAL_ID
        absenceCreate.group = CHANNEL_GROUP_ABSENCE_ID
        absenceRemove.group = CHANNEL_GROUP_ABSENCE_ID

        manager.createNotificationChannel(messages)
        manager.createNotificationChannel(uefsMsg)
        manager.createNotificationChannel(posted)
        manager.createNotificationChannel(dateChanged)
        manager.createNotificationChannel(valueChanged)
        manager.createNotificationChannel(created)
        manager.createNotificationChannel(warnings)
        manager.createNotificationChannel(remote)
        manager.createNotificationChannel(eventGen)
        manager.createNotificationChannel(bigTray)
        manager.createNotificationChannel(serviceFrg)
        manager.createNotificationChannel(commonLow)
        manager.createNotificationChannel(commonDef)
        manager.createNotificationChannel(svcCreate)
        manager.createNotificationChannel(svcUpdate)
        manager.createNotificationChannel(materialPost)
        manager.createNotificationChannel(socialStatements)
        manager.createNotificationChannel(absenceCreate)
        manager.createNotificationChannel(absenceRemove)

        manager.deleteNotificationChannel(CHANNEL_MESSAGES_DCE_ID)
        manager.deleteNotificationChannel(CHANNEL_MESSAGES_SAGRES_ID)
        manager.deleteNotificationChannel(CHANNEL_GRADES_CHANGED_ID)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createChannel(
        channelId: String,
        name: CharSequence,
        importance: Int,
        vibration: LongArray = longArrayOf(150, 300, 150, 300),
        showBadge: Boolean = true,
        enableLights: Boolean = true
    ): NotificationChannel {
        val channel = NotificationChannel(channelId, name, importance)
        channel.enableLights(enableLights)
        channel.setShowBadge(showBadge)
        channel.vibrationPattern = vibration
        return channel
    }

    private fun getManager() = ContextCompat.getSystemService(this, NotificationManager::class.java)!!

    companion object {
        // Notification Groups
        const val CHANNEL_GROUP_MESSAGES_ID = "com.forcetower.uefs.MESSAGES"
        const val CHANNEL_GROUP_GRADES_ID = "com.forcetower.uefs.GRADES"
        const val CHANNEL_GROUP_ABSENCE_ID = "com.forcetower.uefs.GRADES"
        const val CHANNEL_GROUP_GENERAL_ID = "com.forcetower.uefs.GENERAL"
        const val CHANNEL_GROUP_EVENTS_ID = "com.forcetower.uefs.EVENTS"
        const val CHANNEL_GROUP_SERVICE_REQUEST_ID = "com.forcetower.uefs.SERVICE_REQUEST"
        const val CHANNEL_GROUP_DISCIPLINE_ID = "com.forcetower.uefs.DISCIPLINE"
        const val CHANNEL_GROUP_SOCIAL_ID = "com.forcetower.uefs.SOCIAL"
        // Notification Channels
        const val CHANNEL_MESSAGES_TEACHER_ID = "com.forcetower.uefs.MESSAGES.SAGRES.TEACHER.POST"
        const val CHANNEL_MESSAGES_UEFS_ID = "com.forcetower.uefs.MESSAGES.SAGRES.UEFS.POST"
        const val CHANNEL_GRADES_POSTED_ID = "com.forcetower.uefs.GRADES.POSTED"
        const val CHANNEL_GRADES_CREATED_ID = "com.forcetower.uefs.GRADES.CREATE"
        const val CHANNEL_GRADES_DATE_CHANGED_ID = "com.forcetower.uefs.GRADES.DATE_CHANGE"
        const val CHANNEL_GRADES_VALUE_CHANGED_ID = "com.forcetower.uefs.GRADES.VALUE_CHANGED"
        const val CHANNEL_ABSENCE_CREATE_ID = "com.forcetower.uefs.ABSENCE.CREATED"
        const val CHANNEL_ABSENCE_REMOVE_ID = "com.forcetower.uefs.ABSENCE.REMOVED"
        const val CHANNEL_GENERAL_WARNINGS_ID = "com.forcetower.uefs.GENERAL.WARNINGS"
        const val CHANNEL_GENERAL_COMMON_LOW_ID = "com.forcetower.uefs.GENERAL.COMMON.LOW"
        const val CHANNEL_GENERAL_COMMON_HIG_ID = "com.forcetower.uefs.GENERAL.COMMON.HIGH"
        const val CHANNEL_GENERAL_REMOTE_ID = "com.forcetower.uefs.GENERAL.REMOTE"
        const val CHANNEL_GENERAL_BIGTRAY_ID = "com.forcetower.uefs.GENERAL.BIGTRAY"
        const val CHANNEL_GENERAL_SYNC_SERVICE_FOREGROUND = "com.forcetower.uefs.SYNC.FOREGROUND"
        const val CHANNEL_EVENTS_GENERAL_ID = "com.forcetower.uefs.EVENTS.GENERAL"
        const val CHANNEL_SVC_REQ_CREATE_ID = "com.forcetower.uefs.SERVICE_REQUEST.CREATE"
        const val CHANNEL_SVC_REQ_UPDATE_ID = "com.forcetower.uefs.SERVICE_REQUEST.UPDATE"
        const val CHANNEL_DISCIPLINE_MATERIAL_POSTED = "com.forcetower.uefs.DISCIPLINE.MATERIAL.POSTED"
        const val CHANNEL_SOCIAL_STATEMENT_RECEIVED_ID = "com.forcetower.uefs.SOCIAL.STATEMENT.RECEIVED"

        // Deleted Channels
        const val CHANNEL_MESSAGES_DCE_ID = "com.forcetower.uefs.MESSAGES.DCE"
        const val CHANNEL_MESSAGES_SAGRES_ID = "com.forcetower.uefs.MESSAGES.SAGRES.POST"
        const val CHANNEL_GRADES_CHANGED_ID = "com.forcetower.uefs.GRADES.CHANGE"
    }
}
