package dev.forcetower.unes.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import java.util.Calendar

// Bridges Glance's `NextClassWidget` into the system widget host. Standard
// `GlanceAppWidgetReceiver` plumbing — `NextClassWidget` does the rendering;
// this class handles bind/unbind and tick scheduling so the countdown keeps
// rolling even when no fresh snapshot landed from the host process.
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
        when (intent.action) {
            // Our self-scheduled minute/passive tick. Refan to onUpdate so
            // Glance recomposes against the new wall clock.
            ACTION_TICK,
            // User changed the system clock or crossed a TZ boundary. RTC
            // alarms are pinned to wall-clock time, so a clock jump either
            // fires the pending alarm early or orphans it in the future;
            // either way the countdown is wrong until we recompose. onUpdate
            // also reschedules, which re-anchors the tick chain to the new
            // wall clock.
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                val mgr = AppWidgetManager.getInstance(context)
                val ids = mgr.getAppWidgetIds(
                    ComponentName(context, NextClassWidgetReceiver::class.java),
                )
                if (ids.isNotEmpty()) {
                    onUpdate(context, mgr, ids)
                }
            }
        }
    }

    // Adaptive self-tick. While a class is running or about to start we wake
    // every wall-clock minute so the countdown rolls in lockstep with the
    // user's clock (same idea as iOS WidgetKit's per-minute timeline entry).
    // Further from the next state change we coalesce to 15 min, and when the
    // day is fully done with nothing to count down we sleep until midnight —
    // no point waking ~480 times overnight just to re-render the same frame.
    private fun scheduleNextTick(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val nowMs = System.currentTimeMillis()
        val entry = runCatching { loadCurrentEntry(context) }.getOrNull()
        val nextAtMs = nextTickEpochMs(entry, nowMs)
        val pi = tickPendingIntent(context)
        // Inexact alarm — AppWidget refreshes don't need to fire on the exact
        // second, and inexact lets the system batch the wakeup with any other
        // already-pending alarm in the same window. Saves battery for a UI
        // element the user only glances at. `setAndAllowWhileIdle` lets the
        // tick survive Doze, at the cost of being rate-limited to ~once every
        // 9 min in idle — acceptable, since the user only sees the widget
        // when the screen comes on (and `updatePeriodMillis` in the provider
        // info acts as a 30-min safety net on top of this).
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, nextAtMs, pi)
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

    companion object {
        private const val ACTION_TICK = "dev.forcetower.unes.widgets.ACTION_TICK"
        private const val REQUEST_TICK = 0xC1A5
    }
}

// Coarser-than-per-minute cadence for far-from-state-change moments. The
// eyebrow text reads in hours+minutes when the countdown is over an hour
// out ("em 3h 47min"), and at a glance the user doesn't notice 47→46. One
// wake every 15 min keeps the value within a quarter-hour of truth and drops
// overnight wakes by ~15× vs. the previous unconditional per-minute tick.
private const val PASSIVE_TICK_MS = 15L * 60_000L

// One wall-clock minute, used both as the "active" cadence and to align all
// scheduled times to a minute boundary (matches iOS WidgetKit's behavior of
// emitting one entry per minute, so the displayed value flips at the same
// moment as the user's system clock).
private const val MINUTE_MS = 60_000L

internal fun nextTickEpochMs(entry: NextClassEntry?, nowMs: Long): Long {
    // No snapshot yet (first install, before the publisher ran). Default to
    // per-minute so the widget self-heals as soon as a snapshot lands.
    if (entry == null) return alignToNextMinute(nowMs)

    return when (entry.state) {
        // In session — `endsIn` ticks per minute and the in-class handoff
        // threshold flips the layout 30 min before the end. Always per minute.
        NextClassState.InClass -> alignToNextMinute(nowMs)
        // Last hour before the next class: per-minute resolution actually
        // shows up on the eyebrow ("em 23 min"). Earlier than that the
        // eyebrow reads in hours+minutes and per-minute deltas are invisible.
        NextClassState.Upcoming ->
            if (entry.startsIn <= 60) alignToNextMinute(nowMs)
            else alignToNextMinute(nowMs + PASSIVE_TICK_MS)
        // Day done. If there's still a next-day class to count down to, use
        // the same Upcoming cadence (per-minute in the last hour, 15-min
        // before that). If there's nothing — empty entry, no bars, no next
        // day populated — sleep until midnight; the visible frame doesn't
        // change until "today" rolls over.
        NextClassState.DayDone -> when {
            entry.startsIn == 0 -> nextMidnightEpochMs(nowMs)
            entry.startsIn <= 60 -> alignToNextMinute(nowMs)
            else -> alignToNextMinute(nowMs + PASSIVE_TICK_MS)
        }
    }
}

// First wall-clock minute boundary strictly after `epochMs`. Always lands the
// alarm on a HH:MM:00 instant so the countdown rolls in lockstep with the
// user's clock instead of drifting by the few seconds of broadcast latency.
private fun alignToNextMinute(epochMs: Long): Long =
    ((epochMs / MINUTE_MS) + 1L) * MINUTE_MS

private fun nextMidnightEpochMs(nowMs: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = nowMs
    cal.add(Calendar.DATE, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
