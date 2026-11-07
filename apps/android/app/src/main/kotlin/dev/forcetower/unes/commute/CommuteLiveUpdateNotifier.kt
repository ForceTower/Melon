package dev.forcetower.unes.commute

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.forcetower.unes.BuildConfig
import dev.forcetower.unes.MainActivity
import dev.forcetower.unes.R
import dev.forcetower.unes.ui.feature.commute.CommuteEstimate
import dev.forcetower.unes.ui.feature.commute.CommuteFormat
import dev.forcetower.unes.ui.feature.commute.text
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Instant

// Draws the followed bus as an ongoing notification. On Android 16 it asks to
// be promoted to a Live Update: pinned at the top of the shade and the lock
// screen, the estimate in a status-bar chip, the route line as the progress
// track with the bus riding it. Earlier versions show the same notification
// with a plain progress bar.
@Singleton
internal class CommuteLiveUpdateNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = NotificationManagerCompat.from(context)

    // Android 13+ asks for the permission; on every version the student can
    // still turn the app's notifications off.
    fun canPost(): Boolean =
        manager.areNotificationsEnabled() &&
            (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
                )

    // `dismissAt` is when an ended activity leaves the shade.
    fun show(
        record: CommuteLiveRecord,
        now: Instant,
        stale: Boolean,
        dismissAt: Instant? = null,
    ) {
        ensureChannel()
        val notification = when (val view = commuteLiveView(record, now, stale)) {
            is CommuteLiveView.Following -> following(record, view, now)
            CommuteLiveView.Passed -> final(record, R.string.commute_follow_passed, dismissAt, now)
            CommuteLiveView.NoEstimate -> final(record, R.string.commute_follow_no_estimate, dismissAt, now)
        }
        post(notification)
    }

    fun cancel() {
        manager.cancel(NOTIFICATION_TAG, NOTIFICATION_ID)
        cancelStale()
    }

    // Past this without a push the band freezes on the clock and says the
    // connection is gone, like the iOS stale date. Inexact on purpose: it
    // only has to be right by the time someone looks.
    fun scheduleStale(
        activityId: String,
        at: Instant,
    ) {
        // Scenario builds pin AppClock to a fixed past date, so every alarm
        // would ring at once and onStale would re-arm it in a loop.
        if (BuildConfig.SCENARIO) return
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        alarms.setAndAllowWhileIdle(AlarmManager.RTC, at.toEpochMilliseconds(), staleIntent(activityId))
    }

    fun cancelStale() {
        context.getSystemService(AlarmManager::class.java)?.cancel(staleIntent(activityId = ""))
    }

    private fun following(
        record: CommuteLiveRecord,
        view: CommuteLiveView.Following,
        now: Instant,
    ): Notification {
        val resources = context.resources
        val color = ContextCompat.getColor(context, lineColor(record.lineNumber))
        val caption = resources.getString(
            if (view.untilDeparture) {
                R.string.commute_follow_until_departure
            } else {
                R.string.commute_follow_until_your_stop
            },
        )
        // "no ponto" needs no "até o seu ponto" after it.
        val title = if (view.estimate == CommuteEstimate.AtStop) {
            resources.getString(R.string.commute_bus_at_stop)
        } else {
            resources.getString(R.string.commute_live_title, view.estimate.text(resources), caption)
        }
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_commute_bus)
            .setColor(color)
            .setContentTitle(title)
            .setContentText(details(resources, view).ifEmpty { record.lineName })
            .setSubText(resources.getString(R.string.commute_line_accessibility, record.lineNumber))
            .setStyle(progressStyle(record, color))
            .setShortCriticalText(chipText(resources, view.chip))
            .setRequestPromotedOngoing(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setWhen(record.updatedAt * 1_000)
            .setShowWhen(true)
            // Gone at the expiry even if the alarm that ends the follow is late.
            .setTimeoutAfter((record.expiry - now).inWholeMilliseconds)
            .setContentIntent(openApp())
            .setDeleteIntent(stopIntent(record.activityId))
            .addAction(0, resources.getString(R.string.commute_live_action_stop), stopIntent(record.activityId))
            .build()
    }

    private fun final(
        record: CommuteLiveRecord,
        @StringRes title: Int,
        dismissAt: Instant?,
        now: Instant,
    ): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_commute_bus)
            .setColor(ContextCompat.getColor(context, lineColor(record.lineNumber)))
            .setContentTitle(context.getString(title))
            .setContentText(context.getString(R.string.commute_line_accessibility, record.lineNumber))
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setAutoCancel(true)
            .setTimeoutAfter(((dismissAt ?: now) - now).inWholeMilliseconds.coerceAtLeast(FINAL_MIN_MS))
            .setContentIntent(openApp())
            .build()

    // "seu ponto é a próxima parada · chega na UEFS ~07:12", or what went wrong first.
    private fun details(
        resources: Resources,
        view: CommuteLiveView.Following,
    ): String =
        buildList {
            when (view.degraded) {
                CommuteLiveView.Degraded.SignalLost -> add(resources.getString(R.string.commute_state_stale))
                CommuteLiveView.Degraded.Offline -> add(resources.getString(R.string.commute_state_offline))
                null -> Unit
            }
            view.stopsAway?.let { stops ->
                add(
                    if (stops == 1) {
                        resources.getString(R.string.commute_follow_next_stop)
                    } else {
                        resources.getQuantityString(R.plurals.commute_follow_stops_away, stops, stops)
                    },
                )
            }
            val campus = view.campusArrival
            val leaves = view.usuallyLeaves
            if (campus != null) {
                add(resources.getString(R.string.commute_reaches_campus, CommuteFormat.clock(campus)))
            } else if (leaves != null) {
                add(resources.getString(R.string.commute_usually_leaves, CommuteFormat.clock(leaves)))
            }
        }.joinToString(DETAIL_SEPARATOR)

    // The chip is cut past about seven characters, so a long minute band
    // keeps only its numbers.
    private fun chipText(
        resources: Resources,
        chip: CommuteEstimate,
    ): String {
        val full = chip.text(resources)
        if (full.length <= CHIP_LENGTH) return full
        return when (chip) {
            is CommuteEstimate.MinuteRange ->
                chip.low.toString() + resources.getString(R.string.commute_range_separator) + chip.high
            is CommuteEstimate.Minutes -> chip.minutes.toString()
            else -> full
        }
    }

    private fun progressStyle(
        record: CommuteLiveRecord,
        color: Int,
    ): NotificationCompat.ProgressStyle {
        val track = commuteLiveTrack(record)
        val stop = IconCompat.createWithResource(context, R.drawable.ic_commute_stop)
        val style = NotificationCompat.ProgressStyle()
            .setStyledByProgress(true)
            .setProgressSegments(track.segments.map { NotificationCompat.ProgressStyle.Segment(it).setColor(color) })
            .setProgressPoints(
                listOfNotNull(track.minePoint?.let { NotificationCompat.ProgressStyle.Point(it).setColor(color) }),
            )
            .setProgress(track.progress ?: 0)
        if (track.progress != null) {
            style.setProgressTrackerIcon(IconCompat.createWithResource(context, R.drawable.ic_commute_bus))
        }
        if (track.mineAtStart) style.setProgressStartIcon(stop)
        if (track.mineAtEnd) style.setProgressEndIcon(stop)
        return style
    }

    private fun ensureChannel() {
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.commute_live_channel))
                .setDescription(context.getString(R.string.commute_live_channel_description))
                .setSound(null, null)
                .setVibrationEnabled(false)
                .setShowBadge(false)
                .build(),
        )
    }

    private fun post(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        manager.notify(NOTIFICATION_TAG, NOTIFICATION_ID, notification)
    }

    // Taps open "Seguindo o 087" through the same unes:// path pushes use,
    // or the board once the bus passed.
    private fun openApp(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = FOLLOW_DEEPLINK.toUri()
            putExtra("url", FOLLOW_DEEPLINK)
        }
        return PendingIntent.getActivity(context, REQUEST_OPEN, intent, PENDING_FLAGS)
    }

    private fun stopIntent(activityId: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_STOP,
            Intent(context, CommuteLiveUpdateReceiver::class.java)
                .setAction(CommuteLiveUpdateReceiver.ACTION_STOP)
                .putExtra(CommuteLiveUpdateReceiver.EXTRA_ACTIVITY_ID, activityId),
            PENDING_FLAGS,
        )

    private fun staleIntent(activityId: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_STALE,
            Intent(context, CommuteLiveUpdateReceiver::class.java)
                .setAction(CommuteLiveUpdateReceiver.ACTION_STALE)
                .putExtra(CommuteLiveUpdateReceiver.EXTRA_ACTIVITY_ID, activityId),
            PENDING_FLAGS,
        )

    @ColorRes
    private fun lineColor(lineNumber: String): Int =
        when (lineNumber) {
            "003" -> R.color.notification_transit_003
            "088" -> R.color.notification_transit_088
            "087" -> R.color.notification_transit_087
            else -> R.color.notification_accent
        }

    private companion object {
        const val CHANNEL_ID = "commute_live"
        const val NOTIFICATION_TAG = "commute_live"
        const val NOTIFICATION_ID = 1
        const val REQUEST_OPEN = 7_100
        const val REQUEST_STOP = 7_101
        const val REQUEST_STALE = 7_102
        const val PENDING_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        const val FOLLOW_DEEPLINK = "unes://commute/follow"
        const val DETAIL_SEPARATOR = " · "
        const val CHIP_LENGTH = 7

        // An end with no dismissal date still shows long enough to be read.
        const val FINAL_MIN_MS = 60_000L
    }
}
