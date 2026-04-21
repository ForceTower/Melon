package dev.forcetower.melon.core.common

import kotlinx.datetime.DayOfWeek

// Helpers shared by every use case that needs to align "now" with upstream-
// encoded schedule rows. Upstream `dia` (persisted untransformed in
// ClassAllocation.day — see ClassAllocationEntity) is 0=Sunday..6=Saturday.
// This is NOT ISO weekday and NOT Java Calendar.DAY_OF_WEEK — both of those
// are 1-indexed. The legacy v1 app added `+1` at ingest to match
// Calendar.DAY_OF_WEEK; v2 keeps the raw encoding and translates here.

const val MINUTES_IN_DAY: Int = 24 * 60
const val MINUTES_IN_WEEK: Int = 7 * MINUTES_IN_DAY

fun DayOfWeek.toUpstreamDay(): Int = when (this) {
    DayOfWeek.SUNDAY -> 0
    DayOfWeek.MONDAY -> 1
    DayOfWeek.TUESDAY -> 2
    DayOfWeek.WEDNESDAY -> 3
    DayOfWeek.THURSDAY -> 4
    DayOfWeek.FRIDAY -> 5
    DayOfWeek.SATURDAY -> 6
}

// Parses "HH:mm" (or "HH:mm:ss") into minutes since midnight. Returns null on
// anything malformed — callers typically filter those out of their schedule.
fun parseHhMm(value: String?): Int? {
    if (value.isNullOrBlank()) return null
    val parts = value.split(":")
    if (parts.size < 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].take(2).toIntOrNull() ?: return null
    return h * 60 + m
}

// Week-slot index used when picking the next upcoming allocation: day (1..7
// upstream) × minutes-in-day + minute-of-day. Lets a single modulo express
// "how many minutes from now to this slot, rolling into next week if needed".
fun weekSlot(upstreamDay: Int, startMinutes: Int): Int =
    upstreamDay * MINUTES_IN_DAY + startMinutes

fun weekSlotDelta(fromSlot: Int, toSlot: Int): Int =
    ((toSlot - fromSlot) % MINUTES_IN_WEEK + MINUTES_IN_WEEK) % MINUTES_IN_WEEK
