package dev.forcetower.unes.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

// Alarm math + arming, shared by the publisher (data changed) and the
// receiver (fired / boot / clock change). One pending alarm at a time — the
// next 20:00-eve moment — and the receiver re-reads the snapshot when it
// fires, so the payload is as fresh as the last sync.
internal object EvaluationReminderAlarms {
    // Evening-before fire hour, device-local time. Evaluation dates are
    // day-only upstream, so "20:00 na véspera" stands in for "before the exam".
    const val FIRE_HOUR = 20

    fun rearm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val snapshot = EvaluationReminderSnapshot.load(context)
        val nowMs = System.currentTimeMillis()
        val nextAtMs = snapshot?.let { nextFireEpochMs(it, nowMs) }
        val pi = firePendingIntent(context)
        if (nextAtMs == null) {
            alarmManager.cancel(pi)
            return
        }
        // Inexact but Doze-surviving, like the widget tick — a reminder that
        // lands minutes after 20:00 is still a reminder; exact alarms would
        // cost a manifest permission and a settings round-trip on API 31+.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAtMs, pi)
    }

    fun nextFireEpochMs(snapshot: EvaluationReminderSnapshot, nowMs: Long): Long? =
        snapshot.reminders
            .mapNotNull { fireEpochMs(it.dateIso) }
            .filter { it > nowMs }
            .minOrNull()

    // Entries whose evaluation is tomorrow — what the alarm firing "now"
    // should announce. A Doze-deferred alarm that slips past midnight finds
    // an empty set and stays silent rather than promising "amanhã" wrongly.
    fun dueEntries(
        snapshot: EvaluationReminderSnapshot,
        nowMs: Long,
    ): List<EvaluationReminderSnapshot.Entry> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMs
        cal.add(Calendar.DATE, 1)
        val tomorrowIso = String.format(
            java.util.Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
        )
        return snapshot.reminders.filter { it.dateIso == tomorrowIso }
    }

    // 20:00 on the day before `dateIso`, in the device time zone.
    fun fireEpochMs(dateIso: String): Long? {
        val parts = dateIso.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size != 3) return null
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(parts[0], parts[1] - 1, parts[2], FIRE_HOUR, 0, 0)
        cal.add(Calendar.DATE, -1)
        return cal.timeInMillis
    }

    private fun firePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, EvaluationReminderReceiver::class.java).apply {
            action = EvaluationReminderReceiver.ACTION_FIRE
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_FIRE, intent, flags)
    }

    private const val REQUEST_FIRE = 0xE7A1
}
