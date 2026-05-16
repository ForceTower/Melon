package dev.forcetower.unes.ui.feature.settings

import dev.forcetower.unes.mvi.UiState

// What the grade notification reveals on the lock screen. Wire encoding
// matches `user_settings.grade_spoiler` on the API and Room mirror; the
// integers are the contract iOS `SpoilerMode.serverInt` writes.
internal enum class SpoilerMode(val serverInt: Int) {
    Value(0),
    Comment(1),
    Posted(2);

    companion object {
        fun fromServerInt(value: Int): SpoilerMode? = entries.firstOrNull { it.serverInt == value }
    }
}

// Per-icon accent palette used by the settings rows. Mirrors iOS
// `SettingsTone` in `SettingsModels.swift` and the JSX `CFG_TONES`. The
// background/foreground pair is resolved against the theme at the call site
// so feature code stays out of raw color literals.
internal enum class SettingsTone { Plum, Magenta, Teal, Coral, Amber }

// Full UI state bag. Hydrated from the KMP `ObserveSettingsUseCase` flow;
// each toggle write goes through `UpdateSettingsUseCase` so the local DAO
// flips first and the network PATCH reconciles afterwards.
internal data class SettingsUiState(
    val username: String? = null,
    val password: String? = null,
    val lastSyncIso: String? = null,
    val nowEpochSeconds: Long = 0L,
    val spoiler: SpoilerMode = SpoilerMode.Comment,
    val notifMsgBroadcast: Boolean = true,
    val notifMsgClass: Boolean = true,
    val notifMsgDirect: Boolean = true,
    val notifGradePosted: Boolean = true,
    val notifGradeChanged: Boolean = true,
    val notifGradeDateChanged: Boolean = false,
    val notifClassLocation: Boolean = true,
    val notifClassMaterial: Boolean = true,
    val notifClassSubject: Boolean = false,
) : UiState {
    val messageActiveCount: Int get() = countOn(notifMsgBroadcast, notifMsgClass, notifMsgDirect)
    val gradeActiveCount: Int get() = countOn(notifGradePosted, notifGradeChanged, notifGradeDateChanged)
    val classActiveCount: Int get() = countOn(notifClassLocation, notifClassMaterial, notifClassSubject)
    val totalActive: Int get() = messageActiveCount + gradeActiveCount + classActiveCount
}

private fun countOn(vararg values: Boolean): Int = values.count { it }

// Identifiers for each notification toggle so the screen can route taps back
// through one mutator instead of nine. Mapped to the corresponding KMP patch
// field by `SettingsViewModel.toggle`.
internal enum class NotifToggle {
    MsgBroadcast, MsgClass, MsgDirect,
    GradePosted, GradeChanged, GradeDateChanged,
    ClassLocation, ClassMaterial, ClassSubject,
}
