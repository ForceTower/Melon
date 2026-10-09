package dev.forcetower.melon.core.common

// SAGRES fails a discipline below 75% attendance. The allowance is the most
// class-hours a student can miss and still pass, so it floors: 25% of 30h is
// 7,5h, and missing 8 already leaves 73,3%.
fun allowedMissedHours(totalHours: Int): Int = totalHours / 4
