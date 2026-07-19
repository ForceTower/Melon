package dev.forcetower.unes.reminders

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import dev.forcetower.unes.MainActivity
import dev.forcetower.unes.R

// Fires the "Avaliação amanhã" notifications from the on-disk snapshot and
// re-arms the next alarm. Also the boot/clock-change hook: RTC alarms don't
// survive a reboot and are pinned to wall-clock time, so both paths re-anchor
// from the snapshot. No DI on purpose — everything it needs is the snapshot
// file and system services, same isolation the widget receiver keeps.
internal class EvaluationReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_FIRE -> {
                postDueReminders(context)
                EvaluationReminderAlarms.rearm(context)
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> EvaluationReminderAlarms.rearm(context)
        }
    }

    private fun postDueReminders(context: Context) {
        val snapshot = EvaluationReminderSnapshot.load(context) ?: return
        val due = EvaluationReminderAlarms.dueEntries(snapshot, System.currentTimeMillis())
        if (due.isEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.notif_evaluation_reminder_channel))
                .build(),
        )
        due.forEach { entry ->
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle(context.getString(R.string.notif_evaluation_reminder_title))
                .setContentText(
                    context.getString(
                        R.string.notif_evaluation_reminder_body_format,
                        entry.label,
                        entry.disciplineName,
                    ),
                )
                .setContentIntent(contentIntent(context, entry.key))
                .setAutoCancel(true)
                .build()
            manager.notify(entry.key.hashCode(), notification)
        }
    }

    // Taps land on Turmas through the same unes:// path notification pushes
    // use — MainActivity reads either the data URI or the "url" extra.
    private fun contentIntent(context: Context, key: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = CLASSES_DEEPLINK.toUri()
            putExtra("url", CLASSES_DEEPLINK)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, key.hashCode(), intent, flags)
    }

    companion object {
        const val ACTION_FIRE = "dev.forcetower.unes.reminders.ACTION_FIRE"
        private const val CHANNEL_ID = "evaluation_reminders"
        private const val CLASSES_DEEPLINK = "unes://classes"
    }
}
