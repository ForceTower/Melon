package dev.forcetower.unes.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar
import java.util.Locale

// Alarm math + arming for the student's own calendar entries. Same shape as
// `EvaluationReminderAlarms` — one pending alarm at a time, the receiver
// re-reads the snapshot when it fires — but on its own request code and
// snapshot file so the two never clobber each other.
internal object PersonalEventReminderAlarms {
    // Fire hour on the reminder day, device-local. Same slot the evaluation
    // reminders use, so a student never gets two different "tomorrow" times
    // from the same app.
    const val FIRE_HOUR = EvaluationReminderAlarms.FIRE_HOUR

    fun rearm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val snapshot = PersonalEventReminderSnapshot.load(context)
        val nowMs = System.currentTimeMillis()
        val nextAtMs = snapshot?.let { nextFireEpochMs(it, nowMs) }
        val pi = firePendingIntent(context)
        if (nextAtMs == null) {
            alarmManager.cancel(pi)
            return
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAtMs, pi)
    }

    fun nextFireEpochMs(snapshot: PersonalEventReminderSnapshot, nowMs: Long): Long? =
        snapshot.reminders
            .mapNotNull { fireEpochMs(it.fireDateIso) }
            .filter { it > nowMs }
            .minOrNull()

    // Entries whose reminder day is today — what the alarm firing "now" should
    // announce. A Doze-deferred alarm that slips past midnight finds an empty
    // set and stays silent rather than announcing the wrong day.
    fun dueEntries(
        snapshot: PersonalEventReminderSnapshot,
        nowMs: Long,
    ): List<PersonalEventReminderSnapshot.Entry> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMs
        val todayIso = String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
        )
        return snapshot.reminders.filter { it.fireDateIso == todayIso }
    }

    // 20:00 on `dateIso`, in the device time zone. Unlike the evaluation
    // alarms this takes no extra day off — the lead time is already baked into
    // the snapshot's fire date.
    fun fireEpochMs(dateIso: String): Long? {
        val parts = dateIso.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size != 3) return null
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(parts[0], parts[1] - 1, parts[2], FIRE_HOUR, 0, 0)
        return cal.timeInMillis
    }

    private fun firePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, PersonalEventReminderReceiver::class.java).apply {
            action = PersonalEventReminderReceiver.ACTION_FIRE
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_FIRE, intent, flags)
    }

    private const val REQUEST_FIRE = 0xE7A2
}
