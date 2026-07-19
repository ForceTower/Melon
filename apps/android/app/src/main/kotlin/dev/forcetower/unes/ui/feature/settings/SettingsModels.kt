package dev.forcetower.unes.ui.feature.settings

import dev.forcetower.unes.mvi.UiState
import dev.forcetower.unes.theme.ThemeMode

// What the grade notification reveals on the lock screen. Wire encoding
// matches `user_settings.grade_spoiler` on the API and Room mirror; the
// integers are the contract iOS `SpoilerMode.serverInt` writes. The dc
// redesign labels them Valor / Resumo / Discreto.
internal enum class SpoilerMode(val serverInt: Int) {
    Value(0),
    Comment(1),
    Posted(2);

    companion object {
        fun fromServerInt(value: Int): SpoilerMode? = entries.firstOrNull { it.serverInt == value }
    }
}

// Full UI state bag. Identity comes from the KMP profile flow, credentials
// from the vault flow, and the toggles from `ObserveSettingsUseCase`; each
// toggle write goes through `UpdateSettingsUseCase` so the local DAO flips
// first and the network PATCH reconciles afterwards. The theme mode is
// device-local (DataStore) and never syncs.
internal data class SettingsUiState(
    val displayName: String = "",
    val avatarInitial: String = "?",
    val campusLabel: String? = null,
    val username: String? = null,
    val password: String? = null,
    val nowEpochSeconds: Long = 0L,
    // Count of registered passkeys, shown on the "Chaves de acesso" row. Null
    // until the first fetch resolves (or if it fails) so the row can stay quiet.
    val passkeyCount: Int? = null,
    val themeMode: ThemeMode = ThemeMode.System,
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
    // Device-local evening-before reminder (DataStore, never syncs) and its
    // Remote Config gate. Not part of the "n/9 ativas" server-toggle count.
    val evaluationRemindersAvailable: Boolean = false,
    val evaluationRemindersEnabled: Boolean = true,
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
