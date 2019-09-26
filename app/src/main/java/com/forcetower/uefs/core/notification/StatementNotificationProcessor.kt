package com.forcetower.uefs.core.notification

import android.content.Context
import androidx.core.content.ContextCompat
import com.forcetower.sagres.utils.WordUtils
import com.forcetower.uefs.R
import com.forcetower.uefs.service.NotificationCreator
import com.forcetower.uefs.service.NotificationHelper

object StatementNotificationProcessor {

    @JvmStatic
    fun onStatementReceived(context: Context, data: Map<String, String>) {
        if (!NotificationCreator.shouldShowNotification("stg_ntf_statement_received", context)) return
        val type = data["service_typed"]
        val statement = data["statement"]
        val statementId = data["statement_id"]

        if (statement == null || statementId == null) return

//      val senderId = data["sender"]
        val senderName = data["sender_name"]
//      val senderImage = data["sender_image"]

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

        val body = context.getString(R.string.notification_statement_received_common_body, name)
        val builder = NotificationCreator.showDefaultImageNotification(
            context,
            NotificationHelper.CHANNEL_SOCIAL_STATEMENT_RECEIVED_ID,
            title,
            body,
            null
        ).setColor(ContextCompat.getColor(context, R.color.blue_accent))

        NotificationCreator.showNotification(context, statementId.toLongOrNull() ?: 67312L, builder)
    }
}
