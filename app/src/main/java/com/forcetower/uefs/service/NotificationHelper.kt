package com.forcetower.uefs.service

import android.annotation.TargetApi
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import com.forcetower.uefs.R
import com.forcetower.uefs.core.util.VersionUtils
import android.app.NotificationChannel
import android.os.Build


class NotificationHelper(val context: Context): ContextWrapper(context) {

    fun createChannels() {
        if (!VersionUtils.isOreo()) return

        val cGrades     = NotificationChannelGroup(CHANNEL_GROUP_GRADES_ID, getString(R.string.channel_group_grades))
        val cMessages   = NotificationChannelGroup(CHANNEL_GROUP_MESSAGES_ID, getString(R.string.channel_group_messages))
        val cGeneral    = NotificationChannelGroup(CHANNEL_GROUP_GENERAL_ID, getString(R.string.channel_group_general))
        val cEvents     = NotificationChannelGroup(CHANNEL_GROUP_EVENTS_ID, getString(R.string.channel_group_events))

        val manager = getManager()
        manager.createNotificationChannelGroups(listOf(cGrades, cMessages, cGeneral, cEvents))

        val messages    = createChannel(CHANNEL_MESSAGES_SAGRES_ID, getString(R.string.channel_messages_sagres), NotificationManager.IMPORTANCE_DEFAULT)
        val posted      = createChannel(CHANNEL_GRADES_POSTED_ID, getString(R.string.channel_grades_posted), NotificationManager.IMPORTANCE_DEFAULT)
        val changed     = createChannel(CHANNEL_GRADES_CHANGED_ID, getString(R.string.channel_grades_date_changed), NotificationManager.IMPORTANCE_DEFAULT)
        val created     = createChannel(CHANNEL_GRADES_CREATED_ID, getString(R.string.channel_grades_created), NotificationManager.IMPORTANCE_DEFAULT)
        val warnings    = createChannel(CHANNEL_GENERAL_WARNINGS_ID, getString(R.string.warnings), NotificationManager.IMPORTANCE_DEFAULT)
        val remote      = createChannel(CHANNEL_GENERAL_REMOTE_ID, getString(R.string.remote), NotificationManager.IMPORTANCE_DEFAULT)
        val eventGen    = createChannel(CHANNEL_EVENTS_GENERAL_ID, getString(R.string.channel_events_general), NotificationManager.IMPORTANCE_DEFAULT)
        val dceMsg      = createChannel(CHANNEL_MESSAGES_DCE_ID, getString(R.string.channel_messages_dce), NotificationManager.IMPORTANCE_DEFAULT)

        messages.group = CHANNEL_GROUP_MESSAGES_ID
        posted.group = CHANNEL_GROUP_GRADES_ID
        changed.group = CHANNEL_GROUP_GRADES_ID
        created.group = CHANNEL_GROUP_GRADES_ID
        warnings.group = CHANNEL_GROUP_GENERAL_ID
        remote.group = CHANNEL_GROUP_GENERAL_ID
        eventGen.group = CHANNEL_GROUP_EVENTS_ID
        dceMsg.group = CHANNEL_GROUP_MESSAGES_ID

        manager.createNotificationChannel(messages)
        manager.createNotificationChannel(posted)
        manager.createNotificationChannel(changed)
        manager.createNotificationChannel(created)
        manager.createNotificationChannel(warnings)
        manager.createNotificationChannel(remote)
        manager.createNotificationChannel(eventGen)
        manager.createNotificationChannel(dceMsg)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createChannel(channelId: String, name: CharSequence, importance: Int): NotificationChannel {
        val channel = NotificationChannel(channelId, name, importance)
        channel.enableLights(true)
        channel.setShowBadge(true)
        channel.vibrationPattern = longArrayOf(150, 300, 150, 300)
        return channel
    }

    private fun getManager() = getSystemService(NotificationManager::class.java)



    companion object {
        //Notification Groups
        const val CHANNEL_GROUP_MESSAGES_ID     = "com.forcetower.uefs.MESSAGES"
        const val CHANNEL_GROUP_GRADES_ID       = "com.forcetower.uefs.GRADES"
        const val CHANNEL_GROUP_GENERAL_ID      = "com.forcetower.uefs.GENERAL"
        const val CHANNEL_GROUP_EVENTS_ID       = "com.forcetower.uefs.EVENTS"
        //Notification Channels
        const val CHANNEL_MESSAGES_SAGRES_ID    = "com.forcetower.uefs.MESSAGES.SAGRES.POST"
        const val CHANNEL_MESSAGES_DCE_ID       = "com.forcetower.uefs.MESSAGES.DCE"
        const val CHANNEL_GRADES_POSTED_ID      = "com.forcetower.uefs.GRADES.POSTED"
        const val CHANNEL_GRADES_CREATED_ID     = "com.forcetower.uefs.GRADES.CREATE"
        const val CHANNEL_GRADES_CHANGED_ID     = "com.forcetower.uefs.GRADES.CHANGE"
        const val CHANNEL_GENERAL_WARNINGS_ID   = "com.forcetower.uefs.GENERAL.WARNINGS"
        const val CHANNEL_GENERAL_REMOTE_ID     = "com.forcetower.uefs.GENERAL.REMOTE"
        const val CHANNEL_EVENTS_GENERAL_ID     = "com.forcetower.uefs.EVENTS.GENERAL"
    }
}