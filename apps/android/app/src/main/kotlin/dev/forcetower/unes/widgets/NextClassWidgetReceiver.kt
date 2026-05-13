package dev.forcetower.unes.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

// Bridges Glance's `NextClassWidget` into the system widget host. Standard
// `GlanceAppWidgetReceiver` plumbing — `NextClassWidget` does the rendering;
// this class handles bind/unbind and per-minute refresh scheduling so the
// countdown ticks even when no fresh snapshot landed from the host process.
class NextClassWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = NextClassWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        scheduleNextTick(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleNextTick(context)
    }

    override fun onDisabled(context: Context) {
        cancelTick(context)
        super.onDisabled(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TICK) {
            // Re-trigger AppWidget update to recompose the Glance widget
            // against the new wall clock. Cheaper than asking Glance to
            // notify per id — the system fans the broadcast out to every
            // bound instance for us.
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(
                android.content.ComponentName(context, NextClassWidgetReceiver::class.java),
            )
            if (ids.isNotEmpty()) {
                onUpdate(context, mgr, ids)
            }
        }
    }

    // Per-minute self-tick. Equivalent to iOS WidgetKit's per-minute timeline
    // entry, except we're piggybacking on `AlarmManager.RTC` to align to
    // wall-clock minute boundaries so the countdown rolls in lockstep with
    // the user's clock.
    private fun scheduleNextTick(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val nextMinute = nextWallClockMinute()
        val pi = tickPendingIntent(context)
        // Inexact alarm — AppWidget refreshes don't need to fire on the
        // exact second, and inexact lets the system batch the wakeup with
        // any other already-pending alarm in the same window. Saves battery
        // for a UI element the user only glances at. min SDK is 28 so
        // `setAndAllowWhileIdle` is always available.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, nextMinute, pi)
    }

    private fun cancelTick(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.cancel(tickPendingIntent(context))
    }

    private fun tickPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, NextClassWidgetReceiver::class.java).apply {
            action = ACTION_TICK
        }
        // FLAG_IMMUTABLE is mandatory on API 31+ for non-mutable PendingIntents.
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_TICK, intent, flags)
    }

    private fun nextWallClockMinute(): Long {
        val now = System.currentTimeMillis()
        val msInMinute = 60_000L
        return ((now / msInMinute) + 1) * msInMinute
    }

    companion object {
        private const val ACTION_TICK = "dev.forcetower.unes.widgets.ACTION_TICK"
        private const val REQUEST_TICK = 0xC1A5
    }
}
