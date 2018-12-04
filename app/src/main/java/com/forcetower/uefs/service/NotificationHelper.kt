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

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import com.forcetower.uefs.R
import com.forcetower.uefs.core.util.VersionUtils

class NotificationHelper(val context: Context) : ContextWrapper(context) {

    fun createChannels() {
        if (!VersionUtils.isOreo()) return

        val cGrades = NotificationChannelGroup(CHANNEL_GROUP_GRADES_ID, getString(R.string.channel_group_grades))
        val cMessages = NotificationChannelGroup(CHANNEL_GROUP_MESSAGES_ID, getString(R.string.channel_group_messages))
        val cGeneral = NotificationChannelGroup(CHANNEL_GROUP_GENERAL_ID, getString(R.string.channel_group_general))
        val cEvents = NotificationChannelGroup(CHANNEL_GROUP_EVENTS_ID, getString(R.string.channel_group_events))

        val manager = getManager()
        manager.createNotificationChannelGroups(listOf(cGrades, cMessages, cGeneral, cEvents))

        val messages = createChannel(CHANNEL_MESSAGES_TEACHER_ID, getString(R.string.channel_messages_teachers), NotificationManager.IMPORTANCE_DEFAULT)
        val uefsMsg = createChannel(CHANNEL_MESSAGES_UEFS_ID, getString(R.string.channel_messages_uefs), NotificationManager.IMPORTANCE_DEFAULT)
        val posted = createChannel(CHANNEL_GRADES_POSTED_ID, getString(R.string.channel_grades_posted), NotificationManager.IMPORTANCE_DEFAULT)
        val dateChanged = createChannel(CHANNEL_GRADES_DATE_CHANGED_ID, getString(R.string.channel_grades_date_changed), NotificationManager.IMPORTANCE_DEFAULT)
        val valueChanged = createChannel(CHANNEL_GRADES_VALUE_CHANGED_ID, getString(R.string.channel_grades_value_changed), NotificationManager.IMPORTANCE_DEFAULT)
        val created = createChannel(CHANNEL_GRADES_CREATED_ID, getString(R.string.channel_grades_created), NotificationManager.IMPORTANCE_DEFAULT)
        val warnings = createChannel(CHANNEL_GENERAL_WARNINGS_ID, getString(R.string.warnings), NotificationManager.IMPORTANCE_DEFAULT)
        val remote = createChannel(CHANNEL_GENERAL_REMOTE_ID, getString(R.string.remote), NotificationManager.IMPORTANCE_DEFAULT)
        val eventGen = createChannel(CHANNEL_EVENTS_GENERAL_ID, getString(R.string.channel_events_general), NotificationManager.IMPORTANCE_DEFAULT)
        val bigTray = createChannel(CHANNEL_GENERAL_BIGTRAY_ID, getString(R.string.channel_big_tray_quota), NotificationManager.IMPORTANCE_LOW)

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

    private fun getManager() = getSystemService(NotificationManager::class.java)

    companion object {
        // Notification Groups
        const val CHANNEL_GROUP_MESSAGES_ID = "com.forcetower.uefs.MESSAGES"
        const val CHANNEL_GROUP_GRADES_ID = "com.forcetower.uefs.GRADES"
        const val CHANNEL_GROUP_GENERAL_ID = "com.forcetower.uefs.GENERAL"
        const val CHANNEL_GROUP_EVENTS_ID = "com.forcetower.uefs.EVENTS"
        // Notification Channels
        const val CHANNEL_MESSAGES_TEACHER_ID = "com.forcetower.uefs.MESSAGES.SAGRES.TEACHER.POST"
        const val CHANNEL_MESSAGES_UEFS_ID = "com.forcetower.uefs.MESSAGES.SAGRES.UEFS.POST"
        const val CHANNEL_GRADES_POSTED_ID = "com.forcetower.uefs.GRADES.POSTED"
        const val CHANNEL_GRADES_CREATED_ID = "com.forcetower.uefs.GRADES.CREATE"
        const val CHANNEL_GRADES_DATE_CHANGED_ID = "com.forcetower.uefs.GRADES.DATE_CHANGE"
        const val CHANNEL_GRADES_VALUE_CHANGED_ID = "com.forcetower.uefs.GRADES.VALUE_CHANGED"
        const val CHANNEL_GENERAL_WARNINGS_ID = "com.forcetower.uefs.GENERAL.WARNINGS"
        const val CHANNEL_GENERAL_REMOTE_ID = "com.forcetower.uefs.GENERAL.REMOTE"
        const val CHANNEL_GENERAL_BIGTRAY_ID = "com.forcetower.uefs.GENERAL.BIGTRAY"
        const val CHANNEL_EVENTS_GENERAL_ID = "com.forcetower.uefs.EVENTS.GENERAL"

        // Deleted Channels
        const val CHANNEL_MESSAGES_DCE_ID = "com.forcetower.uefs.MESSAGES.DCE"
        const val CHANNEL_MESSAGES_SAGRES_ID = "com.forcetower.uefs.MESSAGES.SAGRES.POST"
        const val CHANNEL_GRADES_CHANGED_ID = "com.forcetower.uefs.GRADES.CHANGE"
    }
}